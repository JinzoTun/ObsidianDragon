package com.obsidian.dragon.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Utility class for handling plugin messages with consistent formatting.
 */
public class MessageUtil {

    private static final String PREFIX = "&e&lObsidianDragon &8» &r";
    private final JavaPlugin plugin;

    public MessageUtil(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Sends a message to a command sender with the plugin prefix.
     *
     * @param sender The command sender
     * @param message The message to send
     */
    public void send(CommandSender sender, String message) {
        if (sender == null || message == null) {
            return;
        }
        sender.sendMessage(colorize(PREFIX + message));
    }

    /**
     * Sends a message from config to a command sender with the plugin prefix.
     *
     * @param sender The command sender
     * @param configPath The config path for the message
     * @param defaultMessage The default message if config is missing
     */
    public void sendConfig(CommandSender sender, String configPath, String defaultMessage) {
        if (sender == null) {
            return;
        }
        String message = plugin.getConfig().getString(configPath, defaultMessage);
        send(sender, message);
    }

    /**
     * Sends a message from config to a command sender with the plugin prefix and placeholder replacement.
     *
     * @param sender The command sender
     * @param configPath The config path for the message
     * @param defaultMessage The default message if config is missing
     * @param placeholder The placeholder to replace (e.g., "%player%")
     * @param value The value to replace it with
     */
    public void sendConfig(CommandSender sender, String configPath, String defaultMessage, String placeholder, String value) {
        if (sender == null) {
            return;
        }
        String message = plugin.getConfig().getString(configPath, defaultMessage);
        if (placeholder != null && value != null) {
            message = message.replace(placeholder, value);
        }
        send(sender, message);
    }

    /**
     * Broadcasts a message to all players with the plugin prefix.
     *
     * @param message The message to broadcast
     */
    public void broadcast(String message) {
        if (message == null) {
            return;
        }
        Component component = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(PREFIX + message);
        Bukkit.broadcast(component);
    }

    /**
     * Broadcasts a message from config to all players with the plugin prefix.
     *
     * @param configPath The config path for the message
     * @param defaultMessage The default message if config is missing
     */
    @SuppressWarnings("unused")
    public void broadcastConfig(String configPath, String defaultMessage) {
        String message = plugin.getConfig().getString(configPath, defaultMessage);
        broadcast(message);
    }

    /**
     * Broadcasts a message from config with placeholder replacement.
     *
     * @param configPath The config path for the message
     * @param defaultMessage The default message if config is missing
     * @param placeholder The placeholder to replace (e.g., "%player%")
     * @param value The value to replace it with
     */
    public void broadcastConfig(String configPath, String defaultMessage, String placeholder, String value) {
        String message = plugin.getConfig().getString(configPath, defaultMessage);
        if (placeholder != null && value != null) {
            message = message.replace(placeholder, value);
        }
        broadcast(message);
    }

    /**
     * Colorizes a message by replacing color codes.
     *
     * @param message The message to colorize
     * @return The colorized message
     */
    private String colorize(String message) {
        if (message == null) {
            return "";
        }
        return message.replace("&", "§");
    }

    /**
     * Gets the plugin prefix without color codes applied.
     *
     * @return The prefix
     */
    @SuppressWarnings("unused")
    public static String getPrefix() {
        return PREFIX;
    }
}

