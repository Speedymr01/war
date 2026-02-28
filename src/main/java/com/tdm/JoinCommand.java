package com.tdm;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JoinCommand implements CommandExecutor {
    private final GameManager gameManager;

    public JoinCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        
        Player player = (Player) sender;

        if (!gameManager.isGameActive()) {
            player.sendMessage(Component.text("No active game to join!", NamedTextColor.RED));
            return true;
        }

        if (gameManager.isGameStarted()) {
            player.sendMessage(Component.text("Game already started!", NamedTextColor.RED));
            return true;
        }

        gameManager.joinGame(player, null);
        return true;
    }
}
