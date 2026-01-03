package com.obsidian.dragon;


import com.obsidian.dragon.command.DragonCommand;
import com.obsidian.dragon.listener.DragonDeathListener;
import com.obsidian.dragon.logic.DragonKillManager;
import com.obsidian.dragon.logic.LootManager;
import com.obsidian.dragon.util.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;

public final class ObsidianDragon extends JavaPlugin {

    private LootManager lootManager;
    private DragonKillManager dragonKillManager;
    private MessageUtil messageUtil;

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

        // Register event listeners
        getServer().getPluginManager().registerEvents(new DragonDeathListener(lootManager), this);
        getLogger().info("DragonDeathListener registered.");

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

            return lootSuccess;
        } catch (Exception e) {
            getLogger().severe("Error reloading plugin: " + e.getMessage());
            return false;
        }
    }

}
