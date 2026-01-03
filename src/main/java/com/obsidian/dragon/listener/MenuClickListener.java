package com.obsidian.dragon.listener;

import com.obsidian.dragon.gui.GUIManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Listens for inventory click events to handle GUI menu interactions.
 */
public class MenuClickListener implements Listener {

    private final GUIManager guiManager;

    public MenuClickListener(GUIManager guiManager) {
        this.guiManager = guiManager;
    }

    /**
     * Handles clicks in custom GUI menus.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        // Only handle player clicks
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().title().toString();

        // Check if this is one of our custom menus
        if (!isCustomMenu(title)) {
            return;
        }

        // Cancel the event to prevent item movement
        event.setCancelled(true);

        Inventory inventory = event.getClickedInventory();

        // Handle clicks outside the menu (in player inventory)
        if (inventory == null || inventory != event.getView().getTopInventory()) {
            return;
        }

        int slot = event.getSlot();
        ItemStack clickedItem = event.getCurrentItem();

        // Pass the click to the GUI manager
        guiManager.handleMenuClick(player, title, slot, clickedItem);
    }

    /**
     * Handles inventory close events to clean up pending confirmations.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        String title = event.getView().title().toString();

        // If a confirmation menu was closed without action, clear the pending confirmation
        if (title.contains("Confirm Dragon Kill")) {
            guiManager.clearPendingConfirmation(player.getUniqueId());
        }
    }

    /**
     * Checks if an inventory title belongs to one of our custom menus.
     *
     * @param title The inventory title
     * @return true if it's a custom menu
     */
    private boolean isCustomMenu(String title) {
        return title.contains("Obsidian Dragon") ||
               title.contains("Confirm Dragon Kill") ||
               title.contains("Loot Configuration");
    }
}

