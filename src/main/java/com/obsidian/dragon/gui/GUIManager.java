package com.obsidian.dragon.gui;

import com.obsidian.dragon.ObsidianDragon;
import com.obsidian.dragon.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Manages all GUI operations for the ObsidianDragon plugin.
 * Handles main menu, confirmation dialogs, and loot configuration.
 */
public class GUIManager {

    private final ObsidianDragon plugin;
    private final MessageUtil msg;
    private final Map<UUID, String> pendingConfirmations;

    public GUIManager(ObsidianDragon plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageUtil();
        this.pendingConfirmations = new HashMap<>();
    }

    /**
     * Opens the main dragon menu for a player.
     *
     * @param player The player to open the menu for
     */
    public void openMainMenu(Player player) {
        Component title = Component.text("Obsidian Dragon");
        Inventory menu = Bukkit.createInventory(null, 27, title);

        // Action buttons (middle row only - centered)
        if (player.hasPermission("obsidiandragon.admin.menu")) {
            menu.setItem(11, createKillDragonButton());
        }
        menu.setItem(13, createSpawnDragonButton(player));
        if (player.hasPermission("obsidiandragon.admin.menu")) {
            menu.setItem(15, createConfigureLootButton());
        }

        player.openInventory(menu);
    }

    /**
     * Opens the confirmation menu for killing the dragon.
     *
     * @param player The player to show the confirmation to
     */
    public void openKillConfirmation(Player player) {
        Component title = LegacyComponentSerializer.legacySection().deserialize("§cConfirm Dragon Kill?");
        Inventory menu = Bukkit.createInventory(null, 27, title);

        // Confirm button (slot 11)
        ItemStack confirm = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.displayName(LegacyComponentSerializer.legacySection().deserialize("§a§lCONFIRM"));
            List<Component> confirmLore = new ArrayList<>();
            confirmLore.add(LegacyComponentSerializer.legacySection().deserialize("§7Click to kill the dragon"));
            confirmLore.add(LegacyComponentSerializer.legacySection().deserialize("§8This action cannot be undone!"));
            confirmMeta.lore(confirmLore);
            confirm.setItemMeta(confirmMeta);
        }
        menu.setItem(11, confirm);

