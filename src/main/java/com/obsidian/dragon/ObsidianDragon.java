package com.obsidian.dragon;


import com.obsidian.dragon.command.DragonCommand;
import com.obsidian.dragon.listener.DragonDeathListener;
import com.obsidian.dragon.logic.LootManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;

public final class ObsidianDragon extends JavaPlugin {

    private LootManager lootManager;

    @Override
    public void onEnable() {
        // Initialize LootManager
        lootManager = new LootManager(this);
        getLogger().info("LootManager initialized with " + lootManager.getLootItemCount() + " loot item(s).");

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

}
