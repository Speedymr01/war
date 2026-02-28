package com.tdm;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ClassSelectionGUI {
    
    public static void openClassSelection(Player player, GameManager gameManager) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("Select Your Class"));
        
        inv.setItem(1, createClassItem(Material.BOW, "Sharpshooter", 
            "Bow", "Leather Armor", "20 Arrows"));
        inv.setItem(3, createClassItem(Material.IRON_AXE, "Berserker",
            "Iron Axe", "Chain Armor", "Shield"));
        inv.setItem(5, createClassItem(Material.IRON_SWORD, "Knight",
            "Iron Sword", "Iron Armor", "Shield"));
        inv.setItem(7, createClassItem(Material.CROSSBOW, "Glass Cannon",
            "Crossbow (Quick Charge II)", "3 Harming Arrows", "10 Regular Arrows"));
        
        player.openInventory(inv);
    }
    
    private static ItemStack createClassItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.GOLD));
        
        java.util.List<Component> loreList = new java.util.ArrayList<>();
        for (String line : lore) {
            loreList.add(Component.text(line, NamedTextColor.GRAY));
        }
        meta.lore(loreList);
        
        item.setItemMeta(meta);
        return item;
    }
    
    public static PlayerClass getClassFromSlot(int slot) {
        switch (slot) {
            case 1:
                return PlayerClass.SHARPSHOOTER;
            case 3:
                return PlayerClass.BERSERKER;
            case 5:
                return PlayerClass.KNIGHT;
            case 7:
                return PlayerClass.GLASS_CANNON;
            default:
                return null;
        }
    }
}
