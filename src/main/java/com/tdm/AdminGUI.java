package com.tdm;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminGUI {
    public static void openAdminGUI(Player admin) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("TDM Admin"));
        inv.setItem(0, createItem(Material.GREEN_CONCRETE, "Activate"));
        inv.setItem(1, createItem(Material.CHEST, "Set Team Spawns"));
        inv.setItem(2, createItem(Material.EMERALD, "Start"));
        // slots 3-5 left empty
        inv.setItem(6, createItem(Material.PLAYER_HEAD, "Players"));
        inv.setItem(7, createItem(Material.CHEST, "Team Settings"));
        inv.setItem(8, createItem(Material.BARRIER, "End Game"));
        admin.openInventory(inv);
    }

    public static void openActivateGUI(Player admin, GameManager gameManager) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("TDM Activate"));
        inv.setItem(0, createItem(Material.ARROW, "Back"));
        inv.setItem(1, createItem(Material.DIAMOND_SWORD, "Free-For-All"));
        boolean randomState = gameManager.getPendingForceAutoAssign();
        Material randMat = randomState ? Material.LIME_DYE : Material.SLIME_BALL;
        String randName = "Force Auto-Assign" + (randomState ? " (applies to next activation)" : " (applies to next activation)");
        ItemStack randItem = createItem(randMat, randName);
        ItemMeta randMeta = randItem.getItemMeta();
        List<Component> lore = new ArrayList<>();
        if (gameManager.isGameActive()) {
            lore.add(Component.text("Click to broadcast join link", NamedTextColor.YELLOW));
        } else {
            lore.add(Component.text("Click to toggle random-only assignment", NamedTextColor.YELLOW));
        }
        randMeta.lore(lore);
        randItem.setItemMeta(randMeta);
        inv.setItem(2, randItem);
        inv.setItem(3, createItem(Material.DIAMOND_HELMET, "4v4"));
        inv.setItem(7, createItem(Material.BARRIER, "Deactivate"));
        admin.openInventory(inv);
    }

    public static void openTeamView(Player admin, GameManager.Team team, GameManager gameManager) {
        List<Player> players = new ArrayList<>();
        for (java.util.UUID uuid : gameManager.getPlayersInTeam(team)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) players.add(p);
        }

        Inventory inv = Bukkit.createInventory(null, 9, Component.text(team.getDisplayName()));
        int slot = 0;
        for (Player p : players) {
            ItemStack is = createItem(Material.PLAYER_HEAD, p.getName());
            inv.setItem(slot++, is);
            if (slot >= 8) break;
        }
        // last slot as back button
        inv.setItem(8, createItem(Material.ARROW, "Back"));
        admin.openInventory(inv);
    }

    public static void openChangeTeamGUI(Player admin, Player target) {
        List<GameManager.Team> teams = Arrays.asList(GameManager.Team.values());
        int size = 9 * ((teams.size() + 8) / 9);
        Inventory inv = Bukkit.createInventory(null, size, Component.text("Change Team: " + target.getName()));
        int slot = 0;
        for (GameManager.Team t : teams) {
            Material mat = Material.WHITE_CONCRETE;
            try {
                mat = Material.valueOf(t.name() + "_CONCRETE");
            } catch (IllegalArgumentException ignored) {}
            ItemStack item = createItem(mat, "Set " + t.getDisplayName());
            inv.setItem(slot++, item);
            if (slot >= size - 1) break;
        }
        // last slot back
        inv.setItem(size - 1, createItem(Material.ARROW, "Back"));
        admin.openInventory(inv);
    }

    public static ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.GREEN));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Opens the inventory used to assign spawns for each team. Clicking a team sets the spawn
     * to the current player's location. The last slot is a back button.
     */
    public static void openSpawnGUI(Player admin, GameManager gameManager) {
        List<GameManager.Team> teams = Arrays.asList(GameManager.Team.values());
        int size = 9 * ((teams.size() + 8) / 9);
        Inventory inv = Bukkit.createInventory(null, size, Component.text("Set Team Spawn"));
        int slot = 0;
        for (GameManager.Team t : teams) {
            Material mat = Material.WHITE_CONCRETE;
            try {
                mat = Material.valueOf(t.name() + "_CONCRETE");
            } catch (IllegalArgumentException ignored) {}
            inv.setItem(slot++, createItem(mat, t.getDisplayName()));
        }
        if (size > 0) {
            inv.setItem(size - 1, createItem(Material.ARROW, "Back"));
        }
        admin.openInventory(inv);
    }

    public static void openPlayersGUI(Player admin, GameManager gameManager) {
        int size = 54; // 6 rows
        Inventory inv = Bukkit.createInventory(null, size, Component.text("TDM Players"));
        int slot = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack is = createItem(Material.PLAYER_HEAD, p.getName());
            inv.setItem(slot++, is);
            if (slot >= size - 1) break; // leave last slot for back
        }
        inv.setItem(size - 1, createItem(Material.ARROW, "Back"));
        admin.openInventory(inv);
    }
    public static void openTeamSettingsGUI(Player admin, GameManager gameManager) {
        int size = 9 * ((GameManager.Team.values().length + 8) / 9);
        Inventory inv = Bukkit.createInventory(null, size, Component.text("TDM Team Settings"));
        int slot = 0;
        for (GameManager.Team t : GameManager.Team.values()) {
            Material mat = Material.WHITE_WOOL;
            // aqua uses light blue wool since aqua and white conflict
            if (t == GameManager.Team.AQUA) {
                mat = Material.LIGHT_BLUE_WOOL;
            } else {
                try {
                    // try to pick a wool color matching team color (fallback to white)
                    mat = Material.valueOf(t.name() + "_WOOL");
                } catch (IllegalArgumentException ignored) {}
            }

            ItemStack item = createItem(mat, t.getDisplayName());
            ItemMeta meta = item.getItemMeta();
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(gameManager.isTeamEnabled(t) ? "Enabled" : "Disabled", NamedTextColor.GRAY));
            lore.add(Component.text("Click to toggle", NamedTextColor.YELLOW));
            meta.lore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }
        admin.openInventory(inv);
    }
}
