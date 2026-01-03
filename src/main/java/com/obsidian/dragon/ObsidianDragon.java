package com.obsidian.dragon;


import com.obsidian.dragon.command.DragonCommand;
import com.obsidian.dragon.economy.EconomyManager;
import com.obsidian.dragon.gui.GUIManager;
import com.obsidian.dragon.listener.DragonDeathListener;
import com.obsidian.dragon.listener.MenuClickListener;
import com.obsidian.dragon.logic.DragonKillManager;
import com.obsidian.dragon.logic.LootManager;
import com.obsidian.dragon.util.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;

public final class ObsidianDragon extends JavaPlugin {

    private LootManager lootManager;
    private DragonKillManager dragonKillManager;
    private MessageUtil messageUtil;
    private GUIManager guiManager;
    private EconomyManager economyManager;

    @Override
    public void onEnable() {
        // Save default config if not exists
        saveDefaultConfig();

        // Initialize MessageUtil
        messageUtil = new MessageUtil(this);

        // Initialize LootManager
        lootManager = new LootManager(this);
        getLogger().info("LootManager initialized with " + lootManager.getLootItemCount() + " loot item(s).");

        // Initialize DragonKillManager
        dragonKillManager = new DragonKillManager(this);
        getLogger().info("DragonKillManager initialized.");

        // Initialize EconomyManager
        economyManager = new EconomyManager(this);
        getLogger().info("EconomyManager initialized with provider: " + economyManager.getProviderName());

        // Initialize GUIManager
        guiManager = new GUIManager(this);
        getLogger().info("GUIManager initialized.");

        // Register event listeners
        getServer().getPluginManager().registerEvents(new DragonDeathListener(lootManager), this);
        getLogger().info("DragonDeathListener registered.");
        getServer().getPluginManager().registerEvents(new MenuClickListener(guiManager), this);
        getLogger().info("MenuClickListener registered.");

        // Register commands
        DragonCommand dragonCommand = new DragonCommand(this);
        PluginCommand cmd = this.getCommand("obsidiandragon");
        if (cmd != null) {
            cmd.setExecutor(dragonCommand);
            cmd.setTabCompleter(dragonCommand);
        } else {
            getLogger().warning("Command 'obsidiandragon' not found in plugin.yml; executor/tab completer not registered.");
        }
        getLogger().info("ObsidianDragon plugin enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ObsidianDragon plugin disabled.");
    }

    /**
     * Gets the LootManager instance.
     * @return the LootManager
     */
    public LootManager getLootManager() {
        return lootManager;
    }

    /**
     * Gets the DragonKillManager instance.
     * @return the DragonKillManager
     */
    public DragonKillManager getDragonKillManager() {
        return dragonKillManager;
    }

    /**
     * Gets the MessageUtil instance.
     * @return the MessageUtil
     */
    public MessageUtil getMessageUtil() {
        return messageUtil;
    }

    /**
     * Gets the GUIManager instance.
     * @return the GUIManager
     */
    public GUIManager getGUIManager() {
        return guiManager;
    }

    /**
     * Gets the EconomyManager instance.
     * @return the EconomyManager
     */
    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    /**
     * Reloads the plugin configuration and loot.
     * @return true if reload was successful
     */
    public boolean reloadPlugin() {
        try {
            // Reload config.yml
            reloadConfig();

            // Reload loot.yml
            boolean lootSuccess = lootManager.reload();

            // Reinitialize DragonKillManager with new config
            dragonKillManager = new DragonKillManager(this);

            // Reload economy settings
            economyManager.reload();

            return lootSuccess;
        } catch (Exception e) {
            getLogger().severe("Error reloading plugin: " + e.getMessage());
            return false;
        }
    }

    /**
     * Spawns the Ender Dragon in The End with economy integration.
     *
     * @param sender The command sender requesting the spawn
     * @return true if the dragon was spawned successfully, false otherwise
     */
    public boolean spawnDragon(org.bukkit.command.CommandSender sender) {
        if (!sender.hasPermission("obsidiandragon.spawn") && !sender.hasPermission("obsidiandragon.admin.menu")) {
            messageUtil.sendConfig(sender, "messages.no-permission",
                    "&cYou don't have permission to do that!");
            return false;
        }

        // Process payment if sender is a player
        double paidAmount = 0;
        if (sender instanceof org.bukkit.entity.Player player) {
            EconomyManager.TransactionResult result = economyManager.processSpawnPayment(player);

            if (!result.isSuccess()) {
                messageUtil.send(sender, "&c" + result.getMessage());
                return false;
            }

            paidAmount = result.getAmount();

            // Send payment confirmation if money was charged
            if (paidAmount > 0) {
                messageUtil.sendConfig(sender, "economy.messages.payment-success",
                        "&aYou paid %cost% to spawn the Ender Dragon!",
                        "%cost%", economyManager.formatCurrency(paidAmount));
            } else if (player.hasPermission("obsidiandragon.spawn.free") || player.hasPermission("obsidiandragon.admin.menu")) {
                messageUtil.sendConfig(sender, "economy.messages.spawn-free",
                        "&7Dragon spawn is free for you!");
            }
        }

        // Attempt to spawn the dragon
        try {
            com.obsidian.dragon.logic.DragonRespawnManager manager = new com.obsidian.dragon.logic.DragonRespawnManager("world_the_end");
            boolean success = manager.spawnDragon();

            if (success) {
                messageUtil.sendConfig(sender, "messages.spawn-success",
                        "&aEnder Dragon respawn sequence started!");
                return true;
            } else {
                // Spawn failed - refund if payment was made
                if (paidAmount > 0 && sender instanceof org.bukkit.entity.Player player) {
                    economyManager.refundSpawnPayment(player, paidAmount);
                    messageUtil.sendConfig(sender, "economy.messages.refund-success",
                            "&aYou have been refunded %amount% (spawn failed).",
                            "%amount%", economyManager.formatCurrency(paidAmount));
                }

                messageUtil.sendConfig(sender, "messages.spawn-failed",
                        "&cFailed to start dragon respawn. Is the dragon already alive or is the portal missing?");
                return false;
            }
        } catch (Exception e) {
            // Exception occurred - refund if payment was made
            if (paidAmount > 0 && sender instanceof org.bukkit.entity.Player player) {
                economyManager.refundSpawnPayment(player, paidAmount);
                messageUtil.sendConfig(sender, "economy.messages.refund-success",
                        "&aYou have been refunded %amount% (error occurred).",
                        "%amount%", economyManager.formatCurrency(paidAmount));
            }

            messageUtil.sendConfig(sender, "messages.spawn-error",
                    "&cError: %error%", "%error%", e.getMessage());
            getLogger().warning("Failed to spawn dragon: " + e.getMessage());
            return false;
        }
    }

}
