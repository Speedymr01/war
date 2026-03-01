package com.tdm;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.Action;

public class GameListener implements Listener {
    private final GameManager gameManager;

    public GameListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        event.setCancelled(true);

        if (title.equals("Select Your Class")) {
            PlayerClass playerClass = ClassSelectionGUI.getClassFromSlot(event.getSlot());
            if (playerClass != null) {
                gameManager.setPlayerClass(player, playerClass);
                playerClass.giveKit(player);
                player.closeInventory();
            }
            return;
        }

        // Admin GUI handling
        if (title.equals("TDM Admin")) {
            // handle admin main menu
            ItemStack clicked = (ItemStack) event.getCurrentItem();
            if (clicked == null) return;
            String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
            switch (name) {
                case "Activate":
                    AdminGUI.openActivateGUI(player, gameManager);
                    player.sendMessage(Component.text("Admin GUI: choose game type to activate.", NamedTextColor.YELLOW));
                    break;
                case "Set Team Spawns":
                    AdminGUI.openSpawnGUI(player, gameManager);
                    player.sendMessage(Component.text("Admin GUI: select a team to set spawn.", NamedTextColor.YELLOW));
                    break;
                case "Start":
                    // Use the same logic as the /tdm start command to ensure checks/permissions are identical
                    Bukkit.dispatchCommand(player, "tdm start");
                    player.sendMessage(Component.text("Admin GUI: start command executed.", NamedTextColor.GREEN));
                    break;
                case "Players":
                    AdminGUI.openPlayersGUI(player, gameManager);
                    player.sendMessage(Component.text("Admin GUI: select player to move.", NamedTextColor.YELLOW));
                    break;
                case "Team Settings":
                    AdminGUI.openTeamSettingsGUI(player, gameManager);
                    player.sendMessage(Component.text("Admin GUI: configure FFA teams.", NamedTextColor.YELLOW));
                    break;
                case "End Game":
                    // Use the same logic as the /tdm end command for continuity
                    Bukkit.dispatchCommand(player, "tdm end");
                    player.sendMessage(Component.text("Admin GUI: end command executed.", NamedTextColor.RED));
                    break;
            }
            return;
        }

        if (title.equals("TDM Activate")) {
            ItemStack clicked = (ItemStack) event.getCurrentItem();
            if (clicked == null) return;
            String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
            switch (name) {
                case "Free-For-All":
                    gameManager.setGameMode(GameManager.GameMode.FREE_FOR_ALL);
                    gameManager.activateGame();
                    // activation should broadcast join options to everyone
                    gameManager.broadcastJoinOptions();
                    // send clickable start link to admins only
                    Component startMsg = Component.text("Match activated! ", NamedTextColor.GREEN)
                        .append(Component.text("[CLICK TO START]", NamedTextColor.AQUA)
                            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/tdm start"))
                            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Start the match now"))))
                        .append(Component.text(" (admins only)", NamedTextColor.GRAY));
                    for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                        if (p.hasPermission("tdm.admin")) p.sendMessage(startMsg);
                    }
                    player.sendMessage(Component.text("Admin GUI: activated Free-For-All mode.", NamedTextColor.GREEN));
                    break;
                case "4v4":
                    gameManager.setGameMode(GameManager.GameMode.FOUR_VS_FOUR);
                    gameManager.activateGame();
                    // activation should broadcast join options to everyone
                    gameManager.broadcastJoinOptions();
                    // send clickable start link to admins only
                    Component startMsg2 = Component.text("Match activated! ", NamedTextColor.GREEN)
                        .append(Component.text("[CLICK TO START]", NamedTextColor.AQUA)
                            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/tdm start 4v4"))
                            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Start the 4v4 match now"))))
                        .append(Component.text(" (admins only)", NamedTextColor.GRAY));
                    for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                        if (p.hasPermission("tdm.admin")) p.sendMessage(startMsg2);
                    }
                    player.sendMessage(Component.text("Admin GUI: activated 4v4 mode.", NamedTextColor.GREEN));
                    break;
                case "Force Auto-Assign":
                case "Force Auto-Assign (applies to next activation)":
                    // This toggle controls the behavior for the next activation only.
                    if (gameManager.isGameActive()) {
                        // cannot change the current active game's assignment behavior
                        player.sendMessage(Component.text("Cannot change team assignment mode for an active game.", NamedTextColor.RED));
                    } else {
                        boolean newState = !gameManager.getPendingForceAutoAssign();
                        gameManager.setPendingForceAutoAssign(newState);
                        player.sendMessage(Component.text("Force auto-assign for next activation " + (newState ? "ENABLED" : "DISABLED") + ".", NamedTextColor.GREEN));
                        AdminGUI.openActivateGUI(player, gameManager);
                        return; // keep inventory open
                    }
                    break;
                case "Deactivate":
                    gameManager.endGame();
                    player.sendMessage(Component.text("Admin GUI: game deactivated.", NamedTextColor.YELLOW));
                    break;
                case "Back":
                    AdminGUI.openAdminGUI(player);
                    return;
            }
            player.closeInventory();
            return;
        }

