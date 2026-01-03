package com.obsidian.dragon.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Vault economy provider implementation.
 * Integrates with Vault API to handle economy transactions.
 */
public class VaultEconomyProvider implements EconomyProvider {

    private final JavaPlugin plugin;
    private Economy economy;

    public VaultEconomyProvider(JavaPlugin plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    /**
     * Sets up the Vault economy service.
     */
    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            economy = null;
            return;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager()
                .getRegistration(Economy.class);
        if (rsp == null) {
            economy = null;
            return;
        }

        economy = rsp.getProvider();
    }

    @Override
    public String getName() {
        return "Vault";
    }

    @Override
    public boolean hasAmount(Player player, double amount) {
        if (!isAvailable()) {
            return false;
        }
        return economy.has(player, amount);
    }

    @Override
    public double getBalance(Player player) {
        if (!isAvailable()) {
            return 0.0;
        }
        return economy.getBalance(player);
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        if (!isAvailable()) {
            return false;
        }

        if (amount < 0) {
            plugin.getLogger().warning("Attempted to withdraw negative amount: " + amount);
            return false;
        }

        if (!hasAmount(player, amount)) {
            return false;
        }

        try {
            return economy.withdrawPlayer(player, amount).transactionSuccess();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to withdraw money from " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deposit(Player player, double amount) {
        if (!isAvailable()) {
            return false;
        }

        if (amount < 0) {
            plugin.getLogger().warning("Attempted to deposit negative amount: " + amount);
            return false;
        }

        try {
            return economy.depositPlayer(player, amount).transactionSuccess();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to deposit money to " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public String format(double amount) {
        if (!isAvailable()) {
            return String.format("$%.2f", amount);
        }
        return economy.format(amount);
    }

    @Override
    public boolean isAvailable() {
        return economy != null;
    }
}

