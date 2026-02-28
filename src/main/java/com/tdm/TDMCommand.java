package com.tdm;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TDMCommand implements CommandExecutor, TabCompleter {
    private final GameManager gameManager;
    private final SpawnManager spawnManager;

    public TDMCommand(TeamDeathmatchPlugin plugin, GameManager gameManager, SpawnManager spawnManager) {
        this.gameManager = gameManager;
        this.spawnManager = spawnManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        
        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /tdm <activate|join|start|end|setspawn|gui>", NamedTextColor.YELLOW));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "activate":
                if (!player.hasPermission("tdm.admin")) {
                    player.sendMessage(Component.text("No permission!", NamedTextColor.RED));
                    return true;
                }
                
                if (gameManager.isGameActive()) {
                    player.sendMessage(Component.text("Game already active!", NamedTextColor.RED));
                    return true;
                }
                
                GameManager.GameMode mode = GameManager.GameMode.FREE_FOR_ALL;
                if (args.length > 1 && args[1].toLowerCase().equals("4v4")) {
                    mode = GameManager.GameMode.FOUR_VS_FOUR;
                }
                
                // ensure necessary spawns are set for the selected mode
                if (mode == GameManager.GameMode.FREE_FOR_ALL) {
                    for (GameManager.Team t : gameManager.getEnabledFfaTeams()) {
                        if (spawnManager.getSpawn(t, mode) == null) {
                            player.sendMessage(Component.text("Spawn for " + t.getDisplayName() + " is not set!", NamedTextColor.RED));
                            return true;
                        }
                    }
                } else {
                    // FOUR_VS_FOUR requires at least red and blue base spawns
                    if (!spawnManager.spawnsSet()) {
                        player.sendMessage(Component.text("Set base red/blue spawns first! /tdm setspawn <team>", NamedTextColor.RED));
                        return true;
                    }
                }
                
                gameManager.setGameMode(mode);
                gameManager.activateGame();
                // broadcast join message with clickable team choices
                gameManager.broadcastJoinOptions();
                // also keep clickable start link for admins only
                Component startMsg = Component.text("Match activated! ", NamedTextColor.GREEN)
                    .append(Component.text("[CLICK TO START]", NamedTextColor.AQUA)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/tdm start"))
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Start the match now"))))
                    .append(Component.text(" (admins only)", NamedTextColor.GRAY));
                for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("tdm.admin")) {
                        p.sendMessage(startMsg);
                    }
                }
                break;

            case "join":
                if (!gameManager.isGameActive()) {
                    player.sendMessage(Component.text("No active game to join!", NamedTextColor.RED));
                    return true;
                }
                
                if (gameManager.isGameStarted()) {
                    player.sendMessage(Component.text("Game already started!", NamedTextColor.RED));
                    return true;
                }
                
                if (gameManager.isPlayerInGame(player.getUniqueId())) {
                    player.sendMessage(Component.text("You're already in the game!", NamedTextColor.RED));
                    return true;
                }
                
                if (args.length > 1) {
                    GameManager.Team t = GameManager.Team.fromString(args[1]);
                    if (t == null) {
                        player.sendMessage(Component.text("Invalid team!", NamedTextColor.RED));
                        return true;
                    }
                    gameManager.joinGame(player, t);
                } else {
                    gameManager.joinGame(player, null);
                }
                break;

            case "start":
                if (!player.hasPermission("tdm.admin")) {
                    player.sendMessage(Component.text("No permission!", NamedTextColor.RED));
                    return true;
                }
                
                if (!gameManager.isGameActive()) {
                    player.sendMessage(Component.text("No active game!", NamedTextColor.RED));
                    return true;
                }
                
                if (gameManager.isGameStarted()) {
                    player.sendMessage(Component.text("Game already started!", NamedTextColor.RED));
                    return true;
                }
                
                if (args.length > 1 && args[1].toLowerCase().equals("4v4")) {
                    GameManager.GameMode currentMode = gameManager.getCurrentGameMode();
                    if (currentMode != GameManager.GameMode.FOUR_VS_FOUR) {
                        gameManager.setGameMode(GameManager.GameMode.FOUR_VS_FOUR);
                    }
                }
                
                gameManager.startGame();
                Bukkit.broadcast(Component.text("Match started!", NamedTextColor.GOLD));
                break;

            case "end":
                if (!player.hasPermission("tdm.admin")) {
                    player.sendMessage(Component.text("No permission!", NamedTextColor.RED));
                    return true;
                }
                
                if (!gameManager.isGameActive()) {
                    player.sendMessage(Component.text("No active game!", NamedTextColor.RED));
                    return true;
                }
                
                gameManager.endGame();
                Bukkit.broadcast(Component.text("Match ended!", NamedTextColor.YELLOW));
                break;

            case "setspawn":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /tdm setspawn <team>", NamedTextColor.YELLOW));
                    return true;
                }
                GameManager.Team t = GameManager.Team.fromString(args[1]);
                if (t == null) {
                    player.sendMessage(Component.text("Invalid team!", NamedTextColor.RED));
                } else {
                    spawnManager.setSpawn(t, player.getLocation());
                    player.sendMessage(Component.text(t.getDisplayName() + " spawn set!", t.getColor()));
                }
                break;

            case "gui":
                if (!player.hasPermission("tdm.admin")) {
                    player.sendMessage(Component.text("No permission!", NamedTextColor.RED));
                    return true;
                }
                AdminGUI.openAdminGUI(player);
                break;

            default:
                player.sendMessage(Component.text("Unknown subcommand!", NamedTextColor.RED));
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            String input = args[0].toLowerCase();
            for (String subcmd : new String[]{"activate", "join", "start", "end", "setspawn", "gui"}) {
                if (subcmd.startsWith(input)) {
                    completions.add(subcmd);
                }
            }
        } else if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            String input = args[1].toLowerCase();
            
            switch (subcommand) {
                case "join":
                    // suggest enabled teams if in FFA, otherwise all if not started
                    if (gameManager.getCurrentGameMode() == GameManager.GameMode.FREE_FOR_ALL) {
                        for (GameManager.Team t : gameManager.getEnabledFfaTeams()) {
                            if (t.name().toLowerCase().startsWith(input)) completions.add(t.name().toLowerCase());
                        }
                    } else {
                        for (GameManager.Team t : new GameManager.Team[]{GameManager.Team.RED, GameManager.Team.BLUE}) {
                            if (t.name().toLowerCase().startsWith(input)) completions.add(t.name().toLowerCase());
                        }
                    }
                    break;
                case "setspawn":
                    // allow any team name
                    for (GameManager.Team t : GameManager.Team.values()) {
                        if (t.name().toLowerCase().startsWith(input)) {
                            completions.add(t.name().toLowerCase());
                        }
                    }
                    break;
                case "activate":
                case "start":
                    if ("4v4".startsWith(input)) completions.add("4v4");
                    break;
            }
        }
        
        return completions;
    }
}
