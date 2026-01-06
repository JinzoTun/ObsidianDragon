package com.obsidian.dragon.gui;

import com.obsidian.dragon.ObsidianDragon;
import com.obsidian.dragon.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.boss.DragonBattle;
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
    private final EditorMenuManager editorMenuManager;

    public GUIManager(ObsidianDragon plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageUtil();
        this.pendingConfirmations = new HashMap<>();
        this.editorMenuManager = new EditorMenuManager(plugin);
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
        // Removed Configure Loot from main menu as requested; keep other buttons only.

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
     * Updated to use a 54-slot layout with a black border; buttons are placed
     * on non-border slots and the back button is bottom-center (slot 49).
     *
     * @param player The player to open the menu for
     */
    public void openLootMenu(Player player) {
        Component title = LegacyComponentSerializer.legacySection().deserialize("§6Loot Configuration");
        Inventory menu = Bukkit.createInventory(null, 54, title);

        // Border (black) and interior filler
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) borderMeta.displayName(LegacyComponentSerializer.legacySection().deserialize("§8 "));
        if (borderMeta != null) border.setItemMeta(borderMeta);

        ItemStack interior = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta intMeta = interior.getItemMeta();
        if (intMeta != null) intMeta.displayName(LegacyComponentSerializer.legacySection().deserialize("§8 "));
        if (intMeta != null) interior.setItemMeta(intMeta);

        // Fill interior then overwrite border
        for (int i = 0; i < 54; i++) menu.setItem(i, interior.clone());
        for (int i = 0; i < 54; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) {
                menu.setItem(i, border.clone());
            }
        }

        // Place Reload Loot (center of second row -> slot 13) on a non-border slot
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
        menu.setItem(13, reload);

        // Place Edit Config (slot 15) on non-border slot
        ItemStack edit = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta editMeta = edit.getItemMeta();
        if (editMeta != null) {
            editMeta.displayName(LegacyComponentSerializer.legacySection().deserialize("§a§lEdit Configuration"));
            List<Component> editLore = new ArrayList<>();
            editLore.add(LegacyComponentSerializer.legacySection().deserialize("§7Loot items: §f" + plugin.getLootManager().getLootItemCount()));
            editLore.add(Component.empty());
            editLore.add(LegacyComponentSerializer.legacySection().deserialize("§8Edit loot.yml manually in the plugins folder"));
            editMeta.lore(editLore);
            edit.setItemMeta(editMeta);
        }
        menu.setItem(15, edit);

        // Back button (slot 49) - Iron Door at bottom-center
        ItemStack back = new ItemStack(Material.IRON_DOOR);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(LegacyComponentSerializer.legacySection().deserialize("§7Back to Main Menu"));
            back.setItemMeta(backMeta);
        }
        menu.setItem(49, back);

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

        // Check if it's an editor menu
        if (title.contains("Editor") || title.contains("Loot Editor") || title.contains("Item Editor") || title.contains("Select Item")) {
            editorMenuManager.handleMenuClick(player, title, slot);
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
     * Gets the EditorMenuManager instance.
     * @return the EditorMenuManager
     */
    public EditorMenuManager getEditorMenuManager() {
        return editorMenuManager;
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
                openKillConfirmation(player);
                break;

            case 13: // Spawn Dragon
                if (!player.hasPermission("obsidiandragon.menu.use") && !player.hasPermission("obsidiandragon.admin.menu")) {
                    msg.send(player, "&cYou don't have permission to do that!");
                    return;
                }
                spawnDragon(player);
                break;

            // slot 15 removed from main menu
         }
     }

    /**
     * Handles clicks in the kill confirmation menu.
     */
    private void handleKillConfirmationClick(Player player, int slot) {
        String action = pendingConfirmations.remove(player.getUniqueId());

        if (action == null || !action.equals("kill_dragon")) {
            return;
        }

        switch (slot) {
            case 11: // Confirm
                plugin.getDragonKillManager().killDragon(player);
                break;

            case 15: // Cancel
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
            case 13: // Reload Loot (moved to slot 13)
                msg.send(player, "&eReloading loot configuration...");
                boolean success = plugin.getLootManager().reload();
                if (success) {
                    msg.send(player, "&aLoot configuration reloaded successfully!");
                    msg.send(player, "&7Loaded " + plugin.getLootManager().getLootItemCount() + " loot item(s).");
                } else {
                    msg.send(player, "&cFailed to reload loot configuration!");
                }
                break;

            case 15: // Edit Config (moved to slot 15)
                msg.send(player, "&7Edit the loot.yml file in the plugins/ODragon folder.");
                msg.send(player, "&7Then use the reload button to apply changes.");
                break;

            case 49: // Back (moved to slot 49)
                openMainMenu(player);
                break;
        }
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
