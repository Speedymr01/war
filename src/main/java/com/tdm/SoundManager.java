package com.tdm;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class SoundManager {
    
    public static void playKillSound(Player player) {
        TeamDeathmatchPlugin plugin = TeamDeathmatchPlugin.getInstance();
        String soundName = plugin.getConfig().getString("sounds.kill-sound-name", "ENTITY_PLAYER_LEVELUP");
        float volume = (float) plugin.getConfig().getDouble("sounds.kill-sound-volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("sounds.kill-sound-pitch", 1.5);
        playSound(player, soundName, volume, pitch);
    }
    
    public static void playAssistSound(Player player) {
        TeamDeathmatchPlugin plugin = TeamDeathmatchPlugin.getInstance();
        String soundName = plugin.getConfig().getString("sounds.assist-sound-name", "ENTITY_EXPERIENCE_ORB_PICKUP");
        float volume = (float) plugin.getConfig().getDouble("sounds.assist-sound-volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("sounds.assist-sound-pitch", 1.2);
        playSound(player, soundName, volume, pitch);
    }
    
    public static void playDeathSound(Player player) {
        TeamDeathmatchPlugin plugin = TeamDeathmatchPlugin.getInstance();
        String soundName = plugin.getConfig().getString("sounds.death-sound-name", "ENTITY_PLAYER_DEATH");
        float volume = (float) plugin.getConfig().getDouble("sounds.death-sound-volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("sounds.death-sound-pitch", 1.0);
        playSound(player, soundName, volume, pitch);
    }
    
    public static void playRespawnSound(Player player) {
        TeamDeathmatchPlugin plugin = TeamDeathmatchPlugin.getInstance();
        String soundName = plugin.getConfig().getString("sounds.respawn-sound-name", "ITEM_TOTEM_USE");
        float volume = (float) plugin.getConfig().getDouble("sounds.respawn-sound-volume", 0.5);
        float pitch = (float) plugin.getConfig().getDouble("sounds.respawn-sound-pitch", 1.0);
        playSound(player, soundName, volume, pitch);
    }
    
    public static void playGameStartSound(Player player) {
        TeamDeathmatchPlugin plugin = TeamDeathmatchPlugin.getInstance();
        String soundName = plugin.getConfig().getString("sounds.game-start-sound-name", "ENTITY_ENDER_DRAGON_GROWL");
        float volume = (float) plugin.getConfig().getDouble("sounds.game-start-sound-volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("sounds.game-start-sound-pitch", 1.0);
        playSound(player, soundName, volume, pitch);
    }
    
    public static void playVictorySound(Player player) {
        TeamDeathmatchPlugin plugin = TeamDeathmatchPlugin.getInstance();
        String soundName = plugin.getConfig().getString("sounds.victory-sound-name", "UI_TOAST_CHALLENGE_COMPLETE");
        float volume = (float) plugin.getConfig().getDouble("sounds.victory-sound-volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("sounds.victory-sound-pitch", 1.0);
        playSound(player, soundName, volume, pitch);
    }
    
    public static void playDefeatSound(Player player) {
        TeamDeathmatchPlugin plugin = TeamDeathmatchPlugin.getInstance();
        String soundName = plugin.getConfig().getString("sounds.defeat-sound-name", "ENTITY_WITHER_SPAWN");
        float volume = (float) plugin.getConfig().getDouble("sounds.defeat-sound-volume", 0.5);
        float pitch = (float) plugin.getConfig().getDouble("sounds.defeat-sound-pitch", 0.8);
        playSound(player, soundName, volume, pitch);
    }
    
    public static void playHeadshotSound(Player player) {
        TeamDeathmatchPlugin plugin = TeamDeathmatchPlugin.getInstance();
        String soundName = plugin.getConfig().getString("sounds.headshot-sound-name", "BLOCK_GLASS_BREAK");
        float volume = (float) plugin.getConfig().getDouble("sounds.headshot-sound-volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("sounds.headshot-sound-pitch", 1.8);
        playSound(player, soundName, volume, pitch);
    }
    
    /**
     * Play a sound - handles both Minecraft sounds and custom sounds
     */
    private static void playSound(Player player, String soundName, float volume, float pitch) {
        try {
            // Try to parse as Minecraft sound enum
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, SoundCategory.PLAYERS, volume, pitch);
        } catch (IllegalArgumentException e) {
            // Not a Minecraft sound, treat as custom sound (e.g., "tdm.kill")
            player.playSound(player.getLocation(), soundName, SoundCategory.PLAYERS, volume, pitch);
        }
    }
}
