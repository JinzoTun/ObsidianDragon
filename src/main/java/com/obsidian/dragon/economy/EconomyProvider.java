package com.obsidian.dragon.economy;

import org.bukkit.entity.Player;

/**
 * Interface for economy provider implementations.
 * Supports multiple economy plugins through abstraction.
 */
public interface EconomyProvider {

    /**
     * Gets the name of this economy provider.
     *
     * @return The provider name (e.g., "Vault", "CoinsEngine")
     */
    String getName();

    /**
     * Checks if the player has at least the specified amount of money.
     *
     * @param player The player to check
     * @param amount The amount to check for
     * @return true if the player has enough money, false otherwise
     */
    boolean hasAmount(Player player, double amount);

    /**
     * Gets the player's current balance.
     *
     * @param player The player to check
     * @return The player's balance
     */
    double getBalance(Player player);

    /**
     * Withdraws the specified amount from the player's account.
     *
     * @param player The player to withdraw from
     * @param amount The amount to withdraw
     * @return true if the withdrawal was successful, false otherwise
     */
    boolean withdraw(Player player, double amount);

    /**
     * Deposits the specified amount into the player's account.
     * Used for refunds or rollbacks.
     *
     * @param player The player to deposit to
     * @param amount The amount to deposit
     * @return true if the deposit was successful, false otherwise
     */
    boolean deposit(Player player, double amount);

    /**
     * Formats a currency amount for display.
     *
     * @param amount The amount to format
     * @return A formatted string representation of the amount
     */
    String format(double amount);

    /**
     * Checks if this economy provider is currently available.
     *
     * @return true if the provider is ready to use, false otherwise
     */
    boolean isAvailable();
}

