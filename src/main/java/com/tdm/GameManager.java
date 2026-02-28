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
    private final Map<UUID, Long> lastDamagers = new HashMap<>();
    
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
        player.sendMessage(Component.text("Class selected: " + playerClass.name(), NamedTextColor.GREEN));
    }

    public void startGame() {
        if (!gameActive || gameStarted) {
            return;
        }
        
        gameStarted = true;
        // reset all team scores
        teamScores.clear();
        for (Team t : Team.values()) {
            teamScores.put(t, 0);
        }
        
        for (UUID uuid : playerTeams.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                respawnPlayer(player);
                if (enableSounds) {
                    SoundManager.playGameStartSound(player);
                }
            }
        }
        
        startScoreboardUpdater();
        
        Bukkit.broadcast(Component.text("GAME STARTED!", NamedTextColor.GOLD));
    }

    public void endGame() {
        gameActive = false;
        gameStarted = false;
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
        lastDamagers.clear();
        // reset scoreboards
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }

    public void handleDeath(Player player, Player killer) {
        Team team = playerTeams.get(player.getUniqueId());
        
        if (enableSounds) {
            SoundManager.playDeathSound(player);
        }
        
        // Set to spectator mode immediately
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        String respawnMsg = plugin.getConfig().getString("messages.respawn-soon", "You are now spectating. Respawning in %time%s...")
                .replace("%time%", String.valueOf(respawnTime));
        player.sendMessage(Component.text(respawnMsg, NamedTextColor.GRAY));
        
        if (killer != null && playerTeams.containsKey(killer.getUniqueId())) {
            Team killerTeam = playerTeams.get(killer.getUniqueId());
            if (killerTeam != null && !killerTeam.equals(team)) {
                // increment score for killer's team
                teamScores.put(killerTeam, teamScores.getOrDefault(killerTeam, 0) + 1);

                // Update killer stats and points
                playerKills.put(killer.getUniqueId(), playerKills.getOrDefault(killer.getUniqueId(), 0) + 1);
                addPoints(killer.getUniqueId(), killPoints);

                // Check for recent headshot on this victim by this killer (within 5s)
                UUID recentBy = recentHeadshotBy.get(player.getUniqueId());
                Long recentTime = recentHeadshotTime.get(player.getUniqueId());
                if (recentBy != null && recentTime != null && recentBy.equals(killer.getUniqueId())) {
                    long age = System.currentTimeMillis() - recentTime;
                    if (age <= 5000) {
                        // award headshot bonus
                        addPoints(killer.getUniqueId(), headshotBonus);
                        killer.sendMessage(Component.text(plugin.getConfig().getString("scoring.headshot-message", "+%points% 💢 HEADSHOT").replace("%points%", String.valueOf(headshotBonus)), NamedTextColor.AQUA));
                        if (enableSounds) {
                            SoundManager.playHeadshotSound(killer);
                        }
                    }
                }

                // Clear recent headshot tracking for victim
                clearRecentHeadshot(player.getUniqueId());

                UUID assister = findAssister(player.getUniqueId(), killer.getUniqueId());
                if (assister != null) {
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
                }

                String killMsg = plugin.getConfig().getString("scoring.kill-message", "+%points% ⚔ KILL")
                        .replace("%points%", String.valueOf(killPoints));
                killer.sendMessage(Component.text(killMsg, NamedTextColor.GREEN));
                if (enableSounds) {
                    SoundManager.playKillSound(killer);
                }
            }
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
        lastDamagers.put(victim, System.currentTimeMillis());
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
        long currentTime = System.currentTimeMillis();
        Long lastDamageTime = lastDamagers.get(victim);
        
        if (lastDamageTime != null && (currentTime - lastDamageTime) < 10000) {
            return null;
        }
        return null;
    }

    private void respawnPlayer(Player player) {
        Team team = playerTeams.get(player.getUniqueId());
        PlayerClass playerClass = playerClasses.get(player.getUniqueId());
        
        if (team == null || playerClass == null) return;
        
        Location spawn = spawnManager.getSpawn(team, currentGameMode);
        
        if (spawn == null) return;
        
        player.teleport(spawn);
        if (healOnRespawn) {
            player.setHealth(20);
        }
        if (resetHungerOnRespawn) {
            player.setFoodLevel(20);
        }
        player.getInventory().clear();
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        
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
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameStarted) {
                    cancel();
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
                            Component.text("TDM"),
                            org.bukkit.scoreboard.RenderType.INTEGER);
                    obj.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR);

                    // compute number of team lines according to mode
                    List<Team> teamsToShow;
                    if (currentGameMode == GameMode.FREE_FOR_ALL) {
                        teamsToShow = new ArrayList<>(enabledFfaTeams);
                        if (teamsToShow.isEmpty()) teamsToShow = Arrays.asList(Team.RED, Team.BLUE);
                    } else {
                        teamsToShow = Arrays.asList(Team.RED, Team.BLUE);
                    }
                    int line = teamsToShow.size() + 5; // start score index
                    obj.getScore("TEAM SCORES").setScore(line--);
                    for (Team t : teamsToShow) {
                        String entry = t.getDisplayName() + ": " + teamScores.getOrDefault(t, 0) + "/" + winsNeeded;
                        obj.getScore(entry).setScore(line--);
                    }
                    obj.getScore(" ").setScore(line--);
                    // personal stats
                    obj.getScore("Your Points: " + playerPoints.getOrDefault(uuid, 0)).setScore(line--);
                    obj.getScore("Kills: " + playerKills.getOrDefault(uuid, 0)).setScore(line--);
                    obj.getScore("Assists: " + playerAssists.getOrDefault(uuid, 0)).setScore(line--);
                    obj.getScore("Headshots: " + playerHeadshots.getOrDefault(uuid, 0)).setScore(line--);

                    player.setScoreboard(board);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void activateGame() {
        gameActive = true;
        gameStarted = false;
        // Apply pending toggle to this activated game and clear pending state
        forceAutoAssignThisGame = pendingForceAutoAssign;
        pendingForceAutoAssign = false;
        
        // Schedule auto-start if enabled
        if (autoStartDelay > 0 && plugin.getConfig().getBoolean("game.auto-start", false)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (gameActive && !gameStarted) {
                        startGame();
                    }
                }
            }.runTaskLater(plugin, autoStartDelay * 20L);
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
        playerTeams.put(playerUuid, team);
        Player p = Bukkit.getPlayer(playerUuid);
        if (p != null) {
            p.sendMessage(Component.text("Your team has been changed to " + team.getDisplayName(), NamedTextColor.GREEN));
            // teleport to team spawn if available
            Location spawn = spawnManager.getSpawn(team, currentGameMode);
            if (spawn != null) p.teleport(spawn);
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
     */
    public void broadcastJoinOptions() {
        if (!gameActive) return;
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
