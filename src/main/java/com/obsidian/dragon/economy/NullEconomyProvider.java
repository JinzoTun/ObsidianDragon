package com.obsidian.dragon.economy;

import org.bukkit.entity.Player;

/**
 * Null/Disabled economy provider.
 * Used when no economy plugin is available or economy is disabled.
 */
public class NullEconomyProvider implements EconomyProvider {

    @Override
    public String getName() {
        return "None";
    }

    @Override
    public boolean hasAmount(Player player, double amount) {
        return true; // Always return true if no economy
    }

    @Override
    public double getBalance(Player player) {
        return 0.0;
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        return true; // No-op
    }

    @Override
    public boolean deposit(Player player, double amount) {
        return true; // No-op
    }

    @Override
    public String format(double amount) {
        return String.format("$%.2f", amount);
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}

