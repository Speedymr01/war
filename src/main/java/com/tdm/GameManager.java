package com.tdm;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.time.Duration;
import java.util.*;

public class GameManager {
    private final TeamDeathmatchPlugin plugin;
    private final SpawnManager spawnManager;
    
    private boolean gameActive = false;
    private boolean gameStarted = false;
    
    private final Map<UUID, Team> playerTeams = new HashMap<>();
    private final Map<UUID, PlayerClass> playerClasses = new HashMap<>();
    private final Map<UUID, Integer> playerKills = new HashMap<>();
    private final Map<UUID, Integer> playerAssists = new HashMap<>();
    private final Map<UUID, Integer> playerHeadshots = new HashMap<>();
    private final Map<UUID, Integer> playerPoints = new HashMap<>();
    // recent headshot: victim -> (damager, timestamp)
    private final Map<UUID, UUID> recentHeadshotBy = new HashMap<>();
    private final Map<UUID, Long> recentHeadshotTime = new HashMap<>();
    // Track last damager per victim: victim -> damager UUID
    private final Map<UUID, UUID> lastDamagerBy = new HashMap<>();
    // Track last damage time per victim: victim -> timestamp
    private final Map<UUID, Long> lastDamagerTime = new HashMap<>();
    // keep the previous damager in case a final hit overwrote a potential assister
    private final Map<UUID, UUID> prevDamagerBy = new HashMap<>();
    private final Map<UUID, Long> prevDamagerTime = new HashMap<>();
    
    // score per team (used for win tracking)
    private final Map<Team, Integer> teamScores = new HashMap<>();
    
    // FFA team configuration
    private final Set<Team> enabledFfaTeams = new HashSet<>();
    
    // pending flag controlled by admin GUI: when true, the next activation will force auto-assignment
    private boolean pendingForceAutoAssign = false;

    // runtime flag for the currently active game: when true players cannot choose teams and
    // will be auto-assigned using the smart (smallest-team) algorithm; random used only when all equal
    private boolean forceAutoAssignThisGame = false;

    // Game mode
    private GameMode currentGameMode = GameMode.FREE_FOR_ALL;
    
    // Task tracking for cleanup on reload
    private org.bukkit.scheduler.BukkitTask scoreboardTask = null;
    private org.bukkit.scheduler.BukkitTask autoStartTask = null;
    private org.bukkit.scheduler.BukkitTask classSelectionCheckTask = null;
    
    // Players who must select a class during game
    private final Set<UUID> playersNeedingClassSelection = new HashSet<>();
    
    // Config values
    private int winsNeeded;
    private int respawnTime;
    private int killPoints;
    private int assistPoints;
    private int headshotBonus;
    private boolean showTitle;
    private boolean enableSounds;
    private boolean enableKillMessages;
    private boolean enableDeathMessages;
    private boolean enableKillStreaks;
    private boolean healOnRespawn;
    private boolean resetHungerOnRespawn;
    private int autoStartDelay;
    private boolean autoBalanceTeams;
    private int maxTeamDifference;

    public GameManager(TeamDeathmatchPlugin plugin, SpawnManager spawnManager) {
        this.plugin = plugin;
        this.spawnManager = spawnManager;
        loadConfigSettings();
    }
    
    private void loadConfigSettings() {
        winsNeeded = plugin.getConfig().getInt("game.wins-needed", 3);
        respawnTime = plugin.getConfig().getInt("game.respawn-time", 5);
        autoStartDelay = plugin.getConfig().getInt("game.auto-start-delay", 0);
        autoBalanceTeams = plugin.getConfig().getBoolean("game.auto-balance-teams", false);
        maxTeamDifference = plugin.getConfig().getInt("game.max-team-difference", 2);
        killPoints = plugin.getConfig().getInt("scoring.kill-points", 10);
        assistPoints = plugin.getConfig().getInt("scoring.assist-points", 5);
        headshotBonus = plugin.getConfig().getInt("scoring.headshot-bonus", 5);
        showTitle = plugin.getConfig().getBoolean("respawn.show-title", true);
        enableSounds = plugin.getConfig().getBoolean("features.enable-sounds", true);
        enableKillMessages = plugin.getConfig().getBoolean("features.enable-kill-messages", true);
        enableDeathMessages = plugin.getConfig().getBoolean("features.enable-death-messages", true);
        enableKillStreaks = plugin.getConfig().getBoolean("features.enable-kill-streaks", true);
        healOnRespawn = plugin.getConfig().getBoolean("respawn.heal-on-respawn", true);
        resetHungerOnRespawn = plugin.getConfig().getBoolean("respawn.reset-hunger-on-respawn", true);
        
        // load enabled FFA teams (default red + blue)
        enabledFfaTeams.clear();
        if (plugin.getConfig().contains("teams.enabled")) {
            for (String key : plugin.getConfig().getConfigurationSection("teams.enabled").getKeys(false)) {
                if (plugin.getConfig().getBoolean("teams.enabled." + key, false)) {
                    Team t = Team.fromString(key);
                    if (t != null) enabledFfaTeams.add(t);
                }
            }
        }
        if (enabledFfaTeams.isEmpty()) {
            enabledFfaTeams.add(Team.RED);
            enabledFfaTeams.add(Team.BLUE);
        }
    }

