package com.tdm;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundManager {
    
    public static void playKillSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
    }
    
    public static void playAssistSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
    }
    
    public static void playDeathSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_DEATH, 1.0f, 1.0f);
    }
    
    public static void playRespawnSound(Player player) {
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.5f, 1.0f);
    }
    
    public static void playGameStartSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
    }
    
    public static void playVictorySound(Player player) {
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }
    
    public static void playDefeatSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.8f);
    }
    
    public static void playHeadshotSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.8f);
    }
}
