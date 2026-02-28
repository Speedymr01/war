package com.tdm;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public enum PlayerClass {
    SHARPSHOOTER,
    BERSERKER,
    KNIGHT,
    GLASS_CANNON;

    public void giveKit(Player player) {
        switch (this) {
            case SHARPSHOOTER:
                player.getInventory().setHelmet(new ItemStack(Material.LEATHER_HELMET));
                player.getInventory().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
                player.getInventory().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
                player.getInventory().setBoots(new ItemStack(Material.LEATHER_BOOTS));
                player.getInventory().addItem(new ItemStack(Material.BOW));
                player.getInventory().addItem(new ItemStack(Material.ARROW, 20));
                break;
                
            case BERSERKER:
                player.getInventory().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
                player.getInventory().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
                player.getInventory().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
                player.getInventory().setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
                player.getInventory().addItem(new ItemStack(Material.IRON_AXE));
                player.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
                break;
                
            case KNIGHT:
                player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
                player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
                player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
                player.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
                break;
                
            case GLASS_CANNON:
                ItemStack crossbow = new ItemStack(Material.CROSSBOW);
                crossbow.addEnchantment(Enchantment.QUICK_CHARGE, 2);
                player.getInventory().addItem(crossbow);
                
                ItemStack harmingArrow = new ItemStack(Material.TIPPED_ARROW, 3);
                PotionMeta meta = (PotionMeta) harmingArrow.getItemMeta();
                meta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 0), true);
                harmingArrow.setItemMeta(meta);
                
                player.getInventory().addItem(harmingArrow);
                player.getInventory().addItem(new ItemStack(Material.ARROW, 10));
                break;
        }
    }
}