    public boolean isGameActive() {
        return gameActive;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public boolean isPlayerInGame(UUID uuid) {
        return playerTeams.containsKey(uuid);
    }

    public void joinGame(Player player, Team requestedTeam) {
        if (!gameActive || gameStarted) {
            player.sendMessage(Component.text("Cannot join right now!", NamedTextColor.RED));
            return;
        }

        Team team;
        if (currentGameMode == GameMode.FREE_FOR_ALL) {
            if (forceAutoAssignThisGame) {
                // forced auto-assign for this active game: choose smallest enabled team for balance
                // if all enabled teams are equal, pick a random enabled team
                team = getSmallestEnabledTeam();
                if (team == null) team = getRandomEnabledTeam();
                if (team == null) {
                    player.sendMessage(Component.text("No teams available to join!", NamedTextColor.RED));
                    return;
                }
            } else if (requestedTeam != null) {
                if (!isTeamEnabled(requestedTeam)) {
                    player.sendMessage(Component.text("That team is not enabled!", NamedTextColor.RED));
                    return;
                }
                team = requestedTeam;
            } else {
                // choose smallest team for balance, fallback to random
                team = getSmallestEnabledTeam();
                if (team == null) {
                    team = getRandomEnabledTeam();
                }
                if (team == null) {
                    player.sendMessage(Component.text("No teams available to join!", NamedTextColor.RED));
                    return;
                }
            }
        } else {
            // other modes still only red/blue balancing by size
            team = requestedTeam != null ? requestedTeam : getSmallestTeam();
        }

        playerTeams.put(player.getUniqueId(), team);
        playerKills.put(player.getUniqueId(), 0);
        playerAssists.put(player.getUniqueId(), 0);
        
        // Update nametag color
        updatePlayerNametagColor(player, team);
        
        player.sendMessage(Component.text("You joined ", NamedTextColor.GRAY)
            .append(Component.text(team.getDisplayName(), team.getColor())));
        
        ClassSelectionGUI.openClassSelection(player, this);
    }

    public boolean getPendingForceAutoAssign() {
        return pendingForceAutoAssign;
    }

    public void setPendingForceAutoAssign(boolean pending) {
        this.pendingForceAutoAssign = pending;
    }

    public boolean isForceAutoAssignThisGame() {
        return forceAutoAssignThisGame;
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public void setPlayerClass(Player player, PlayerClass playerClass) {
        playerClasses.put(player.getUniqueId(), playerClass);
        player.sendMessage(Component.text("Class selected: " + playerClass.getName(), NamedTextColor.GREEN));
    }

    public void startGame() {
        if (!gameActive || gameStarted) {
            return;
        }
        
        // Check if there are any players
        if (playerTeams.isEmpty()) {
            Bukkit.broadcast(Component.text("Cannot start game: No players have joined!", NamedTextColor.RED));
            return;
        }
        
        // Check if spawns are set for active teams
        boolean hasValidSpawns = false;
        if (currentGameMode == GameMode.FREE_FOR_ALL) {
            // Check if at least one enabled team has a spawn
            for (Team team : enabledFfaTeams) {
                if (spawnManager.getSpawn(team, currentGameMode) != null) {
                    hasValidSpawns = true;
                    break;
                }
            }
        } else {
            // For other modes, check red and blue
            hasValidSpawns = spawnManager.getSpawn(Team.RED, currentGameMode) != null 
                          && spawnManager.getSpawn(Team.BLUE, currentGameMode) != null;
        }
        
        if (!hasValidSpawns) {
            Bukkit.broadcast(Component.text("Cannot start game: No spawn points set! Use /tdm admin to set spawns.", NamedTextColor.RED));
            return;
        }
        
        gameStarted = true;
        // reset all team scores
        teamScores.clear();
        for (Team t : Team.values()) {
            teamScores.put(t, 0);
        }
        
        playersNeedingClassSelection.clear();
        
        for (UUID uuid : playerTeams.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                // If player hasn't selected a class, open GUI and track them
                if (playerClasses.get(uuid) == null) {
                    playersNeedingClassSelection.add(uuid);
                    ClassSelectionGUI.openClassSelection(player, this);
                    player.sendMessage(Component.text("Please select a class to continue!", NamedTextColor.YELLOW));
                } else {
                    respawnPlayer(player);
                    if (enableSounds) {
                        SoundManager.playGameStartSound(player);
                    }
                }
            }
        }
        
        // Start task to reopen GUI for players who close it without selecting
        startClassSelectionCheckTask();
        
        startScoreboardUpdater();
        
        Bukkit.broadcast(Component.text("GAME STARTED!", NamedTextColor.GOLD));
    }

    public void endGame() {
        gameActive = false;
        gameStarted = false;
        
        // Cancel any running tasks
        if (scoreboardTask != null) {
            scoreboardTask.cancel();
            scoreboardTask = null;
        }
        if (autoStartTask != null) {
            autoStartTask.cancel();
            autoStartTask = null;
        }
        if (classSelectionCheckTask != null) {
            classSelectionCheckTask.cancel();
            classSelectionCheckTask = null;
        }
        playersNeedingClassSelection.clear();
        forceAutoAssignThisGame = false; // clear runtime assignment forcing when game ends
        
        // determine winner by highest score
        Team winner = null;
        int best = -1;
        for (Map.Entry<Team,Integer> entry : teamScores.entrySet()) {
            if (entry.getValue() > best) {
                best = entry.getValue();
                winner = entry.getKey();
            }
        }
        if (winner == null) winner = Team.RED; // fallback
        
        // Put all players in spectator mode
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerTeams.containsKey(player.getUniqueId())) {
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
            }
        }
        
