package com.obsidian.dragon.listener;

import com.obsidian.dragon.ObsidianDragon;
import com.obsidian.dragon.gui.EditorMenuManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Listens for chat input from players using the editor system.
 */
public class ChatInputListener implements Listener {

    private final EditorMenuManager editorManager;
    private final ObsidianDragon plugin;

    public ChatInputListener(ObsidianDragon plugin, EditorMenuManager editorManager) {
        this.plugin = plugin;
        this.editorManager = editorManager;
    }

    /**
     * Handles chat input for editor dialogs.
     */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Check if player is awaiting input
        if (editorManager.isAwaitingChatInput(player)) {
            event.setCancelled(true);

            String message = event.getMessage();

            // Handle response on main thread
            plugin.getServer().getScheduler().runTask(plugin,
                () -> editorManager.handleDialogResponse(player, message)
            );
        }
    }
}

