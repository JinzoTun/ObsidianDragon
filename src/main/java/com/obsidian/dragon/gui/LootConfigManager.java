package com.obsidian.dragon.gui;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

/**
 * Manages reading and writing to loot.yml configuration.
 */
public class LootConfigManager {

    private final JavaPlugin plugin;
    private final File lootFile;

    public LootConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.lootFile = new File(plugin.getDataFolder(), "loot.yml");
    }

    /**
     * Gets all loot entries from the configuration.
     */
    public List<LootEntry> getAllLootEntries() {
        List<LootEntry> entries = new ArrayList<>();

        if (!lootFile.exists()) {
            return entries;
        }

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(lootFile);
            List<?> lootList = config.getList("loot");

            if (lootList == null) {
                return entries;
            }

            int index = 0;
            for (Object obj : lootList) {
                ConfigurationSection section;
                if (obj instanceof ConfigurationSection) {
                    section = (ConfigurationSection) obj;
                } else if (obj instanceof Map) {
                    section = config.createSection("temp_" + index, (Map<?, ?>) obj);
                } else {
                    continue;
                }

                // parseLootEntry always returns a non-null LootEntry (with defaults), so add directly
                LootEntry entry = parseLootEntry(section);
                entries.add(entry);
                index++;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load loot entries: " + e.getMessage());
        }

        return entries;
    }

    /**
     * Gets a specific loot entry by index.
     */
    public LootEntry getLootEntry(int index) {
        List<LootEntry> entries = getAllLootEntries();
        if (index >= 0 && index < entries.size()) {
            return entries.get(index);
        }
        return null;
    }

    /**
     * Updates a loot entry at the specified index.
     */
    public boolean updateLootEntry(int index, LootEntry newEntry) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(lootFile);
            List<Map<String, Object>> lootList = new ArrayList<>();

            // Load existing entries
            List<?> existingLoot = config.getList("loot");
            if (existingLoot != null) {
                for (Object obj : existingLoot) {
                    if (obj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = new LinkedHashMap<>((Map<String, Object>) obj);
                        lootList.add(map);
                    } else if (obj instanceof ConfigurationSection section) {
                        lootList.add(sectionToMap(section));
                    }
                }
            }

            // Update the specific entry
            if (index >= 0 && index < lootList.size()) {
                lootList.set(index, entryToMap(newEntry));
            }

            // Save back to file
            config.set("loot", lootList);
            config.save(lootFile);
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to update loot entry: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a loot entry at the specified index.
     */
    public boolean deleteLootEntry(int index) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(lootFile);
            List<Map<String, Object>> lootList = new ArrayList<>();

            // Load existing entries
            List<?> existingLoot = config.getList("loot");
            if (existingLoot != null) {
                for (Object obj : existingLoot) {
                    if (obj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = new LinkedHashMap<>((Map<String, Object>) obj);
                        lootList.add(map);
                    } else if (obj instanceof ConfigurationSection section) {
                        lootList.add(sectionToMap(section));
                    }
                }
            }

            // Remove the entry
            if (index >= 0 && index < lootList.size()) {
                lootList.remove(index);
            }

            // Save back to file
            config.set("loot", lootList);
            config.save(lootFile);
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to delete loot entry: " + e.getMessage());
            return false;
        }
    }

    /**
     * Adds a new default loot entry.
     */
    public void addNewLootEntry() {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(lootFile);
            List<Map<String, Object>> lootList = new ArrayList<>();

            // Load existing entries
            List<?> existingLoot = config.getList("loot");
            if (existingLoot != null) {
                for (Object obj : existingLoot) {
                    if (obj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = new LinkedHashMap<>((Map<String, Object>) obj);
                        lootList.add(map);
                    } else if (obj instanceof ConfigurationSection section) {
                        lootList.add(sectionToMap(section));
                    }
                }
            }

            // Create new default entry
            LootEntry newEntry = new LootEntry();
            newEntry.material = "DIAMOND";
            newEntry.amount = "1";
            newEntry.chance = 100.0;
            newEntry.customName = "&bNew Loot Item";
            newEntry.lore = new ArrayList<>();
            newEntry.lore.add("&7Edit this item");
            newEntry.enchantments = new ArrayList<>();

            lootList.add(entryToMap(newEntry));

            // Save back to file
            config.set("loot", lootList);
            config.save(lootFile);
            // done

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to add new loot entry: " + e.getMessage());
            // failed - logged
        }
    }

    /**
     * Parses a LootEntry from a ConfigurationSection.
     */
    private LootEntry parseLootEntry(ConfigurationSection section) {
        LootEntry entry = new LootEntry();

        entry.material = section.getString("material", "DIAMOND");
        entry.amount = section.getString("amount", "1");
        entry.chance = section.getDouble("chance", 100.0);
        entry.customName = section.getString("name", "");
        entry.lore = section.getStringList("lore");
        entry.enchantments = section.getStringList("enchantments");

        return entry;
    }

    /**
     * Converts a LootEntry to a Map for YAML storage.
     */
    private Map<String, Object> entryToMap(LootEntry entry) {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("material", entry.material);
        map.put("amount", entry.amount);
        map.put("chance", entry.chance);

        if (entry.customName != null && !entry.customName.isEmpty()) {
            map.put("name", entry.customName);
        }

        if (entry.lore != null && !entry.lore.isEmpty()) {
            map.put("lore", entry.lore);
        }

        if (entry.enchantments != null && !entry.enchantments.isEmpty()) {
            map.put("enchantments", entry.enchantments);
        }

        return map;
    }

    /**
     * Converts a ConfigurationSection to a Map.
     */
    private Map<String, Object> sectionToMap(ConfigurationSection section) {
        Map<String, Object> map = new LinkedHashMap<>();

        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection) {
                map.put(key, sectionToMap((ConfigurationSection) value));
            } else {
                map.put(key, value);
            }
        }

        return map;
    }

    /**
     * Represents a loot entry configuration.
     */
    public static class LootEntry implements Cloneable {
        public String material;
        public String amount;
        public double chance;
        public String customName;
        public List<String> lore;
        public List<String> enchantments;

        @Override
        public LootEntry clone() {
            try {
                LootEntry cloned = (LootEntry) super.clone();
                cloned.lore = lore != null ? new ArrayList<>(lore) : new ArrayList<>();
                cloned.enchantments = enchantments != null ? new ArrayList<>(enchantments) : new ArrayList<>();
                return cloned;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Sorts loot entries by chance (descending) and saves them back to loot.yml.
     *
     * @return true if successful
     */
    public boolean sortByChanceDescending() {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(lootFile);
            List<LootEntry> entries = getAllLootEntries();
            // Sort by chance descending
            entries.sort(Comparator.comparingDouble((LootEntry e) -> e.chance).reversed());

            // Convert to list of maps
            List<Map<String, Object>> outList = new ArrayList<>();
            for (LootEntry e : entries) outList.add(entryToMap(e));

            config.set("loot", outList);
            config.save(lootFile);
            return true;
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to sort loot entries: " + ex.getMessage());
            return false;
        }
    }
}