        if (title.equals("TDM Teams")) {
            ItemStack clicked = (ItemStack) event.getCurrentItem();
            if (clicked == null) return;
            String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
            if (name.equals("Red Team")) {
                AdminGUI.openTeamView(player, GameManager.Team.RED, gameManager);
                player.sendMessage(Component.text("Admin GUI: viewing Red team.", NamedTextColor.YELLOW));
            } else if (name.equals("Blue Team")) {
                AdminGUI.openTeamView(player, GameManager.Team.BLUE, gameManager);
                player.sendMessage(Component.text("Admin GUI: viewing Blue team.", NamedTextColor.YELLOW));
            }
            return;
        }

        if (title.equals("TDM Team Settings")) {
            ItemStack clicked = (ItemStack) event.getCurrentItem();
            if (clicked == null) return;
            String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
            if (name.equals("Back")) {
                AdminGUI.openAdminGUI(player);
                return;
            }
            GameManager.Team t = GameManager.Team.fromString(name);
            if (t != null) {
                gameManager.toggleFfaTeam(t);
                AdminGUI.openTeamSettingsGUI(player, gameManager); // reopen to update toggles
                player.sendMessage(Component.text(t.getDisplayName() + " toggled.", NamedTextColor.YELLOW));
            }
            return;
        }

        if (title.equals("Set Team Spawn")) {
            ItemStack clicked = (ItemStack) event.getCurrentItem();
            if (clicked == null) return;
            String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
            if (name.equals("Back")) {
                AdminGUI.openAdminGUI(player);
                return;
            }
            GameManager.Team t = GameManager.Team.fromString(name);
            if (t != null) {
                gameManager.getSpawnManager().setSpawn(t, player.getLocation());
                player.sendMessage(Component.text(t.getDisplayName() + " spawn updated.", NamedTextColor.GREEN));
            }
            return;
        }

        if (title.equals("TDM Players")) {
            ItemStack clicked = (ItemStack) event.getCurrentItem();
            if (clicked == null) return;
            String playerName = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
            if (playerName.equals("Back")) {
                AdminGUI.openAdminGUI(player);
                return;
            }
            Player target = Bukkit.getPlayer(playerName);
            if (target != null) {
                AdminGUI.openChangeTeamGUI(player, target, gameManager);
            }
            return;
        }

        if (title.endsWith(" Team")) {
            // clicking a player in the team view
            ItemStack clicked = (ItemStack) event.getCurrentItem();
            if (clicked == null) return;
            String playerName = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
            if (playerName.equals("Back")) {
                AdminGUI.openAdminGUI(player);
                return;
            }
            Player target = Bukkit.getPlayer(playerName);
            if (target != null) {
                AdminGUI.openChangeTeamGUI(player, target, gameManager);
            }
            return;
        }

