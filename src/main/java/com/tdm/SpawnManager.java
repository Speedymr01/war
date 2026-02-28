package com.tdm;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.*;

public class SpawnManager {
    private final TeamDeathmatchPlugin plugin;
    private final Map<GameManager.Team, Location> teamSpawns = new EnumMap<>(GameManager.Team.class);
    // 4v4 mode spawns per team
    private final Map<GameManager.Team, List<Location>> teamSpawns4v4 = new EnumMap<>(GameManager.Team.class);

    public SpawnManager(TeamDeathmatchPlugin plugin) {
        this.plugin = plugin;
        loadSpawns();
    }

    /**
     * Load everything again from configuration. Can be called after plugin.reloadConfig().
     */
    public void reloadSpawns() {
        teamSpawns.clear();
        teamSpawns4v4.clear();
        loadSpawns();
    }

    public void setSpawn(GameManager.Team team, Location location) {
        if (team == null || location == null) return;
        teamSpawns.put(team, location);
        saveSpawn(team, location);
    }

    public Location getSpawn(GameManager.Team team, GameManager.GameMode mode) {
        if (mode == GameManager.GameMode.FOUR_VS_FOUR) {
            List<Location> list = teamSpawns4v4.get(team);
            if (list != null && !list.isEmpty()) {
                return list.get(new Random().nextInt(list.size()));
            }
        }
        return teamSpawns.get(team);
    }

    public boolean spawnsSet() {
        return teamSpawns.containsKey(GameManager.Team.RED) && teamSpawns.containsKey(GameManager.Team.BLUE);
    }
    
    public boolean spawnsSet4v4() {
        for (GameManager.Team t : GameManager.Team.values()) {
            List<Location> list = teamSpawns4v4.get(t);
            if (list == null || list.size() < 4) return false;
        }
        return true;
    }

    private void saveSpawn(GameManager.Team team, Location location) {
        FileConfiguration config = plugin.getConfig();
        String key = team.name().toLowerCase();
        String path = "spawns." + key + ".";
        config.set(path + "world", location.getWorld().getName());
        config.set(path + "x", location.getX());
        config.set(path + "y", location.getY());
        config.set(path + "z", location.getZ());
        config.set(path + "yaw", location.getYaw());
        config.set(path + "pitch", location.getPitch());
        plugin.saveConfig();
    }

    private void loadSpawns() {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("spawns")) {
            for (String key : config.getConfigurationSection("spawns").getKeys(false)) {
                GameManager.Team t = GameManager.Team.fromString(key);
                if (t == null) continue;
                Location loc = loadSpawn(config, key);
                if (loc != null) {
                    teamSpawns.put(t, loc);
                }
            }
        }
    }

    private Location loadSpawn(FileConfiguration config, String team) {
        String path = "spawns." + team + ".";
        if (!config.contains(path + "world")) {
            return null;
        }
        String worldName = config.getString(path + "world");
        double x = config.getDouble(path + "x");
        double y = config.getDouble(path + "y");
        double z = config.getDouble(path + "z");
        float yaw = (float) config.getDouble(path + "yaw");
        float pitch = (float) config.getDouble(path + "pitch");
        return new Location(plugin.getServer().getWorld(worldName), x, y, z, yaw, pitch);
    }
}
