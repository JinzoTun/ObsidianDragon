package com.obsidian.dragon.command;

import com.obsidian.dragon.ObsidianDragon;
import com.obsidian.dragon.logic.DragonRespawnManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;


import java.util.ArrayList;

import java.util.Collections;
import java.util.List;

public class DragonCommand implements CommandExecutor, TabCompleter {

    private final ObsidianDragon plugin;

    public DragonCommand(ObsidianDragon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage("§e§lObsidianDragon Commands:");
            sender.sendMessage("§7/dragon spawn §f- Spawn the Ender Dragon");
            sender.sendMessage("§7/dragon reload §f- Reload loot configuration");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "spawn" -> handleSpawn(sender);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage("§cUnknown command. Use §e/dragon §cfor help.");
        }
        return true;
    }

    /**
     * Handles the spawn subcommand.
     */
    private void handleSpawn(CommandSender sender) {
        if (!sender.hasPermission("obsidiandragon.spawn")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return;
        }

        try {
            DragonRespawnManager manager = new DragonRespawnManager("world_the_end");
            boolean success = manager.spawnDragon();
            if (success) {
                sender.sendMessage("§aEnder Dragon respawn sequence started!");
            } else {
                sender.sendMessage("§cFailed to start dragon respawn. Is the dragon already alive or is the portal missing?");
            }
        } catch (Exception e) {
            sender.sendMessage("§cError: " + e.getMessage());
            plugin.getLogger().warning("Failed to spawn dragon: " + e.getMessage());
        }
    }

    /**
     * Handles the reload subcommand for loot configuration.
     */
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("obsidiandragon.admin.loot")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return;
        }

        sender.sendMessage("§eReloading loot configuration...");
        boolean success = plugin.getLootManager().reload();

        if (success) {
            int count = plugin.getLootManager().getLootItemCount();
            sender.sendMessage("§aLoot configuration reloaded successfully!");
            sender.sendMessage("§7Loaded " + count + " loot item(s).");
        } else {
            sender.sendMessage("§cFailed to reload loot configuration! Check console for errors.");
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String partial = args[0].toLowerCase();

            // Add "spawn" if player has permission
            if (sender.hasPermission("obsidiandragon.spawn") && "spawn".startsWith(partial)) {
                completions.add("spawn");
            }

            // Add "reload" if player has admin permission
            if (sender.hasPermission("obsidiandragon.admin.loot") && "reload".startsWith(partial)) {
                completions.add("reload");
            }

            return completions;
        }
        return Collections.emptyList();
    }
}
