package com.tdm;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerClass {
    private final String id;
    private final String name;
    private final Material helmet;
    private final Material chestplate;
    private final Material leggings;
    private final Material boots;
    private final Material weapon;
    private final Material offHand;
    private final List<ItemStack> extraItems;
    private final Map<Material, Map<Enchantment, Integer>> enchantments;
    private final int healthBonus;
    @SuppressWarnings("unused") // Reserved for future armor bonus feature
    private final int armorBonus;
    private final Material guiIcon;
    
    private static final Map<String, PlayerClass> loadedClasses = new HashMap<>();
    
    public PlayerClass(String id, String name, Material helmet, Material chestplate, 
                      Material leggings, Material boots, Material weapon, Material offHand,
                      List<ItemStack> extraItems, Map<Material, Map<Enchantment, Integer>> enchantments,
                      int healthBonus, int armorBonus, Material guiIcon) {
        this.id = id;
        this.name = name;
        this.helmet = helmet;
        this.chestplate = chestplate;
        this.leggings = leggings;
        this.boots = boots;
        this.weapon = weapon;
        this.offHand = offHand;
        this.extraItems = extraItems;
        this.enchantments = enchantments;
        this.healthBonus = healthBonus;
        this.armorBonus = armorBonus;
        this.guiIcon = guiIcon;
    }
    
    public static void loadClassesFromConfig(ConfigurationSection config) {
        loadedClasses.clear();
        
        if (config == null) return;
        
        for (String classId : config.getKeys(false)) {
            ConfigurationSection classSection = config.getConfigurationSection(classId);
            if (classSection == null || !classSection.getBoolean("enabled", true)) continue;
            
            try {
                String name = classSection.getString("name", classId);
                Material helmet = parseMaterial(classSection.getString("helmet", "NONE"));
                Material chestplate = parseMaterial(classSection.getString("chestplate", "NONE"));
                Material leggings = parseMaterial(classSection.getString("leggings", "NONE"));
                Material boots = parseMaterial(classSection.getString("boots", "NONE"));
                Material weapon = parseMaterial(classSection.getString("weapon", "NONE"));
                Material offHand = parseMaterial(classSection.getString("off-hand", "NONE"));
                int healthBonus = classSection.getInt("health-bonus", 0);
                int armorBonus = classSection.getInt("armor-bonus", 0);
                
                // Parse extra items
                List<ItemStack> extraItems = new ArrayList<>();
                List<String> extraItemsConfig = classSection.getStringList("extra-items");
                for (String itemStr : extraItemsConfig) {
                    ItemStack item = parseItemStack(itemStr);
                    if (item != null) extraItems.add(item);
                }
                
                // Parse enchantments
                Map<Material, Map<Enchantment, Integer>> enchantments = new HashMap<>();
                List<String> enchantmentsConfig = classSection.getStringList("enchantments");
                for (String enchStr : enchantmentsConfig) {
                    parseEnchantment(enchStr, enchantments);
                }
                
                // Get GUI icon from config, or auto-determine
                Material guiIcon;
                String iconConfig = classSection.getString("gui-icon", "AUTO");
                if (iconConfig.equalsIgnoreCase("AUTO")) {
                    // Auto-determine: use weapon, or first armor piece, or default
                    guiIcon = weapon != null && weapon != Material.AIR ? weapon :
                              helmet != null && helmet != Material.AIR ? helmet :
                              chestplate != null && chestplate != Material.AIR ? chestplate :
                              Material.IRON_SWORD;
                } else {
                    guiIcon = parseMaterial(iconConfig);
                    if (guiIcon == null) guiIcon = Material.IRON_SWORD;
                }
                
                PlayerClass playerClass = new PlayerClass(classId, name, helmet, chestplate, leggings, boots,
                                                         weapon, offHand, extraItems, enchantments, 
                                                         healthBonus, armorBonus, guiIcon);
                loadedClasses.put(classId, playerClass);
            } catch (Exception e) {
                System.err.println("Failed to load class " + classId + ": " + e.getMessage());
            }
        }
    }
    
    private static Material parseMaterial(String materialStr) {
        if (materialStr == null || materialStr.equalsIgnoreCase("NONE")) return null;
        try {
            return Material.valueOf(materialStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid material: " + materialStr);
            return null;
        }
    }
    
    @SuppressWarnings("deprecation") // Using stable API methods for 1.21.1 compatibility
    private static ItemStack parseItemStack(String itemStr) {
        String[] parts = itemStr.split(":");
        if (parts.length < 1) return null;
        
        Material material = parseMaterial(parts[0]);
        if (material == null) return null;
        
        int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
        
        ItemStack item = new ItemStack(material, amount);
        
        // Special handling for tipped arrows
        if (material == Material.TIPPED_ARROW && parts.length > 2) {
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            if (meta != null) {
                PotionEffectType effectType = parsePotionEffect(parts[2]);
                if (effectType != null) {
                    int amplifier = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;
                    // Use base potion type for better compatibility
                    org.bukkit.potion.PotionType baseType = getPotionTypeFromEffect(effectType, amplifier);
                    if (baseType != null) {
                        meta.setBasePotionType(baseType);
                    } else {
                        // Fallback to custom effect
                        meta.addCustomEffect(new PotionEffect(effectType, 1, amplifier), true);
                    }
                    item.setItemMeta(meta);
                }
            }
        }
        
        // Special handling for splash potions and lingering potions
        if ((material == Material.SPLASH_POTION || material == Material.LINGERING_POTION) && parts.length > 2) {
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            if (meta != null) {
                PotionEffectType effectType = parsePotionEffect(parts[2]);
                if (effectType != null) {
                    int amplifier = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;
                    // Use base potion type for better compatibility
                    org.bukkit.potion.PotionType baseType = getPotionTypeFromEffect(effectType, amplifier);
                    if (baseType != null) {
                        meta.setBasePotionType(baseType);
                    } else {
                        // Fallback to custom effect
                        int duration = parts.length > 4 ? Integer.parseInt(parts[4]) : 1;
                        meta.addCustomEffect(new PotionEffect(effectType, duration, amplifier), true);
                    }
                    item.setItemMeta(meta);
                } else {
                    System.err.println("Failed to parse potion effect: " + parts[2]);
                }
            }
        }
        
        return item;
    }
    
    /**
     * Get the appropriate PotionType for an effect and amplifier
     */
    private static org.bukkit.potion.PotionType getPotionTypeFromEffect(PotionEffectType effectType, int amplifier) {
        if (effectType == PotionEffectType.INSTANT_DAMAGE) {
            return amplifier >= 1 ? org.bukkit.potion.PotionType.STRONG_HARMING : org.bukkit.potion.PotionType.HARMING;
        } else if (effectType == PotionEffectType.INSTANT_HEALTH) {
            return amplifier >= 1 ? org.bukkit.potion.PotionType.STRONG_HEALING : org.bukkit.potion.PotionType.HEALING;
        } else if (effectType == PotionEffectType.REGENERATION) {
            return amplifier >= 1 ? org.bukkit.potion.PotionType.STRONG_REGENERATION : org.bukkit.potion.PotionType.REGENERATION;
        } else if (effectType == PotionEffectType.SPEED) {
            return amplifier >= 1 ? org.bukkit.potion.PotionType.STRONG_SWIFTNESS : org.bukkit.potion.PotionType.SWIFTNESS;
        } else if (effectType == PotionEffectType.STRENGTH) {
            return amplifier >= 1 ? org.bukkit.potion.PotionType.STRONG_STRENGTH : org.bukkit.potion.PotionType.STRENGTH;
        } else if (effectType == PotionEffectType.JUMP_BOOST) {
            return amplifier >= 1 ? org.bukkit.potion.PotionType.STRONG_LEAPING : org.bukkit.potion.PotionType.LEAPING;
        } else if (effectType == PotionEffectType.SLOWNESS) {
            return amplifier >= 1 ? org.bukkit.potion.PotionType.STRONG_SLOWNESS : org.bukkit.potion.PotionType.SLOWNESS;
        } else if (effectType == PotionEffectType.POISON) {
            return amplifier >= 1 ? org.bukkit.potion.PotionType.STRONG_POISON : org.bukkit.potion.PotionType.POISON;
        } else if (effectType == PotionEffectType.WEAKNESS) {
            return org.bukkit.potion.PotionType.WEAKNESS;
        }
        return null; // No matching base potion type
    }
    
    /**
     * Parse potion effect type from string, handling common aliases
     */
    @SuppressWarnings("deprecation")
    private static PotionEffectType parsePotionEffect(String effectName) {
        effectName = effectName.toUpperCase();
        
        // Handle common aliases
        switch (effectName) {
            case "HARM":
            case "INSTANT_DAMAGE":
            case "DAMAGE":
                return PotionEffectType.INSTANT_DAMAGE;
            case "HEAL":
            case "INSTANT_HEALTH":
            case "HEALTH":
                return PotionEffectType.INSTANT_HEALTH;
            case "REGEN":
            case "REGENERATION":
                return PotionEffectType.REGENERATION;
            case "SPEED":
            case "SWIFTNESS":
                return PotionEffectType.SPEED;
            case "STRENGTH":
                return PotionEffectType.STRENGTH;
            case "JUMP":
            case "JUMP_BOOST":
                return PotionEffectType.JUMP_BOOST;
            case "SLOW":
            case "SLOWNESS":
                return PotionEffectType.SLOWNESS;
            case "POISON":
                return PotionEffectType.POISON;
            case "WEAKNESS":
                return PotionEffectType.WEAKNESS;
            default:
                // Try to get by name
                try {
                    return PotionEffectType.getByName(effectName);
                } catch (Exception e) {
                    return null;
                }
        }
    }
    
    @SuppressWarnings("deprecation") // Using stable API methods for 1.21.1 compatibility
    private static void parseEnchantment(String enchStr, Map<Material, Map<Enchantment, Integer>> enchantments) {
        String[] parts = enchStr.split(":");
        if (parts.length < 3) return;
        
        Material material = parseMaterial(parts[0]);
        if (material == null) return;
        
        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(parts[1].toLowerCase()));
        if (enchantment == null) return;
        
        int level = Integer.parseInt(parts[2]);
        
        enchantments.computeIfAbsent(material, k -> new HashMap<>()).put(enchantment, level);
    }
    
    public void giveKit(Player player) {
        player.getInventory().clear();
        
        // Set armor
        if (helmet != null && helmet != Material.AIR) {
            player.getInventory().setHelmet(new ItemStack(helmet));
        }
        if (chestplate != null && chestplate != Material.AIR) {
            player.getInventory().setChestplate(new ItemStack(chestplate));
        }
        if (leggings != null && leggings != Material.AIR) {
            player.getInventory().setLeggings(new ItemStack(leggings));
        }
        if (boots != null && boots != Material.AIR) {
            player.getInventory().setBoots(new ItemStack(boots));
        }
        
        // Give weapon
        if (weapon != null && weapon != Material.AIR) {
            ItemStack weaponItem = new ItemStack(weapon);
            applyEnchantments(weaponItem, weapon);
            player.getInventory().addItem(weaponItem);
        }
        
        // Set off-hand
        if (offHand != null && offHand != Material.AIR) {
            player.getInventory().setItemInOffHand(new ItemStack(offHand));
        }
        
        // Give extra items
        for (ItemStack item : extraItems) {
            ItemStack clone = item.clone();
            applyEnchantments(clone, clone.getType());
            player.getInventory().addItem(clone);
        }
        
        // Apply health bonus
        if (healthBonus != 0) {
            double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth + healthBonus);
            player.setHealth(maxHealth + healthBonus);
        }
    }
    
    private void applyEnchantments(ItemStack item, Material material) {
        if (enchantments.containsKey(material)) {
            Map<Enchantment, Integer> itemEnchants = enchantments.get(material);
            for (Map.Entry<Enchantment, Integer> entry : itemEnchants.entrySet()) {
                item.addUnsafeEnchantment(entry.getKey(), entry.getValue());
            }
        }
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public Material getGuiIcon() {
        return guiIcon;
    }
    
    /**
     * Get a list of all items this class has (for tooltip display)
     */
    public List<String> getLoadoutDescription() {
        List<String> description = new ArrayList<>();
        
        // Armor
        if (helmet != null && helmet != Material.AIR) {
            description.add(formatItemName(helmet));
        }
        if (chestplate != null && chestplate != Material.AIR) {
            description.add(formatItemName(chestplate));
        }
        if (leggings != null && leggings != Material.AIR) {
            description.add(formatItemName(leggings));
        }
        if (boots != null && boots != Material.AIR) {
            description.add(formatItemName(boots));
        }
        
        // Weapon
        if (weapon != null && weapon != Material.AIR) {
            description.add(formatItemName(weapon));
        }
        
        // Off-hand
        if (offHand != null && offHand != Material.AIR) {
            description.add(formatItemName(offHand));
        }
        
        // Extra items
        for (ItemStack item : extraItems) {
            String itemDesc = formatItemName(item.getType());
            if (item.getAmount() > 1) {
                itemDesc += " x" + item.getAmount();
            }
            description.add(itemDesc);
        }
        
        return description;
    }
    
    /**
     * Format material name to be more readable
     */
    private String formatItemName(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        // Capitalize first letter of each word
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        return result.toString().trim();
    }
    
    public static PlayerClass getClass(String id) {
        return loadedClasses.get(id);
    }
    
    public static List<PlayerClass> getAllClasses() {
        return new ArrayList<>(loadedClasses.values());
    }
    
    /**
     * Get the first enabled class as a fallback default
     */
    public static PlayerClass getFirstEnabledClass() {
        if (loadedClasses.isEmpty()) return null;
        return loadedClasses.values().iterator().next();
    }
    
    public static void clearClasses() {
        loadedClasses.clear();
    }
}