        List<Map.Entry<UUID, Integer>> rankings = new ArrayList<>();
        for (UUID uuid : playerTeams.keySet()) {
            int score = getPlayerPoints(uuid);
            // fallback: if no points tracked, compute from kills/assists
            if (score == 0) {
                score = playerKills.getOrDefault(uuid, 0) * killPoints + playerAssists.getOrDefault(uuid, 0) * assistPoints + playerHeadshots.getOrDefault(uuid, 0) * headshotBonus;
            }
            rankings.add(new AbstractMap.SimpleEntry<>(uuid, score));
        }
        rankings.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        Bukkit.broadcast(Component.text("=".repeat(40), NamedTextColor.GOLD));
        Bukkit.broadcast(Component.text("GAME OVER!", NamedTextColor.GOLD));
        Bukkit.broadcast(Component.text(winner.name() + " TEAM WINS!", 
            winner == Team.RED ? NamedTextColor.RED : NamedTextColor.BLUE));
        Bukkit.broadcast(Component.text(""));
        Bukkit.broadcast(Component.text("PLAYER RANKINGS:", NamedTextColor.YELLOW));
        
        for (int i = 0; i < rankings.size(); i++) {
            UUID uuid = rankings.get(i).getKey();
            int score = rankings.get(i).getValue();
            Player p = Bukkit.getPlayer(uuid);
            String name = p != null ? p.getName() : "Unknown";
            Team team = playerTeams.get(uuid);
            
            if (p != null) {
                if (team == winner) {
                    if (enableSounds) {
                        SoundManager.playVictorySound(p);
                    }
                } else {
                    if (enableSounds) {
                        SoundManager.playDefeatSound(p);
                    }
                }
            }
            
            Bukkit.broadcast(Component.text((i + 1) + ". ", NamedTextColor.GOLD)
                .append(Component.text(name, team.getColor()))
                .append(Component.text(" - " + score + " points", NamedTextColor.GRAY)));
        }
        Bukkit.broadcast(Component.text("=".repeat(40), NamedTextColor.GOLD));
        
