package com.obsidian.dragon.logic;

import com.obsidian.dragon.ObsidianDragon;
import com.obsidian.dragon.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.DragonBattle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the instant killing of the Ender Dragon with cooldowns and proper checks.
 */
public class DragonKillManager {

    private final ObsidianDragon plugin;
    private final MessageUtil msg;
    private final Map<UUID, Long> cooldowns;
    private final int cooldownSeconds;

    public DragonKillManager(ObsidianDragon plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageUtil();
        this.cooldowns = new HashMap<>();
        this.cooldownSeconds = plugin.getConfig().getInt("dragon-kill.cooldown", 300);
    }

    /**
     * Attempts to kill the Ender Dragon instantly.
     *
     * @param sender The command sender
     * @return true if the dragon was killed successfully, false otherwise
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean killDragon(CommandSender sender) {
        // Permission check
        if (!sender.hasPermission("obsidiandragon.admin.kill")) {
            msg.sendConfig(sender, "dragon-kill.messages.no-permission",
                    "&cYou don't have permission to use this command.");
            return false;
        }

        // Cooldown check (only for players)
        if (sender instanceof Player player) {
            UUID playerId = player.getUniqueId();
            if (cooldowns.containsKey(playerId)) {
                long timeLeft = (cooldowns.get(playerId) - System.currentTimeMillis()) / 1000;
                if (timeLeft > 0) {
                    msg.sendConfig(sender, "dragon-kill.messages.cooldown",
                            "&cThis command is on cooldown! Wait %time% seconds.",
                            "%time%", String.valueOf(timeLeft));
                    return false;
                }
            }
        }

        // Get The End world
        World endWorld = Bukkit.getWorld("world_the_end");
        if (endWorld == null) {
            msg.send(sender, "&cThe End world is not loaded!");
            plugin.getLogger().warning("The End world (world_the_end) is not loaded!");
            return false;
        }

        // Get DragonBattle
        DragonBattle battle = endWorld.getEnderDragonBattle();
        if (battle == null) {
            msg.send(sender, "&cNo DragonBattle found in The End!");
            plugin.getLogger().warning("No DragonBattle found in world: world_the_end");
            return false;
        }

        // Get the Ender Dragon
        EnderDragon dragon = battle.getEnderDragon();
        if (dragon == null || dragon.isDead()) {
            msg.sendConfig(sender, "dragon-kill.messages.no-dragon",
                    "&cNo Ender Dragon is currently alive in The End!");
            return false;
        }

        // Kill the dragon
        try {
            dragon.setHealth(0.0);

            // Send confirmation to sender
            msg.sendConfig(sender, "dragon-kill.messages.success",
                    "&a&lDragon killed! &7The Ender Dragon has been slain.");

            // Broadcast to all players
            msg.broadcastConfig("dragon-kill.messages.broadcast",
                    "&7%player% has instantly killed the Ender Dragon!",
                    "%player%", sender.getName());

            // Set cooldown (only for players)
            if (sender instanceof Player player) {
                if (cooldownSeconds > 0) {
                    cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldownSeconds * 1000L));
                }
            }

            plugin.getLogger().info(sender.getName() + " has killed the Ender Dragon using /dragon kill");
            return true;

        } catch (Exception e) {
            msg.send(sender, "&cAn error occurred while killing the dragon!");
            plugin.getLogger().severe("Error killing dragon: " + e.getMessage());
            if (plugin.getLogger().isLoggable(java.util.logging.Level.FINE)) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Dragon kill error details:", e);
            }
            return false;
        }
    }

    /**
     * Clears the cooldown for a specific player.
     *
     * @param playerId The UUID of the player
     */
    @SuppressWarnings("unused")
    public void clearCooldown(UUID playerId) {
        if (playerId != null) {
            cooldowns.remove(playerId);
        }
    }

    /**
     * Clears all cooldowns.
     */
    @SuppressWarnings("unused")
    public void clearAllCooldowns() {
        cooldowns.clear();
    }

    /**
     * Gets the remaining cooldown time for a player in seconds.
     *
     * @param playerId The UUID of the player
     * @return The remaining cooldown time in seconds, or 0 if no cooldown
     */
    @SuppressWarnings("unused")
    public long getCooldownRemaining(UUID playerId) {
        if (playerId == null || !cooldowns.containsKey(playerId)) {
            return 0;
        }
        long timeLeft = (cooldowns.get(playerId) - System.currentTimeMillis()) / 1000;
        return Math.max(0, timeLeft);
    }
}

