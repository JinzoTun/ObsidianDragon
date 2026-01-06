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

        // Special handling for Item Editor menu (allow open content menu on slot 29)
        if (title.contains("Item Editor")) {
            handleItemEditorClick(event, player);
            return;
        }

        // Special handling for Content menu
        if (title.contains("Content")) {
            handleContentClick(event, player);
            return;
        }

        // Cancel the event to prevent item movement for other menus
        event.setCancelled(true);

        Inventory inventory = event.getClickedInventory();

        // Handle clicks outside the menu (in player inventory)
        if (inventory == null || inventory != event.getView().getTopInventory()) {
            return;
        }

        int slot = event.getSlot();

        // Pass the click to the GUI manager
        guiManager.handleMenuClick(player, title, slot, event.getCurrentItem());
    }

    /**
     * Handles clicks in the Item Editor menu (drag and drop on slot 29).
     */
    private void handleItemEditorClick(InventoryClickEvent event, Player player) {
        Inventory topInventory = event.getView().getTopInventory();
        Inventory clickedInventory = event.getClickedInventory();

        int slot = event.getSlot();

        // If clicking in top inventory
        if (clickedInventory == topInventory) {
            // Slot 29 is the swap slot - open content editor
            if (slot == 29) {
                // open content editor to allow single-slot swap
                event.setCancelled(true);
                guiManager.getEditorMenuManager().openContentEditor(player);
                return;
            }

            // For other slots, cancel and handle as button click
            event.setCancelled(true);
            guiManager.handleMenuClick(player, event.getView().title().toString(), slot, event.getCurrentItem());
        }

        // Allow clicking in player inventory to pick items
        // Don't cancel - let them pick items from their inventory
    }

    /**
     * Handles clicks in the Content single-slot menu.
     */
    private void handleContentClick(InventoryClickEvent event, Player player) {
        Inventory topInventory = event.getView().getTopInventory();
        Inventory clickedInventory = event.getClickedInventory();
        int slot = event.getSlot();

        // If clicking in player inventory (outside top inventory) - allow normal behavior
        if (clickedInventory != topInventory) {
            return; // allow normal inventory interactions (including shift-click into the content slot)
        }

        // Use content slot index consistent with EditorMenuManager
        final int contentSlot = 22;

        // Clicking in the Content top inventory
        // Behavior:
        // - contentSlot: act like a normal chest slot (do not cancel) so players can drag/drop/take
        // - slot 49: Back (return to item editor without saving)
        // - slot 53: Save (read contentSlot content and persist)
        // - all other top slots: cancel (non-interactive)

        if (slot == contentSlot) {
            // Allow default behavior for the active content slot
            return;
        }

        // Back button (slot 49)
        if (slot == 49) {
            event.setCancelled(true);
            player.closeInventory();
            int idx = guiManager.getEditorMenuManager().getSessionIndex(player);
            if (idx >= 0) {
                guiManager.getEditorMenuManager().openItemEditorMenu(player, idx);
            } else {
                guiManager.getEditorMenuManager().clearSession(player.getUniqueId());
                guiManager.getEditorMenuManager().openEditorMenu(player);
            }
            return;
        }

        // Save button (slot 53)
        if (slot == 53) {
            event.setCancelled(true);

            // Read whatever is currently in the content slot
            ItemStack content = topInventory.getItem(contentSlot);
            // Apply to session (in-memory) and then persist
            guiManager.getEditorMenuManager().applyContentItem(player, content);

            // Close inventory and save
            player.closeInventory();
            guiManager.getEditorMenuManager().handleContentSave(player);
            return;
        }

        // Other top slots are non-interactive
        event.setCancelled(true);
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

        // Do NOT clear editor sessions just because the Item Editor closed.
        // Sessions are explicitly removed when the player presses Save/Back/Delete.
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
               title.contains("Loot Configuration") ||
               title.contains("Editor") ||
               title.contains("Loot Editor") ||
               title.contains("Item Editor") ||
               title.contains("Content");
    }
}