        playerTeams.clear();
        playerClasses.clear();
        playerKills.clear();
        playerAssists.clear();
        playerHeadshots.clear();
        lastDamagerBy.clear();
        lastDamagerTime.clear();
        // reset scoreboards and nametag colors
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            clearPlayerNametagColor(p);
        }
    }

    public void handleDeath(Player player, Player killer, org.bukkit.event.entity.EntityDamageEvent.DamageCause cause) {
        plugin.getLogger().info("[DEBUG] DEATH HANDLER CALLED: player=" + player.getName() + ", killer=" + (killer != null ? killer.getName() : "null") + ", cause=" + cause);
        Team team = playerTeams.get(player.getUniqueId());
        plugin.getLogger().info("[DEBUG] DEATH: victim team=" + team);
        
        if (enableSounds) {
            SoundManager.playDeathSound(player);
        }
        
        // Set to spectator mode immediately
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        String respawnMsg = plugin.getConfig().getString("messages.respawn-soon", "You are now spectating. Respawning in %time%s...")
                .replace("%time%", String.valueOf(respawnTime));
        player.sendMessage(Component.text(respawnMsg, NamedTextColor.GRAY));
        
        // Broadcast death message
        Component deathMessage;
        if (killer != null) {
            NamedTextColor victimColor = team != null ? team.getColor() : NamedTextColor.RED;
            Team killerTeam = playerTeams.get(killer.getUniqueId());
            NamedTextColor killerColor = killerTeam != null ? killerTeam.getColor() : NamedTextColor.RED;
            deathMessage = Component.text(player.getName(), victimColor)
                    .append(Component.text(" was killed by ", NamedTextColor.GRAY))
                    .append(Component.text(killer.getName(), killerColor));
        } else {
            // Environmental death
            NamedTextColor victimColor = team != null ? team.getColor() : NamedTextColor.RED;
            String causeName = cause != null ? cause.name().replace("_", " ").toLowerCase() : "unknown";
            deathMessage = Component.text(player.getName(), victimColor)
                    .append(Component.text(" died to ", NamedTextColor.GRAY))
                    .append(Component.text(causeName, NamedTextColor.GOLD));
        }
        
        // Send to all players
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (isPlayerInGame(onlinePlayer.getUniqueId())) {
                onlinePlayer.sendMessage(deathMessage);
            }
        }
        
        if (killer != null && playerTeams.containsKey(killer.getUniqueId())) {
            Team killerTeam = playerTeams.get(killer.getUniqueId());
            if (killerTeam != null && !killerTeam.equals(team)) {
                // increment score for killer's team
                teamScores.put(killerTeam, teamScores.getOrDefault(killerTeam, 0) + 1);

                // Update killer stats and points
                playerKills.put(killer.getUniqueId(), playerKills.getOrDefault(killer.getUniqueId(), 0) + 1);
                addPoints(killer.getUniqueId(), killPoints);

                // Check if the killing blow was a headshot
                UUID recentBy = recentHeadshotBy.get(player.getUniqueId());
                
                if (recentBy != null && recentBy.equals(killer.getUniqueId())) {
                    // award headshot bonus - killing blow was a headshot
                    recordHeadshot(killer.getUniqueId());
                    addPoints(killer.getUniqueId(), headshotBonus);
                    killer.sendMessage(Component.text(plugin.getConfig().getString("scoring.headshot-message", "+%points% 💢 HEADSHOT").replace("%points%", String.valueOf(headshotBonus)), NamedTextColor.AQUA));
                    if (enableSounds) {
                        SoundManager.playHeadshotSound(killer);
                    }
                }

                // Clear recent headshot tracking for victim
                clearRecentHeadshot(player.getUniqueId());

                // now handle kill award if we have a killer, regardless of damage type
                if (killer != null && killerTeam != null && !killerTeam.equals(team)) {
                    String killMsg = plugin.getConfig().getString("scoring.kill-message", "+%points% ⚔ KILL")
                            .replace("%points%", String.valueOf(killPoints));
                    killer.sendMessage(Component.text(killMsg, NamedTextColor.GREEN));
                    plugin.getLogger().info("[DEBUG] Assist: KILL AWARDED to " + killer.getUniqueId() + " with cause " + cause);
                    if (enableSounds) {
                        SoundManager.playKillSound(killer);
                    }
                }
            }
        }
        
        // Determine assister before clearing damage tracking (run for all deaths, regardless of killer)
        UUID assister = findAssister(player.getUniqueId(), killer != null ? killer.getUniqueId() : null);
        
        // clear damage tracking for victim now that they are dead
        lastDamagerBy.remove(player.getUniqueId());
        lastDamagerTime.remove(player.getUniqueId());
        prevDamagerBy.remove(player.getUniqueId());
        prevDamagerTime.remove(player.getUniqueId());

        // award assist if we have a valid assister and it is not the killer
        if (assister != null) {
            plugin.getLogger().info("[DEBUG] Assist: Found assister = " + assister);
            if (!assister.equals(killer != null ? killer.getUniqueId() : null)) {
                plugin.getLogger().info("[DEBUG] Assist: Assister differs from killer, AWARDING ASSIST TO " + assister);
                playerAssists.put(assister, playerAssists.getOrDefault(assister, 0) + 1);
                addPoints(assister, assistPoints);
                
                Player assistPlayer = Bukkit.getPlayer(assister);
                if (assistPlayer != null) {
                    String assistMsg = plugin.getConfig().getString("scoring.assist-message", "+%points% ⭐ ASSIST")
                            .replace("%points%", String.valueOf(assistPoints));
                    assistPlayer.sendMessage(Component.text(assistMsg, NamedTextColor.YELLOW));
                    if (enableSounds) {
                        SoundManager.playAssistSound(assistPlayer);
                    }
                }
            } else {
                plugin.getLogger().info("[DEBUG] Assist: Assister equals killer (" + assister + "), not awarding assist (they get kill instead)");
            }
            
            // Award team point for assist if no killer (environmental death)
            if (killer == null) {
                Team assisterTeam = playerTeams.get(assister);
                if (assisterTeam != null && !assisterTeam.equals(playerTeams.get(player.getUniqueId()))) {
                    teamScores.put(assisterTeam, teamScores.getOrDefault(assisterTeam, 0) + 1);
                    plugin.getLogger().info("[DEBUG] Assist: Team " + assisterTeam + " score incremented for environmental kill");
                }
            }
        } else {
            plugin.getLogger().info("[DEBUG] Assist: No assister found");
        }

        // check any team reached win threshold
        for (int score : teamScores.values()) {
            if (score >= winsNeeded) {
                endGame();
                return;
            }
        }
        
        scheduleRespawn(player);
    }

    public void recordDamage(UUID victim, UUID damager) {
        if (victim == null || damager == null) return;
        UUID current = lastDamagerBy.get(victim);
        Long currentTime = lastDamagerTime.get(victim);
        if (current != null && currentTime != null) {
            // if the new hit is from a different player we push the old entry to 'previous'
            if (!current.equals(damager)) {
                prevDamagerBy.put(victim, current);
                prevDamagerTime.put(victim, currentTime);
                plugin.getLogger().info("[DEBUG] Assist: Recording damage - victim=" + victim + ", new damager=" + damager + ", previous damager moved=" + current);
            }
        }
        lastDamagerBy.put(victim, damager);
        lastDamagerTime.put(victim, System.currentTimeMillis());
        plugin.getLogger().info("[DEBUG] Assist: Last damager updated - victim=" + victim + ", damager=" + damager);
    }
    
    public void recordHeadshot(UUID damager) {
        playerHeadshots.put(damager, playerHeadshots.getOrDefault(damager, 0) + 1);
    }

    /**
     * Mark that a victim was hit in the head by damager at current time.
     * This is used to award headshot bonus on kill if the killer matches.
     */
    public void markRecentHeadshot(UUID victim, UUID damager) {
        if (victim == null || damager == null) return;
        recentHeadshotBy.put(victim, damager);
        recentHeadshotTime.put(victim, System.currentTimeMillis());
    }

    public void clearRecentHeadshot(UUID victim) {
        recentHeadshotBy.remove(victim);
        recentHeadshotTime.remove(victim);
    }

    public void addPoints(UUID player, int points) {
        if (player == null) return;
        playerPoints.put(player, playerPoints.getOrDefault(player, 0) + points);
    }

    public int getPlayerPoints(UUID player) {
        return playerPoints.getOrDefault(player, 0);
    }
    
    public int getPlayerHeadshots(UUID uuid) {
        return playerHeadshots.getOrDefault(uuid, 0);
    }

    private void scheduleRespawn(Player player) {
        int[] countdown = {respawnTime};
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !gameStarted) {
                    cancel();
                    return;
                }
                
                if (countdown[0] > 0) {
                    if (plugin.getConfig().getBoolean("respawn.show-countdown", true)) {
                        player.sendMessage(Component.text("Respawning in " + countdown[0] + "s...", NamedTextColor.YELLOW));
                    }
                    countdown[0]--;
                } else {
                    if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                        respawnPlayer(player);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private UUID findAssister(UUID victim, UUID killer) {
        if (victim == null) return null;
        UUID lastDamager = lastDamagerBy.get(victim);
        Long lastDamageTime = lastDamagerTime.get(victim);
        plugin.getLogger().info("[DEBUG] Assist: findAssister called - victim=" + victim + ", killer=" + killer + ", lastDamager=" + lastDamager);
        if (lastDamager == null || lastDamageTime == null) {
            plugin.getLogger().info("[DEBUG] Assist: No last damager found");
            return null;
        }
        // if the hit happened more than 10 seconds before death, no assist
        long currentTime = System.currentTimeMillis();
        long timeSinceDamage = currentTime - lastDamageTime;
        plugin.getLogger().info("[DEBUG] Assist: Time since damage = " + timeSinceDamage + "ms");
        if (timeSinceDamage > 10000) {
            plugin.getLogger().info("[DEBUG] Assist: Damage too old (>10s), no assist");
            return null;
        }
        // if the last damager was the victim themselves, ignore as well
        if (lastDamager.equals(victim)) {
            plugin.getLogger().info("[DEBUG] Assist: Last damager is victim, no assist");
            return null;
        }
        // return the last damager - caller will decide if it's an assist or kill based on killer and cause
        plugin.getLogger().info("[DEBUG] Assist: Returning potential assister = " + lastDamager);
        return lastDamager;
    }

    private void respawnPlayer(Player player) {
        Team team = playerTeams.get(player.getUniqueId());
        PlayerClass playerClass = playerClasses.get(player.getUniqueId());
        
        if (team == null) {
            player.sendMessage(Component.text("Error: You are not assigned to a team!", NamedTextColor.RED));
            return;
        }
        
        // Auto-assign default class if missing
        if (playerClass == null) {
            playerClass = PlayerClass.getFirstEnabledClass();
            if (playerClass == null) {
                player.sendMessage(Component.text("Error: No classes available! Contact an admin.", NamedTextColor.RED));
                return;
            }
            playerClasses.put(player.getUniqueId(), playerClass);
            player.sendMessage(Component.text("Auto-assigned class: " + playerClass.getName(), NamedTextColor.YELLOW));
        }
        
        Location spawn = spawnManager.getSpawn(team, currentGameMode);
        
        // Fallback: try to find any available spawn if team spawn is missing
        if (spawn == null) {
            player.sendMessage(Component.text("Warning: No spawn set for " + team.getDisplayName() + "! Using fallback.", NamedTextColor.YELLOW));
            
            // Try other teams' spawns as fallback
            for (Team t : Team.values()) {
                spawn = spawnManager.getSpawn(t, currentGameMode);
                if (spawn != null) break;
            }
            
            // If still no spawn, use player's current location
            if (spawn == null) {
                spawn = player.getLocation();
                player.sendMessage(Component.text("Error: No spawns configured! Staying at current location.", NamedTextColor.RED));
            }
        }
        
        player.teleport(spawn);
        if (healOnRespawn) {
            player.setHealth(20);
        }
        if (resetHungerOnRespawn) {
            player.setFoodLevel(20);
        }
        player.getInventory().clear();
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        
        // Update nametag color
        updatePlayerNametagColor(player, team);
        
        playerClass.giveKit(player);
        
        if (showTitle) {
            player.showTitle(Title.title(
                Component.text("RESPAWNED", NamedTextColor.GREEN),
                Component.empty(),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(1000), Duration.ofMillis(500))
            ));
        }
        
        if (enableSounds) {
            SoundManager.playRespawnSound(player);
        }
    }

    private Team getSmallestTeam() {
        int redCount = 0, blueCount = 0;
        for (Team team : playerTeams.values()) {
            if (team == Team.RED) redCount++;
            else blueCount++;
        }
        return redCount <= blueCount ? Team.RED : Team.BLUE;
    }

    private void startScoreboardUpdater() {
        // Cancel existing task if any
        if (scoreboardTask != null && !scoreboardTask.isCancelled()) {
            scoreboardTask.cancel();
        }
        
        BukkitRunnable runnable = new BukkitRunnable() {
            @SuppressWarnings("deprecation")
            @Override
            public void run() {
                if (!gameStarted) {
                    cancel();
                    scoreboardTask = null;
                    return;
                }

                // update sidebar scoreboard every second
                for (UUID uuid : playerTeams.keySet()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null) continue;

                    org.bukkit.scoreboard.Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
                    org.bukkit.scoreboard.Objective obj = board.registerNewObjective(
                            "tdm",
                            org.bukkit.scoreboard.Criteria.DUMMY,
                            Component.text("⚔ TDM ⚔", NamedTextColor.GOLD),
                            org.bukkit.scoreboard.RenderType.INTEGER);
                    obj.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR);
                    
                    // Register team colors on this scoreboard so nametags show colors
                    for (UUID otherUuid : playerTeams.keySet()) {
                        Player otherPlayer = Bukkit.getPlayer(otherUuid);
                        if (otherPlayer != null) {
                            Team otherTeam = playerTeams.get(otherUuid);
                            if (otherTeam != null) {
                                String teamName = "tdm_" + otherTeam.name().toLowerCase();
                                org.bukkit.scoreboard.Team scoreboardTeam = board.getTeam(teamName);
                                if (scoreboardTeam == null) {
                                    scoreboardTeam = board.registerNewTeam(teamName);
                                    scoreboardTeam.setColor(GameManager.this.getChatColor(otherTeam));
                                }
                                scoreboardTeam.addEntry(otherPlayer.getName());
                            }
                        }
                    }

                    // compute number of team lines according to mode
                    List<Team> teamsToShow;
                    if (currentGameMode == GameMode.FREE_FOR_ALL) {
                        teamsToShow = new ArrayList<>(enabledFfaTeams);
                        if (teamsToShow.isEmpty()) teamsToShow = Arrays.asList(Team.RED, Team.BLUE);
                    } else {
                        teamsToShow = Arrays.asList(Team.RED, Team.BLUE);
                    }
                    int line = teamsToShow.size() + 5; // start score index
                    
                    // Team scores header with color
                    String headerEntry = org.bukkit.ChatColor.YELLOW + "TEAM SCORES";
                    obj.getScore(headerEntry).setScore(line--);
                    
                    // Team scores with team colors
                    for (Team t : teamsToShow) {
                        org.bukkit.ChatColor chatColor = GameManager.this.getChatColor(t);
                        String entry = chatColor + t.getDisplayName() + ": " + teamScores.getOrDefault(t, 0) + "/" + winsNeeded;
                        obj.getScore(entry).setScore(line--);
                    }
                    
                    // Empty line
                    obj.getScore(" ").setScore(line--);
                    
                    // Personal stats with colors
                    String pointsEntry = org.bukkit.ChatColor.GOLD + "Your Points: " + playerPoints.getOrDefault(uuid, 0);
                    obj.getScore(pointsEntry).setScore(line--);
                    
                    String killsEntry = org.bukkit.ChatColor.GREEN + "Kills: " + playerKills.getOrDefault(uuid, 0);
                    obj.getScore(killsEntry).setScore(line--);
                    
                    String assistsEntry = org.bukkit.ChatColor.YELLOW + "Assists: " + playerAssists.getOrDefault(uuid, 0);
                    obj.getScore(assistsEntry).setScore(line--);
                    
                    String headshotsEntry = org.bukkit.ChatColor.AQUA + "Headshots: " + playerHeadshots.getOrDefault(uuid, 0);
                    obj.getScore(headshotsEntry).setScore(line--);

                    player.setScoreboard(board);
                }
            }
        };
        scoreboardTask = runnable.runTaskTimer(plugin, 0L, 20L);
    }
    
    private void startClassSelectionCheckTask() {
        // Cancel existing task if any
        if (classSelectionCheckTask != null && !classSelectionCheckTask.isCancelled()) {
            classSelectionCheckTask.cancel();
        }
        
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameStarted || playersNeedingClassSelection.isEmpty()) {
                    if (playersNeedingClassSelection.isEmpty()) {
                        cancel();
                        classSelectionCheckTask = null;
                    }
                    return;
                }
                
                // Check each player who needs to select a class
                for (UUID uuid : new HashSet<>(playersNeedingClassSelection)) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null) {
                        playersNeedingClassSelection.remove(uuid);
                        continue;
                    }
                    
                    // If player has now selected a class, remove from set and respawn them
                    if (playerClasses.containsKey(uuid)) {
                        playersNeedingClassSelection.remove(uuid);
                        GameManager.this.respawnPlayer(player);
                        if (enableSounds) {
                            SoundManager.playGameStartSound(player);
                        }
                    } else {
                        // Check if player's inventory is closed (not the class selection GUI)
                        String invTitle = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(player.getOpenInventory().title());
                        if (!invTitle.equals("Select Your Class")) {
                            // Player closed the GUI without selecting - reopen it
                            ClassSelectionGUI.openClassSelection(player, GameManager.this);
                        }
                    }
                }
            }
        };
        classSelectionCheckTask = runnable.runTaskTimer(plugin, 20L, 10L);  // Check every 0.5 seconds
    }
    
    /**
     * Convert Team NamedTextColor to ChatColor for scoreboard
     */
    @SuppressWarnings("deprecation")
    private org.bukkit.ChatColor getChatColor(Team team) {
        switch (team) {
            case RED: return org.bukkit.ChatColor.RED;
            case BLUE: return org.bukkit.ChatColor.BLUE;
            case GREEN: return org.bukkit.ChatColor.GREEN;
            case YELLOW: return org.bukkit.ChatColor.YELLOW;
            case PURPLE: return org.bukkit.ChatColor.LIGHT_PURPLE;
            case ORANGE: return org.bukkit.ChatColor.GOLD;
            case AQUA: return org.bukkit.ChatColor.AQUA;
            case GRAY: return org.bukkit.ChatColor.GRAY;
            case WHITE: return org.bukkit.ChatColor.WHITE;
            case BLACK: return org.bukkit.ChatColor.BLACK;
            default: return org.bukkit.ChatColor.WHITE;
        }
    }
    
    /**
     * Update a player's nametag color to match their team
     */
    @SuppressWarnings("deprecation")
    private void updatePlayerNametagColor(Player player, Team team) {
        if (player == null || team == null) return;
        
        // Update on main scoreboard for all players to see
        org.bukkit.scoreboard.Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        
        // Get or create a team for this color on main board
        String teamName = "tdm_" + team.name().toLowerCase();
        org.bukkit.scoreboard.Team mainTeam = mainBoard.getTeam(teamName);
        
        if (mainTeam == null) {
            mainTeam = mainBoard.registerNewTeam(teamName);
            mainTeam.setColor(getChatColor(team));
        }
        
        // Add player to the team on main board
        mainTeam.addEntry(player.getName());
        
        // Also update on all online players' personal scoreboards so they see the color
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            org.bukkit.scoreboard.Scoreboard playerBoard = onlinePlayer.getScoreboard();
            if (playerBoard != null && playerBoard != mainBoard) {
                org.bukkit.scoreboard.Team playerTeam = playerBoard.getTeam(teamName);
                if (playerTeam == null) {
                    playerTeam = playerBoard.registerNewTeam(teamName);
                    playerTeam.setColor(getChatColor(team));
                }
                playerTeam.addEntry(player.getName());
            }
        }
    }
    
    /**
     * Remove a player's nametag color
     */
    private void clearPlayerNametagColor(Player player) {
        if (player == null) return;
        
        // Clear from main scoreboard
        org.bukkit.scoreboard.Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (org.bukkit.scoreboard.Team team : mainBoard.getTeams()) {
            if (team.getName().startsWith("tdm_")) {
                team.removeEntry(player.getName());
            }
        }
        
        // Clear from all players' personal scoreboards
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            org.bukkit.scoreboard.Scoreboard playerBoard = onlinePlayer.getScoreboard();
            if (playerBoard != null && playerBoard != mainBoard) {
                for (org.bukkit.scoreboard.Team team : playerBoard.getTeams()) {
                    if (team.getName().startsWith("tdm_")) {
                        team.removeEntry(player.getName());
                    }
                }
            }
        }
    }

    public void activateGame() {
        gameActive = true;
        gameStarted = false;
        // Apply pending toggle to this activated game and clear pending state
        forceAutoAssignThisGame = pendingForceAutoAssign;
        pendingForceAutoAssign = false;
        
        // Warn admins if spawns are not configured
        boolean hasSpawns = false;
        if (currentGameMode == GameMode.FREE_FOR_ALL) {
            for (Team team : enabledFfaTeams) {
                if (spawnManager.getSpawn(team, currentGameMode) != null) {
                    hasSpawns = true;
                    break;
                }
            }
        } else {
            hasSpawns = spawnManager.getSpawn(Team.RED, currentGameMode) != null 
                     && spawnManager.getSpawn(Team.BLUE, currentGameMode) != null;
        }
        
        if (!hasSpawns) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("tdm.admin")) {
                    p.sendMessage(Component.text("⚠ Warning: No spawn points configured! Set spawns before starting.", NamedTextColor.RED));
                }
            }
        }
        
        // Warn if no classes are loaded
        if (PlayerClass.getAllClasses().isEmpty()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("tdm.admin")) {
                    p.sendMessage(Component.text("⚠ Warning: No classes configured in config.yml!", NamedTextColor.RED));
                }
            }
        }
        
        // Schedule auto-start if enabled
        if (autoStartDelay > 0 && plugin.getConfig().getBoolean("game.auto-start", false)) {
            // Cancel existing auto-start task if any
            if (autoStartTask != null && !autoStartTask.isCancelled()) {
                autoStartTask.cancel();
            }
            
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    if (gameActive && !gameStarted) {
                        startGame();
                    }
                    autoStartTask = null;
                }
            };
            autoStartTask = runnable.runTaskLater(plugin, autoStartDelay * 20L);
        }
    }

    public Team getPlayerTeam(UUID uuid) {
        return playerTeams.get(uuid);
    }

    public List<UUID> getPlayersInTeam(Team team) {
        List<UUID> list = new ArrayList<>();
        for (Map.Entry<UUID, Team> e : playerTeams.entrySet()) {
            if (e.getValue() == team) list.add(e.getKey());
        }
        return list;
    }

    public void setPlayerTeam(UUID playerUuid, Team team) {
        if (playerUuid == null || team == null) return;
        
        Player p = Bukkit.getPlayer(playerUuid);
        
        // Validate team is enabled in FFA mode
        if (currentGameMode == GameMode.FREE_FOR_ALL && !isTeamEnabled(team)) {
            if (p != null) {
                p.sendMessage(Component.text("Cannot move to " + team.getDisplayName() + " - team is not enabled!", NamedTextColor.RED));
            }
            // Notify admin who tried to move the player
            for (Player admin : Bukkit.getOnlinePlayers()) {
                if (admin.hasPermission("tdm.admin") && !admin.equals(p)) {
                    admin.sendMessage(Component.text("Cannot move player to " + team.getDisplayName() + " - team is not enabled in FFA mode!", NamedTextColor.RED));
                }
            }
            return;
        }
        
        // Check if team has a spawn point
        Location spawn = spawnManager.getSpawn(team, currentGameMode);
        if (spawn == null) {
            if (p != null) {
                p.sendMessage(Component.text("Warning: " + team.getDisplayName() + " has no spawn point set!", NamedTextColor.YELLOW));
            }
            // Notify admin
            for (Player admin : Bukkit.getOnlinePlayers()) {
                if (admin.hasPermission("tdm.admin") && !admin.equals(p)) {
                    admin.sendMessage(Component.text("Warning: " + team.getDisplayName() + " has no spawn point! Player moved anyway.", NamedTextColor.YELLOW));
                }
            }
        }
        
        playerTeams.put(playerUuid, team);
        
        if (p != null) {
            // Update nametag color
            updatePlayerNametagColor(p, team);
            p.sendMessage(Component.text("Your team has been changed to " + team.getDisplayName(), NamedTextColor.GREEN));
            // teleport to team spawn if available
            if (spawn != null) {
                p.teleport(spawn);
            } else {
                p.sendMessage(Component.text("No spawn available - staying at current location.", NamedTextColor.GRAY));
            }
        }
    }

    // ---- FFA team utilities ----
    public Set<Team> getEnabledFfaTeams() {
        return Collections.unmodifiableSet(enabledFfaTeams);
    }

    public boolean isTeamEnabled(Team team) {
        return enabledFfaTeams.contains(team);
    }

    public void toggleFfaTeam(Team team) {
        if (team == null) return;
        if (enabledFfaTeams.contains(team)) enabledFfaTeams.remove(team);
        else enabledFfaTeams.add(team);
        plugin.getConfig().set("teams.enabled." + team.name().toLowerCase(), enabledFfaTeams.contains(team));
        plugin.saveConfig();
        // broadcast update if game active and not started
        if (gameActive && !gameStarted) {
            broadcastJoinOptions();
        }
    }

    private Team getRandomEnabledTeam() {
        if (enabledFfaTeams.isEmpty()) return null;
        int idx = new Random().nextInt(enabledFfaTeams.size());
        int i = 0;
        for (Team t : enabledFfaTeams) {
            if (i++ == idx) return t;
        }
        return null;
    }

    private Team getSmallestEnabledTeam() {
        if (enabledFfaTeams.isEmpty()) return null;
        // compute counts for enabled teams
        Map<Team, Integer> counts = new EnumMap<>(Team.class);
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (Team t : enabledFfaTeams) {
            int count = 0;
            for (Team assigned : playerTeams.values()) {
                if (assigned == t) count++;
            }
            counts.put(t, count);
            if (count < min) min = count;
            if (count > max) max = count;
        }
        // if all enabled teams are equal counts, return null to signal caller to use random
        if (min == max) return null;
        // otherwise return a team with the minimum count
        for (Team t : enabledFfaTeams) {
            if (counts.getOrDefault(t, 0) == min) return t;
        }
        return null;
    }

    /**
     * Send a broadcast listing available teams to join.
     * Phrase adapts to current mode and enabled teams.
     * If force auto-assign is active, only shows a single [JOIN] button.
     */
    public void broadcastJoinOptions() {
        if (!gameActive) return;
        
        // If force auto-assign is active, only show a single join button
        if (forceAutoAssignThisGame) {
            String base;
            if (currentGameMode == GameMode.FREE_FOR_ALL) {
                base = "Join the free for all: ";
            } else {
                base = "Join the match: ";
            }
            Component joinMsg = Component.text(base, NamedTextColor.GREEN)
                .append(Component.text("[JOIN]", NamedTextColor.GOLD)
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/tdm join"))
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Join the match (random team)"))));
            Bukkit.broadcast(joinMsg);
            return;
        }
        
        // Normal mode: show team selection options
        String base;
        if (currentGameMode == GameMode.FREE_FOR_ALL) {
            base = "Join the free for all: ";
        } else {
            base = "Join the match: ";
        }
        Component joinMsg = Component.text(base, NamedTextColor.GREEN);
        // include random join button
        joinMsg = joinMsg.append(Component.text("[Random]", NamedTextColor.GRAY)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/tdm join"))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Join a balanced team"))));
        boolean first = true;
        List<Team> teamsToShow;
        if (currentGameMode == GameMode.FREE_FOR_ALL) {
            teamsToShow = new ArrayList<>(enabledFfaTeams);
            if (teamsToShow.isEmpty()) teamsToShow = Arrays.asList(Team.RED, Team.BLUE);
        } else {
            teamsToShow = Arrays.asList(Team.RED, Team.BLUE);
        }
        for (Team t : teamsToShow) {
            if (!first) joinMsg = joinMsg.append(Component.text(" or ", NamedTextColor.GREEN));
            joinMsg = joinMsg.append(Component.text("[" + t.name().toUpperCase() + "]", t.getColor())
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/tdm join " + t.name().toLowerCase()))
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Join the " + t.getDisplayName()))));
            first = false;
        }
        Bukkit.broadcast(joinMsg);
    }

    public TeamDeathmatchPlugin getPlugin() {
        return plugin;
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfigSettings();
        if (spawnManager != null) {
            spawnManager.reloadSpawns();
        }
    }

    public int getHeadshotBonus() {
        return headshotBonus;
    }
    public boolean isKillStreaksEnabled() {
        return enableKillStreaks;
    }

    public boolean isKillMessagesEnabled() {
        return enableKillMessages;
    }

    public boolean isDeathMessagesEnabled() {
        return enableDeathMessages;
    }

    public void balanceTeams() {
        if (!autoBalanceTeams) return;
        
        int redCount = 0, blueCount = 0;
        for (Team team : playerTeams.values()) {
            if (team == Team.RED) redCount++;
            else blueCount++;
        }
        
        int diff = Math.abs(redCount - blueCount);
        if (diff > maxTeamDifference) {
            // Team balancing logic would go here
            // For now, this is a placeholder
        }
    }

    public GameMode getCurrentGameMode() {
        return currentGameMode;
    }

    public void setGameMode(GameMode mode) {
        this.currentGameMode = mode;
    }

    public enum GameMode {
        FREE_FOR_ALL, FOUR_VS_FOUR
    }

    public enum Team {
        RED("Red Team", NamedTextColor.RED),
        BLUE("Blue Team", NamedTextColor.BLUE),
        GREEN("Green Team", NamedTextColor.GREEN),
        YELLOW("Yellow Team", NamedTextColor.YELLOW),
        PURPLE("Purple Team", NamedTextColor.LIGHT_PURPLE),
        ORANGE("Orange Team", NamedTextColor.GOLD),
        AQUA("Aqua Team", NamedTextColor.AQUA),
        GRAY("Gray Team", NamedTextColor.GRAY),
        WHITE("White Team", NamedTextColor.WHITE),
        BLACK("Black Team", NamedTextColor.BLACK);
        
        private final String displayName;
        private final NamedTextColor color;
        
        Team(String displayName, NamedTextColor color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public String getDisplayName() { return displayName; }
        public NamedTextColor getColor() { return color; }
        
        public static Team fromString(String name) {
            for (Team t : values()) {
                if (t.name().equalsIgnoreCase(name) || t.displayName.equalsIgnoreCase(name)) {
                    return t;
                }
            }
            return null;
        }
    }
}