        if (title.startsWith("Change Team:")) {
            ItemStack clicked = (ItemStack) event.getCurrentItem();
            if (clicked == null) return;
            String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
            if (name.equals("Back")) {
                AdminGUI.openAdminGUI(player);
                return;
            }
            String targetName = title.substring("Change Team: ".length());
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) return;
            if (name.startsWith("Set ")) {
                String teamPart = name.substring("Set ".length());
                GameManager.Team t = GameManager.Team.fromString(teamPart);
                if (t != null) {
                    gameManager.setPlayerTeam(target.getUniqueId(), t);
                    player.sendMessage(Component.text("Admin GUI: moved " + target.getName() + " to " + t.getDisplayName() + ".", NamedTextColor.GREEN));
                }
            }
            player.closeInventory();
            return;
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!gameManager.isGameStarted()) return;
        
        Player victim = event.getPlayer();
        Player killer = null;
        org.bukkit.event.entity.EntityDamageEvent last = victim.getLastDamageCause();
        org.bukkit.event.entity.EntityDamageEvent.DamageCause cause = last != null ? last.getCause() : null;
        
        // Only set killer if death was caused by a player-inflicted damage type
        boolean isPlayerCausedDeath = (cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.PROJECTILE
                || cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.MAGIC
                || cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.BLOCK_EXPLOSION);
        
        if (isPlayerCausedDeath && last instanceof org.bukkit.event.entity.EntityDamageByEntityEvent) {
            org.bukkit.entity.Entity dam = ((org.bukkit.event.entity.EntityDamageByEntityEvent) last).getDamager();
            // handle direct player damage or indirect via TNT
            if (dam instanceof Player) {
                killer = (Player) dam;
            } else if (dam instanceof TNTPrimed) {
                TNTPrimed tnt = (TNTPrimed) dam;
                if (tnt.getSource() instanceof Player) {
                    killer = (Player) tnt.getSource();
                }
            }
        }
        // For environmental deaths (FALL, FIRE, etc), killer stays null so last damager gets assist
        
        event.setCancelled(true);
        victim.spigot().respawn();
        
        gameManager.handleDeath(victim, killer, cause);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!gameManager.isGameStarted()) return;
        event.setRespawnLocation(event.getPlayer().getLocation());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!gameManager.isGameStarted()) return;
        if (!(event.getEntity() instanceof Player)) return;
        
        Player victim = (Player) event.getEntity();
        Player damager = null;
        boolean isRanged = false;
        
        // Detect if damager is player, projectile, or primed TNT
        if (event.getDamager() instanceof Player) {
            damager = (Player) event.getDamager();
            isRanged = false;
        } else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                damager = (Player) projectile.getShooter();
                isRanged = true;
            }
        } else if (event.getDamager() instanceof TNTPrimed) {
            TNTPrimed tnt = (TNTPrimed) event.getDamager();
            if (tnt.getSource() instanceof Player) {
                damager = (Player) tnt.getSource();
            }
        }
        
        if (damager != null) {
            // Check teams and friendly fire
            GameManager.Team victimTeam = gameManager.getPlayerTeam(victim.getUniqueId());
            GameManager.Team damagerTeam = gameManager.getPlayerTeam(damager.getUniqueId());
            
            if (victimTeam == damagerTeam) {
                // If friendly fire is disabled, cancel team damage
                if (!gameManager.getPlugin().getConfig().getBoolean("rules.friendly-fire", false)) {
                    event.setCancelled(true);
                    return;
                }
            }
            
            // Check for headshot based on damage type
            boolean isHeadshot = false;

            // Read configured head-region fraction (defaults to 0.25)
            double headFraction = gameManager.getPlugin().getConfig().getDouble("mechanics.head-region-percentage", 0.25);

            if (isRanged) {
                // For ranged attacks, check projectile impact location
                Projectile projectile = (Projectile) event.getDamager();
                // Only detect headshots for arrows
                if (projectile instanceof Arrow) {
                    isHeadshot = HeadshotDetector.isRangedHeadshot(projectile.getLocation(), victim, headFraction);
                }
            } else {
                // For melee attacks, use raycast detection
                isHeadshot = HeadshotDetector.isMeleeHeadshot(damager, victim, headFraction);
            }
            
            // Only mark headshot if this damage will kill the player (killing blow)
            if (isHeadshot) {
                double damageAmount = event.getFinalDamage();
                double victimHealth = victim.getHealth();
                
                // Check if this hit will kill the player
                if (damageAmount >= victimHealth) {
                    // This is a killing blow headshot - mark it
                    gameManager.markRecentHeadshot(victim.getUniqueId(), damager.getUniqueId());
                }
                // Note: Headshot counter, points, message, and sound are awarded when the kill happens in GameManager
            }
            
            gameManager.recordDamage(victim.getUniqueId(), damager.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (gameManager.isGameActive()) {
            event.getPlayer().sendMessage(Component.text("You left during an active game!"));
        }
    }
    
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        // Check if TNT block damage is disabled
        if (!gameManager.getPlugin().getConfig().getBoolean("rules.tnt-block-damage", false)) {
            event.blockList().clear();
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == org.bukkit.Material.TNT) {
            Player p = event.getPlayer();
            // convert to primed TNT and cancel placement
            event.setCancelled(true);
            TNTPrimed tnt = p.getWorld().spawn(event.getBlock().getLocation().add(0.5,0,0.5), TNTPrimed.class);
            tnt.setFuseTicks(80);
            tnt.setSource(p);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null
                && event.getClickedBlock().getType() == org.bukkit.Material.TNT
                && event.getItem() != null && event.getItem().getType() == org.bukkit.Material.FLINT_AND_STEEL) {
            Player p = event.getPlayer();
            event.setCancelled(true);
            org.bukkit.block.Block b = event.getClickedBlock();
            b.setType(org.bukkit.Material.AIR);
            TNTPrimed tnt = p.getWorld().spawn(b.getLocation().add(0.5,0,0.5), TNTPrimed.class);
            tnt.setFuseTicks(80);
            tnt.setSource(p);
            // damage the flint and steel item using Damageable meta
            ItemStack flint = event.getItem();
            if (flint != null && flint.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable dmg) {
                dmg.setDamage(dmg.getDamage() + 1);
                flint.setItemMeta((ItemMeta)dmg);
            }
        }
    }
}
