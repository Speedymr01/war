package com.tdm;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TeamDeathmatchPlugin extends JavaPlugin {
    private GameManager gameManager;
    private SpawnManager spawnManager;
    private static TeamDeathmatchPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        
        // Detect if this is a reload
        boolean isReload = gameManager != null;
        if (isReload) {
            getLogger().warning("Plugin reload detected! Cleaning up active games...");
            cleanupOnReload();
        }
        
        saveDefaultConfig();

        // Load classes from config
        PlayerClass.loadClassesFromConfig(getConfig().getConfigurationSection("classes"));

        spawnManager = new SpawnManager(this);
        gameManager = new GameManager(this, spawnManager);

        TDMCommand tdmCommand = new TDMCommand(this, gameManager, spawnManager);
        getCommand("tdm").setExecutor(tdmCommand);
        getCommand("tdm").setTabCompleter(tdmCommand);

        getServer().getPluginManager().registerEvents(new GameListener(gameManager), this);

        getLogger().info("TeamDeathmatch plugin enabled!");
        getLogger().info("Loaded " + PlayerClass.getAllClasses().size() + " classes from config");
        
        if (isReload) {
            getLogger().warning("Plugin reloaded. Active games have been ended and players reset.");
        }
    }

    @Override
    public void onDisable() {
        if (gameManager != null && gameManager.isGameActive()) {
            getLogger().info("Ending active game due to plugin disable...");
            gameManager.endGame();
        }
        
        // Clean up all player states
        cleanupAllPlayers();
        
        getLogger().info("TeamDeathmatch plugin disabled!");
    }
    
    /**
     * Clean up when a reload is detected
     */
    private void cleanupOnReload() {
        if (gameManager != null && gameManager.isGameActive()) {
            gameManager.endGame();
        }
        cleanupAllPlayers();
    }
    
    /**
     * Reset all players to a clean state
     */
    private void cleanupAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Reset scoreboard
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            
            // Clear inventory if they were in a game
            if (gameManager != null && gameManager.isPlayerInGame(player.getUniqueId())) {
                player.getInventory().clear();
                player.setHealth(20.0);
                player.setFoodLevel(20);
                player.setSaturation(20.0f);
            }
        }
    }

    public GameManager getGameManager() {
        return gameManager;
    }
    
    public static TeamDeathmatchPlugin getInstance() {
        return instance;
    }
}
