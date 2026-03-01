package com.tdm;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ClassSelectionGUI {
    
    public static void openClassSelection(Player player, GameManager gameManager) {
        List<PlayerClass> classes = PlayerClass.getAllClasses();
        int size = 54; // Always use double chest size (6 rows)
        Inventory inv = Bukkit.createInventory(null, size, Component.text("Select Your Class"));
        
        // Place classes in slots 1, 3, 5, 7 for each row (2nd, 4th, 6th, 8th positions)
        int[] slots = {1, 3, 5, 7, 10, 12, 14, 16, 19, 21, 23, 25, 28, 30, 32, 34, 37, 39, 41, 43, 46, 48, 50, 52};
        
        for (int i = 0; i < classes.size() && i < slots.length; i++) {
            inv.setItem(slots[i], createClassItem(classes.get(i)));
        }
        
        player.openInventory(inv);
    }
    
    private static ItemStack createClassItem(PlayerClass playerClass) {
        ItemStack item = new ItemStack(playerClass.getGuiIcon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(playerClass.getName(), NamedTextColor.GOLD));
        
        java.util.List<Component> loreList = new java.util.ArrayList<>();
        loreList.add(Component.text(""));
        
        // Add loadout description
        for (String itemDesc : playerClass.getLoadoutDescription()) {
            loreList.add(Component.text("• " + itemDesc, NamedTextColor.GRAY));
        }
        
        loreList.add(Component.text(""));
        loreList.add(Component.text("Click to select", NamedTextColor.YELLOW));
        meta.lore(loreList);
        
        item.setItemMeta(meta);
        return item;
    }
    
    public static PlayerClass getClassFromSlot(int slot) {
        List<PlayerClass> classes = PlayerClass.getAllClasses();
        
        // Map slots to class indices
        int[] slots = {1, 3, 5, 7, 10, 12, 14, 16, 19, 21, 23, 25, 28, 30, 32, 34, 37, 39, 41, 43, 46, 48, 50, 52};
        
        for (int i = 0; i < slots.length && i < classes.size(); i++) {
            if (slots[i] == slot) {
                return classes.get(i);
            }
        }
        
        return null;
    }
}
