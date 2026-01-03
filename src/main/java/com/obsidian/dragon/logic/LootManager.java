package com.obsidian.dragon.logic;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages custom loot drops for the Ender Dragon.
 * Handles loading, validation, and spawning of loot items.
 */
public class LootManager {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final File lootFile;
    private final List<LootItem> lootItems;
    private final Random random;

    public LootManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.lootFile = new File(plugin.getDataFolder(), "loot.yml");
        this.lootItems = new ArrayList<>();
        this.random = ThreadLocalRandom.current();

        // Create default loot.yml if it doesn't exist
        createDefaultConfig();
        // Load the configuration
        loadConfiguration();
    }

    /**
     * Creates the default loot.yml configuration file.
     */
    private void createDefaultConfig() {
        if (!lootFile.exists()) {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                logger.warning("Failed to create plugin data folder!");
            }
            try (InputStream in = plugin.getResource("loot.yml")) {
                if (in != null) {
                    Files.copy(in, lootFile.toPath());
                    logger.info("Created default loot.yml configuration file.");
                } else {
                    // Fallback: create a basic default config if resource is missing
                    createFallbackConfig();
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to create default loot.yml!", e);
                createFallbackConfig();
            }
        }
    }

    /**
     * Creates a fallback configuration if the resource file is missing.
     */
    private void createFallbackConfig() {
        try {
            if (!lootFile.createNewFile()) {
                logger.warning("Could not create new loot.yml file (may already exist)");
                return;
            }
            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(lootFile);
            defaultConfig.set("loot", new ArrayList<>());
            defaultConfig.save(lootFile);
            logger.info("Created fallback loot.yml configuration file.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create fallback loot.yml!", e);
        }
    }

    /**
     * Loads and parses the loot configuration from loot.yml.
     * @return true if configuration loaded successfully, false otherwise
     */
    public boolean loadConfiguration() {
        lootItems.clear();

        if (!lootFile.exists()) {
            logger.warning("loot.yml not found! Creating default configuration...");
            createDefaultConfig();
        }

        try {
            FileConfiguration lootConfig = YamlConfiguration.loadConfiguration(lootFile);

            // Validate YAML structure
            if (!lootConfig.contains("loot")) {
                logger.severe("Invalid loot.yml: Missing 'loot' section!");
                return false;
            }

            List<?> lootList = lootConfig.getList("loot");
            if (lootList == null || lootList.isEmpty()) {
                logger.warning("loot.yml contains no loot items. Dragon will drop nothing.");
                return true;
            }

            // Parse each loot item
            int itemIndex = 0;
            for (Object obj : lootList) {
                itemIndex++;
                ConfigurationSection section;

                if (obj instanceof ConfigurationSection) {
                    section = (ConfigurationSection) obj;
                } else if (obj instanceof java.util.Map) {
                    // Handle maps from YAML
                    section = lootConfig.createSection("temp_" + itemIndex, (java.util.Map<?, ?>) obj);
                } else {
                    logger.warning("Skipping invalid loot item #" + itemIndex + ": Not a valid configuration section");
                    continue;
                }

                try {
                    LootItem item = parseLootItem(section, itemIndex);
                    if (item != null) {
                        lootItems.add(item);
                        logger.info("Loaded loot item #" + itemIndex + ": " + item.material.name() +
                                   " (chance: " + item.chance + "%)");
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to parse loot item #" + itemIndex + ": " + e.getMessage(), e);
                }
            }

            logger.info("Successfully loaded " + lootItems.size() + " loot item(s) from loot.yml");
            return true;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load loot.yml! Check for YAML syntax errors.", e);
            return false;
        }
    }

    /**
     * Parses a single loot item from configuration.
     */
    private LootItem parseLootItem(ConfigurationSection section, int index) {
        // Validate required fields
        if (!section.contains("material")) {
            logger.warning("Loot item #" + index + " missing required field: 'material'");
            return null;
        }

        String materialName = section.getString("material", "").toUpperCase();
        Material material;
        try {
            material = Material.valueOf(materialName);
            if (!material.isItem()) {
                logger.warning("Loot item #" + index + ": Material '" + materialName + "' is not a valid item!");
                return null;
            }
        } catch (IllegalArgumentException e) {
            logger.warning("Loot item #" + index + ": Invalid material name '" + materialName + "'");
            return null;
        }

        // Parse amount (can be single value or range)
        int minAmount = 1;
        int maxAmount = 1;
        String amountStr = section.getString("amount");
        if (amountStr != null) {
            if (amountStr.contains("-")) {
                String[] parts = amountStr.split("-");
                try {
                    minAmount = Integer.parseInt(parts[0].trim());
                    maxAmount = Integer.parseInt(parts[1].trim());
                    if (minAmount < 1 || maxAmount < 1) {
                        logger.warning("Loot item #" + index + ": Amount must be positive, using default (1)");
                        minAmount = maxAmount = 1;
                    }
                    if (minAmount > maxAmount) {
                        int temp = minAmount;
                        minAmount = maxAmount;
                        maxAmount = temp;
                    }
                } catch (NumberFormatException e) {
                    logger.warning("Loot item #" + index + ": Invalid amount range '" + amountStr + "', using default (1)");
                }
            } else {
                try {
                    minAmount = maxAmount = Math.max(1, section.getInt("amount", 1));
                } catch (Exception e) {
                    logger.warning("Loot item #" + index + ": Invalid amount value, using default (1)");
                }
            }
        }

        // Parse chance (0-100)
        double chance = section.getDouble("chance", 100.0);
        if (chance < 0 || chance > 100) {
            logger.warning("Loot item #" + index + ": Chance must be between 0-100, clamping value");
            chance = Math.max(0, Math.min(100, chance));
        }

        // Parse optional fields
        String customName = section.getString("name");
        List<String> lore = section.getStringList("lore");
        List<String> enchantments = section.getStringList("enchantments");

        return new LootItem(material, minAmount, maxAmount, chance, customName, lore, enchantments);
    }

    /**
     * Spawns loot at the specified location based on configured drop chances.
     */
    public void spawnLoot(Location location) {
        if (location == null || location.getWorld() == null) {
            logger.warning("Cannot spawn loot: Invalid location!");
            return;
        }

        if (lootItems.isEmpty()) {
            logger.info("No loot items configured, skipping loot drop.");
            return;
        }

        int droppedCount = 0;
        for (LootItem lootItem : lootItems) {
            // Check drop chance
            double roll = random.nextDouble() * 100;
            if (roll > lootItem.chance) {
                continue; // Item didn't drop
            }

            // Create the item
            ItemStack item = createItem(lootItem);
            if (item != null) {
                location.getWorld().dropItemNaturally(location, item);
                droppedCount++;
            }
        }

        logger.info("Spawned " + droppedCount + " loot item(s) at " +
                   formatLocation(location));
    }

    /**
     * Creates an ItemStack from a LootItem configuration.
     */
    private ItemStack createItem(LootItem lootItem) {
        try {
            // Determine amount
            int amount;
            if (lootItem.minAmount == lootItem.maxAmount) {
                amount = lootItem.minAmount;
            } else {
                amount = random.nextInt(lootItem.maxAmount - lootItem.minAmount + 1) + lootItem.minAmount;
            }

            ItemStack item = new ItemStack(lootItem.material, amount);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                // Set custom name using modern Adventure API
                if (lootItem.customName != null && !lootItem.customName.isEmpty()) {
                    Component nameComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(lootItem.customName);
                    meta.displayName(nameComponent);
                }

                // Set lore using modern Adventure API
                if (lootItem.lore != null && !lootItem.lore.isEmpty()) {
                    List<Component> componentLore = new ArrayList<>();
                    for (String line : lootItem.lore) {
                        componentLore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
                    }
                    meta.lore(componentLore);
                }

                item.setItemMeta(meta);
            }

            // Apply enchantments
            if (lootItem.enchantments != null && !lootItem.enchantments.isEmpty()) {
                for (String enchantStr : lootItem.enchantments) {
                    applyEnchantment(item, enchantStr);
                }
            }

            return item;

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to create item: " + lootItem.material.name(), e);
            return null;
        }
    }

    /**
     * Applies an enchantment to an item from a string like "SHARPNESS:5".
     */
    private void applyEnchantment(ItemStack item, String enchantStr) {
        try {
            String[] parts = enchantStr.split(":");
            if (parts.length != 2) {
                logger.warning("Invalid enchantment format: '" + enchantStr + "' (expected FORMAT:LEVEL)");
                return;
            }

            String enchantName = parts[0].trim().toUpperCase();
            int level;
            try {
                level = Integer.parseInt(parts[1].trim());
                if (level < 1) {
                    logger.warning("Enchantment level must be positive: " + enchantStr);
                    return;
                }
            } catch (NumberFormatException e) {
                logger.warning("Invalid enchantment level: " + enchantStr);
                return;
            }

            // Try to get enchantment by key using modern Paper Registry API
            Enchantment enchantment = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.ENCHANTMENT)
                    .get(NamespacedKey.minecraft(enchantName.toLowerCase()));
            if (enchantment == null) {
                logger.warning("Unknown enchantment: '" + enchantName + "'");
                return;
            }

            // Apply enchantment (bypass level restrictions for custom configs)
            item.addUnsafeEnchantment(enchantment, level);

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to apply enchantment: " + enchantStr, e);
        }
    }

    /**
     * Formats a location for logging.
     */
    private String formatLocation(Location loc) {
        return String.format("%s(%.1f, %.1f, %.1f)",
                           loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }

    /**
     * Reloads the loot configuration.
     * @return true if reload was successful, false otherwise
     */
    public boolean reload() {
        logger.info("Reloading loot configuration...");
        return loadConfiguration();
    }

    /**
     * Gets the number of configured loot items.
     */
    public int getLootItemCount() {
        return lootItems.size();
    }

    /**
     * Internal class representing a loot item configuration.
     */
    private static class LootItem {
        final Material material;
        final int minAmount;
        final int maxAmount;
        final double chance;
        final String customName;
        final List<String> lore;
        final List<String> enchantments;

        LootItem(Material material, int minAmount, int maxAmount, double chance,
                String customName, List<String> lore, List<String> enchantments) {
            this.material = material;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.chance = chance;
            this.customName = customName;
            this.lore = lore;
            this.enchantments = enchantments;
        }
    }
}