        // Cancel button (slot 15)
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.displayName(LegacyComponentSerializer.legacySection().deserialize("§c§lCANCEL"));
            List<Component> cancelLore = new ArrayList<>();
            cancelLore.add(LegacyComponentSerializer.legacySection().deserialize("§7Click to go back"));
            cancelMeta.lore(cancelLore);
            cancel.setItemMeta(cancelMeta);
        }
        menu.setItem(15, cancel);

        // Warning item (slot 4)
        ItemStack warning = new ItemStack(Material.BARRIER);
        ItemMeta warnMeta = warning.getItemMeta();
        if (warnMeta != null) {
            warnMeta.displayName(LegacyComponentSerializer.legacySection().deserialize("§c§lWARNING"));
            List<Component> warnLore = new ArrayList<>();
            warnLore.add(LegacyComponentSerializer.legacySection().deserialize("§7This will instantly kill"));
            warnLore.add(LegacyComponentSerializer.legacySection().deserialize("§7the Ender Dragon!"));
            warnMeta.lore(warnLore);
            warning.setItemMeta(warnMeta);
        }
        menu.setItem(4, warning);


        pendingConfirmations.put(player.getUniqueId(), "kill_dragon");
        player.openInventory(menu);
    }

    /**
     * Opens the loot configuration menu for admins.
     *
     * @param player The player to open the menu for
     */
    public void openLootMenu(Player player) {
        Component title = LegacyComponentSerializer.legacySection().deserialize("§6Loot Configuration");
        Inventory menu = Bukkit.createInventory(null, 27, title);

        // Reload Loot button (slot 11)
        ItemStack reload = new ItemStack(Material.BOOK);
        ItemMeta reloadMeta = reload.getItemMeta();
        if (reloadMeta != null) {
            reloadMeta.displayName(LegacyComponentSerializer.legacySection().deserialize("§e§lReload Loot Config"));
            List<Component> reloadLore = new ArrayList<>();
            reloadLore.add(LegacyComponentSerializer.legacySection().deserialize("§7Click to reload loot.yml"));
            reloadLore.add(Component.empty());
            reloadLore.add(LegacyComponentSerializer.legacySection().deserialize("§8Current items: §6" + plugin.getLootManager().getLootItemCount()));
            reloadMeta.lore(reloadLore);
            reload.setItemMeta(reloadMeta);
        }
        menu.setItem(11, reload);

        // Edit Config button (slot 13)
        ItemStack edit = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta editMeta = edit.getItemMeta();
        if (editMeta != null) {
            editMeta.displayName(LegacyComponentSerializer.legacySection().deserialize("§a§lEdit Configuration"));
            List<Component> editLore = new ArrayList<>();
            editLore.add(LegacyComponentSerializer.legacySection().deserialize("§7Loot items: §f" + plugin.getLootManager().getLootItemCount()));
            editLore.add(Component.empty());
            editLore.add(LegacyComponentSerializer.legacySection().deserialize("§8Edit loot.yml manually"));
            editLore.add(LegacyComponentSerializer.legacySection().deserialize("§8in the plugins folder"));
            editMeta.lore(editLore);
            edit.setItemMeta(editMeta);
        }
        menu.setItem(13, edit);

        // Back button (slot 18)
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(LegacyComponentSerializer.legacySection().deserialize("§7Back to Main Menu"));
            back.setItemMeta(backMeta);
        }
        menu.setItem(18, back);


        player.openInventory(menu);
    }

    /**
     * Handles clicks in GUI menus.
     *
     * @param player    The player who clicked
     * @param title     The title of the inventory
     * @param slot      The slot that was clicked
     * @param item      The item that was clicked
     */
    public void handleMenuClick(Player player, String title, int slot, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        // Main Menu Handling
        if (title.contains("Obsidian Dragon")) {
            handleMainMenuClick(player, slot);
        }
        // Kill Confirmation Handling
        else if (title.contains("Confirm Dragon Kill")) {
            handleKillConfirmationClick(player, slot);
        }
        // Loot Configuration Handling
        else if (title.contains("Loot Configuration")) {
            handleLootMenuClick(player, slot);
        }
    }

    /**
     * Handles clicks in the main menu.
     */
    private void handleMainMenuClick(Player player, int slot) {
        switch (slot) {
            case 11: // Kill Dragon
                if (!player.hasPermission("obsidiandragon.admin.menu")) {
                    msg.send(player, "&cYou don't have permission to do that!");
                    return;
                }
                player.closeInventory();
                openKillConfirmation(player);
                break;

            case 13: // Spawn Dragon
                if (!player.hasPermission("obsidiandragon.menu.use") && !player.hasPermission("obsidiandragon.admin.menu")) {
                    msg.send(player, "&cYou don't have permission to do that!");
                    return;
                }
                player.closeInventory();
                spawnDragon(player);
                break;

            case 15: // Configure Loot
                if (!player.hasPermission("obsidiandragon.admin.menu")) {
                    msg.send(player, "&cYou don't have permission to do that!");
                    return;
                }
                player.closeInventory();
                openLootMenu(player);
                break;
        }
    }

    /**
     * Handles clicks in the kill confirmation menu.
     */
    private void handleKillConfirmationClick(Player player, int slot) {
        String action = pendingConfirmations.remove(player.getUniqueId());

        if (action == null || !action.equals("kill_dragon")) {
            player.closeInventory();
            return;
        }

        switch (slot) {
            case 11: // Confirm
                player.closeInventory();
                plugin.getDragonKillManager().killDragon(player);
                break;

            case 15: // Cancel
                player.closeInventory();
                msg.send(player, "&7Action cancelled.");
                openMainMenu(player);
                break;
        }
    }

    /**
     * Handles clicks in the loot configuration menu.
     */
    private void handleLootMenuClick(Player player, int slot) {
        switch (slot) {
            case 11: // Reload Loot
                player.closeInventory();
                msg.send(player, "&eReloading loot configuration...");
                boolean success = plugin.getLootManager().reload();
                if (success) {
                    msg.send(player, "&aLoot configuration reloaded successfully!");
                    msg.send(player, "&7Loaded " + plugin.getLootManager().getLootItemCount() + " loot item(s).");
                } else {
                    msg.send(player, "&cFailed to reload loot configuration!");
                }
                break;

            case 13: // Edit Config (info only)
                msg.send(player, "&7Edit the loot.yml file in the plugins/ODragon folder.");
                msg.send(player, "&7Then use the reload button to apply changes.");
                break;

            case 18: // Back
                player.closeInventory();
                openMainMenu(player);
                break;
        }
    }

    /**
     * Creates the dragon status item.
     */
    private ItemStack createDragonStatusItem() {
        DragonBattle battle = getDragonBattle();
        boolean isDragonAlive = battle != null && battle.getEnderDragon() != null && !battle.getEnderDragon().isDead();

        Material material = isDragonAlive ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (isDragonAlive) {
                meta.displayName(LegacyComponentSerializer.legacySection().deserialize("§a§lDRAGON ALIVE"));
                List<Component> lore = new ArrayList<>();
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§7The Ender Dragon is"));
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§7currently alive in The End"));
                lore.add(Component.empty());
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§a● Active"));
                meta.lore(lore);
            } else {
                meta.displayName(LegacyComponentSerializer.legacySection().deserialize("§c§lDRAGON DEAD"));
                List<Component> lore = new ArrayList<>();
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§7The Ender Dragon is"));
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§7currently not alive"));
                lore.add(Component.empty());
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§c● Inactive"));
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates the dragon health item.
     */
    private ItemStack createDragonHealthItem() {
        DragonBattle battle = getDragonBattle();
        EnderDragon dragon = battle != null ? battle.getEnderDragon() : null;

        ItemStack item = new ItemStack(Material.DRAGON_HEAD);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (dragon != null && !dragon.isDead()) {
                double health = dragon.getHealth();
                double maxHealth = 200.0; // Default Ender Dragon max health
                AttributeInstance maxHealthAttr = dragon.getAttribute(Attribute.MAX_HEALTH);
                if (maxHealthAttr != null) {
                    maxHealth = maxHealthAttr.getValue();
                }
                int healthPercent = (int) ((health / maxHealth) * 100);

                meta.displayName(LegacyComponentSerializer.legacySection().deserialize("§d§lENDER DRAGON"));
                List<Component> lore = new ArrayList<>();
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Health: §c" + String.format("%.1f", health) + " §7/ §c" + String.format("%.1f", maxHealth)));
                lore.add(Component.empty());

                // Health bar visualization
                String healthBar;
                if (healthPercent > 75) {
                    healthBar = "§a▓▓▓▓▓▓▓▓§8▓▓";
                } else if (healthPercent > 50) {
                    healthBar = "§e▓▓▓▓▓▓§8▓▓▓▓";
                } else if (healthPercent > 25) {
                    healthBar = "§6▓▓▓▓§8▓▓▓▓▓▓";
                } else {
                    healthBar = "§c▓▓§8▓▓▓▓▓▓▓▓";
                }
                lore.add(LegacyComponentSerializer.legacySection().deserialize(healthBar + " §f" + healthPercent + "%"));

                meta.lore(lore);
            } else {
                meta.displayName(LegacyComponentSerializer.legacySection().deserialize("§8§lENDER DRAGON"));
                List<Component> lore = new ArrayList<>();
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§7The dragon is not alive"));
                lore.add(Component.empty());
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§8No health data available"));
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates the kill dragon button.
     */
    private ItemStack createKillDragonButton() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize("§c§lKILL DRAGON"));
            List<Component> lore = new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Instantly kill the Ender Dragon"));
            lore.add(Component.empty());
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§cAdmin Only"));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§8Requires confirmation"));
            lore.add(Component.empty());
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§8Click to continue"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates the spawn dragon button.
     */
    private ItemStack createSpawnDragonButton(Player player) {
        ItemStack item = new ItemStack(Material.DRAGON_EGG);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize("§a§lSPAWN DRAGON"));
            List<Component> lore = new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Respawn the Ender Dragon"));
            lore.add(Component.empty());

            boolean isFree = player.hasPermission("obsidiandragon.spawn.free") || player.hasPermission("obsidiandragon.admin.menu");
            boolean economyEnabled = plugin.getEconomyManager().isEconomyEnabled();

            if (isFree) {
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§aFree spawn (Admin/Bypass)"));
            } else if (economyEnabled) {
                double cost = plugin.getEconomyManager().getSpawnCost();
                String formattedCost = plugin.getEconomyManager().formatCurrency(cost);
                double balance = plugin.getEconomyManager().getBalance(player);
                String formattedBalance = plugin.getEconomyManager().formatCurrency(balance);

                lore.add(LegacyComponentSerializer.legacySection().deserialize("§6Cost: §f" + formattedCost));
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§6Your Balance: §f" + formattedBalance));

                lore.add(Component.empty());
                if (balance >= cost) {
                    lore.add(LegacyComponentSerializer.legacySection().deserialize("§aYou can afford this!"));
                } else {
                    double needed = cost - balance;
                    String formattedNeeded = plugin.getEconomyManager().formatCurrency(needed);
                    lore.add(LegacyComponentSerializer.legacySection().deserialize("§cNeed " + formattedNeeded + " more"));
                }
            } else {
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§aFree (Economy disabled)"));
            }

            lore.add(Component.empty());
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§8Click to spawn"));

            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates the economy information display item.
     */
    private ItemStack createEconomyInfoItem(Player player) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize("§6§lECONOMY INFO"));
            List<Component> lore = new ArrayList<>();

            boolean economyEnabled = plugin.getEconomyManager().isEconomyEnabled();
            String provider = plugin.getEconomyManager().getProviderName();

            if (economyEnabled) {
                double balance = plugin.getEconomyManager().getBalance(player);
                String formattedBalance = plugin.getEconomyManager().formatCurrency(balance);
                double spawnCost = plugin.getEconomyManager().getSpawnCost();
                String formattedCost = plugin.getEconomyManager().formatCurrency(spawnCost);

                lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Provider: §f" + provider));
                lore.add(Component.empty());
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Your Balance:"));
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§f" + formattedBalance));
                lore.add(Component.empty());
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Dragon Spawn Cost:"));
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§f" + formattedCost));

                if (player.hasPermission("obsidiandragon.spawn.free") || player.hasPermission("obsidiandragon.admin.menu")) {
                    lore.add(Component.empty());
                    lore.add(LegacyComponentSerializer.legacySection().deserialize("§aYou have free spawn!"));
                }
            } else {
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Economy: §cDisabled"));
                lore.add(Component.empty());
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Dragon spawning is free"));
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§7for everyone!"));
            }

            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates the configure loot button.
     */
    private ItemStack createConfigureLootButton() {
        ItemStack item = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize("§d§lLOOT CONFIG"));
            List<Component> lore = new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Configure dragon loot drops"));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§7and reward settings."));
            lore.add(Component.empty());
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Configured Items: §f" + plugin.getLootManager().getLootItemCount()));
            lore.add(Component.empty());
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§8Permission: §7obsidiandragon.admin.menu"));
            lore.add(Component.empty());
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§e▶ Click to open loot menu"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }


    /**
     * Spawns the dragon for a player.
     */
    private void spawnDragon(Player player) {
        plugin.spawnDragon(player);
    }

    /**
     * Gets the DragonBattle instance safely.
     */
    private DragonBattle getDragonBattle() {
        try {
            World endWorld = Bukkit.getWorld("world_the_end");
            if (endWorld == null) {
                return null;
            }
            return endWorld.getEnderDragonBattle();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get dragon battle: " + e.getMessage());
            return null;
        }
    }

    /**
     * Clears any pending confirmations for a player.
     *
     * @param playerId The player's UUID
     */
    public void clearPendingConfirmation(UUID playerId) {
        pendingConfirmations.remove(playerId);
    }
}

