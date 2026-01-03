package com.obsidian.dragon.economy;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;
import su.nightexpress.coinsengine.api.currency.Currency;

/**
 * CoinsEngine economy provider implementation.
 * Integrates with CoinsEngine API to handle economy transactions.
 */
public class CoinsEngineProvider implements EconomyProvider {

    private final JavaPlugin plugin;
    private Currency defaultCurrency;

    public CoinsEngineProvider(JavaPlugin plugin) {
        this.plugin = plugin;
        setupCoinsEngine();
    }

    /**
     * Sets up the CoinsEngine API.
     */
    @SuppressWarnings("deprecation")
    private void setupCoinsEngine() {
        if (plugin.getServer().getPluginManager().getPlugin("CoinsEngine") == null) {
            defaultCurrency = null;
            return;
        }

        try {
            // Get all registered currencies and pick the first one as default
            var currencies = CoinsEngineAPI.getCurrencyManager().getCurrencies();
            if (!currencies.isEmpty()) {
                defaultCurrency = currencies.iterator().next();
            } else {
                plugin.getLogger().warning("CoinsEngine found but no currencies are configured!");
                defaultCurrency = null;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to setup CoinsEngine: " + e.getMessage());
            defaultCurrency = null;
        }
    }

    @Override
    public String getName() {
        return "CoinsEngine";
    }

    @Override
    public boolean hasAmount(Player player, double amount) {
        if (!isAvailable()) {
            return false;
        }
        return CoinsEngineAPI.getBalance(player, defaultCurrency) >= amount;
    }

    @Override
    public double getBalance(Player player) {
        if (!isAvailable()) {
            return 0.0;
        }
        return CoinsEngineAPI.getBalance(player, defaultCurrency);
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
            CoinsEngineAPI.removeBalance(player, defaultCurrency, amount);
            return true;
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
            CoinsEngineAPI.addBalance(player, defaultCurrency, amount);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to deposit money to " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public String format(double amount) {
        if (!isAvailable()) {
            return String.format("%.2f coins", amount);
        }
        return defaultCurrency.format(amount);
    }

    @Override
    public boolean isAvailable() {
        return defaultCurrency != null;
    }
}

