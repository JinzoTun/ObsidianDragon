package com.obsidian.dragon.command;

import com.obsidian.dragon.ObsidianDragon;
import com.obsidian.dragon.util.MessageUtil;
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
    private final MessageUtil msg;

    public DragonCommand(ObsidianDragon plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageUtil();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        if (args.length == 0) {
            msg.send(sender, "&e&lObsidianDragon Commands:");
            msg.send(sender, "&7/dragon menu &f- Open the dragon menu GUI");
            msg.send(sender, "&7/dragon spawn &f- Spawn the Ender Dragon");
            msg.send(sender, "&7/dragon kill &f- Instantly kill the Ender Dragon");
            msg.send(sender, "&7/dragon reload &f- Reload plugin configuration");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "menu" -> handleMenu(sender);
            case "spawn" -> handleSpawn(sender);
            case "kill" -> handleKill(sender);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage("§cUnknown command. Use §e/dragon §cfor help.");
        }
        return true;
    }

    /**
     * Handles the menu subcommand.
     */
    private void handleMenu(CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player player)) {
            msg.send(sender, "&cOnly players can use this command!");
            return;
        }

        if (!player.hasPermission("obsidiandragon.menu.use") && !player.hasPermission("obsidiandragon.admin.menu")) {
            msg.sendConfig(sender, "messages.no-permission",
                    "&cYou don't have permission to use this command.");
            return;
        }

        plugin.getGUIManager().openMainMenu(player);
    }

    /**
     * Handles the spawn subcommand.
     */
    private void handleSpawn(CommandSender sender) {
        plugin.spawnDragon(sender);
    }

    /**
     * Handles the kill subcommand.
     */
    private void handleKill(CommandSender sender) {
        plugin.getDragonKillManager().killDragon(sender);
    }

    /**
     * Handles the reload subcommand for plugin configuration.
     */
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("obsidiandragon.admin.loot")) {
            msg.sendConfig(sender, "messages.no-permission",
                    "&cYou don't have permission to use this command.");
            return;
        }

        msg.send(sender, "&eReloading plugin configuration...");
        boolean success = plugin.reloadPlugin();

        if (success) {
            int lootCount = plugin.getLootManager().getLootItemCount();
            msg.sendConfig(sender, "messages.reload-success",
                    "&aConfiguration reloaded successfully!");

            // Send details with both placeholders replaced
            String detailsMsg = plugin.getConfig().getString("messages.reload-success-details",
                    "&7Config: %config% | Loot items: %loot%");
            detailsMsg = detailsMsg.replace("%config%", "✓").replace("%loot%", String.valueOf(lootCount));
            msg.send(sender, detailsMsg);
        } else {
            msg.sendConfig(sender, "messages.reload-failed",
                    "&cFailed to reload configuration! Check console for errors.");
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String partial = args[0].toLowerCase();

            // Add "menu" if player has permission
            if ((sender.hasPermission("obsidiandragon.menu.use") || sender.hasPermission("obsidiandragon.admin.menu"))
                    && "menu".startsWith(partial)) {
                completions.add("menu");
            }

            // Add "spawn" if player has permission
            if (sender.hasPermission("obsidiandragon.spawn") && "spawn".startsWith(partial)) {
                completions.add("spawn");
            }

            // Add "kill" if player has permission
            if (sender.hasPermission("obsidiandragon.admin.kill") && "kill".startsWith(partial)) {
                completions.add("kill");
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
