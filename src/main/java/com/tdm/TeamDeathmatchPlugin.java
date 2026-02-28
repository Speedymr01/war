package com.tdm;

import org.bukkit.plugin.java.JavaPlugin;

public class TeamDeathmatchPlugin extends JavaPlugin {
    private GameManager gameManager;
    private SpawnManager spawnManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        spawnManager = new SpawnManager(this);
        gameManager = new GameManager(this, spawnManager);
        
        TDMCommand tdmCommand = new TDMCommand(this, gameManager, spawnManager);
        getCommand("tdm").setExecutor(tdmCommand);
        getCommand("tdm").setTabCompleter(tdmCommand);
        
        getServer().getPluginManager().registerEvents(new GameListener(gameManager), this);
        
        getLogger().info("TeamDeathmatch plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (gameManager != null && gameManager.isGameActive()) {
            gameManager.endGame();
        }
        getLogger().info("TeamDeathmatch plugin disabled!");
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
