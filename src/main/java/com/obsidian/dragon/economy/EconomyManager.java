package com.obsidian.dragon.economy;

import com.obsidian.dragon.ObsidianDragon;
import org.bukkit.entity.Player;

/**
 * Manages economy integration for the ObsidianDragon plugin.
 * Handles transaction processing, provider selection, and rollback scenarios.
 */
public class EconomyManager {

    private final ObsidianDragon plugin;
    private EconomyProvider provider;
    private double spawnCost;
    private boolean economyEnabled;

    public EconomyManager(ObsidianDragon plugin) {
        this.plugin = plugin;
        loadConfiguration();
        setupEconomyProvider();
    }

    /**
     * Loads economy configuration from config.yml.
     */
    private void loadConfiguration() {
        economyEnabled = plugin.getConfig().getBoolean("economy.enabled", true);
        spawnCost = plugin.getConfig().getDouble("economy.spawn-cost", 1000.0);

        // Validate cost
        if (spawnCost < 0) {
            plugin.getLogger().warning("Spawn cost cannot be negative! Setting to 0.");
            spawnCost = 0;
        }
    }

    /**
     * Sets up the economy provider based on configuration.
     */
    private void setupEconomyProvider() {
        if (!economyEnabled) {
            provider = new NullEconomyProvider();
            plugin.getLogger().info("Economy integration is disabled in config.");
            return;
        }

        String providerType = plugin.getConfig().getString("economy.provider", "auto").toLowerCase();

        switch (providerType) {
            case "vault" -> setupVault();
            case "coinsengine" -> setupCoinsEngine();
            case "auto" -> setupAuto();
            default -> {
                plugin.getLogger().warning("Unknown economy provider: " + providerType + ". Using auto-detection.");
                setupAuto();
            }
        }
    }

    /**
     * Sets up Vault economy provider.
     */
    private void setupVault() {
        VaultEconomyProvider vaultProvider = new VaultEconomyProvider(plugin);
        if (vaultProvider.isAvailable()) {
            provider = vaultProvider;
            plugin.getLogger().info("Economy provider: Vault");
        } else {
            plugin.getLogger().warning("Vault economy provider configured but not available! Using null provider.");
            provider = new NullEconomyProvider();
        }
    }

    /**
     * Sets up CoinsEngine economy provider.
     */
    private void setupCoinsEngine() {
        // Check if CoinsEngine plugin is available and enabled before instantiating
        org.bukkit.plugin.Plugin coinsPlugin = plugin.getServer().getPluginManager().getPlugin("CoinsEngine");
        if (coinsPlugin == null || !coinsPlugin.isEnabled()) {
            if (coinsPlugin != null) {
                plugin.getLogger().warning("CoinsEngine plugin is installed but failed to enable! Check if Vault is installed.");
                plugin.getLogger().warning("CoinsEngine requires Vault to function. Using null economy provider.");
            } else {
                plugin.getLogger().warning("CoinsEngine economy provider configured but plugin not found! Using null provider.");
            }
            provider = new NullEconomyProvider();
            return;
        }

        try {
            CoinsEngineProvider coinsProvider = new CoinsEngineProvider(plugin);
            if (coinsProvider.isAvailable()) {
                provider = coinsProvider;
                plugin.getLogger().info("Economy provider: CoinsEngine");
            } else {
                plugin.getLogger().warning("CoinsEngine is enabled but has no currencies configured! Using null provider.");
                provider = new NullEconomyProvider();
            }
        } catch (NoClassDefFoundError | Exception e) {
            plugin.getLogger().warning("Failed to initialize CoinsEngine provider: " + e.getMessage());
            plugin.getLogger().warning("This usually means CoinsEngine failed to load. Check if Vault is installed.");
            provider = new NullEconomyProvider();
        }
    }

    /**
     * Auto-detects and sets up the best available economy provider.
     */
    private void setupAuto() {
        // Try Vault first (most common)
        VaultEconomyProvider vaultProvider = new VaultEconomyProvider(plugin);
        if (vaultProvider.isAvailable()) {
            provider = vaultProvider;
            plugin.getLogger().info("Economy provider: Vault (auto-detected)");
            return;
        }

        // Try CoinsEngine - check if plugin is available and enabled
        org.bukkit.plugin.Plugin coinsPlugin = plugin.getServer().getPluginManager().getPlugin("CoinsEngine");
        if (coinsPlugin != null && coinsPlugin.isEnabled()) {
            try {
                CoinsEngineProvider coinsProvider = new CoinsEngineProvider(plugin);
                if (coinsProvider.isAvailable()) {
                    provider = coinsProvider;
                    plugin.getLogger().info("Economy provider: CoinsEngine (auto-detected)");
                    return;
                }
            } catch (NoClassDefFoundError | Exception e) {
                plugin.getLogger().warning("CoinsEngine found but failed to initialize: " + e.getMessage());
            }
        }

        // No economy plugin found
        provider = new NullEconomyProvider();
        plugin.getLogger().warning("No economy plugin detected! Dragon spawning will be free.");
        plugin.getLogger().info("To enable economy, install Vault + an economy plugin, or configure CoinsEngine with Vault.");
    }

    /**
     * Processes a dragon spawn payment from a player.
     *
     * @param player The player attempting to spawn the dragon
     * @return TransactionResult indicating the outcome
     */
    public TransactionResult processSpawnPayment(Player player) {
        // Check if player has free spawn permission
        if (player.hasPermission("obsidiandragon.spawn.free") || player.hasPermission("obsidiandragon.admin.menu")) {
            return TransactionResult.success(0, "Free spawn (admin/bypass)");
        }

        // Check if economy is enabled and cost is greater than 0
        if (!economyEnabled || spawnCost <= 0 || !provider.isAvailable()) {
            return TransactionResult.success(0, "Economy disabled or free");
        }

        // Check if player has enough money
        if (!provider.hasAmount(player, spawnCost)) {
            double balance = provider.getBalance(player);
            double needed = spawnCost - balance;
            String message = String.format("Insufficient funds! Need %s more (Balance: %s, Cost: %s)",
                    provider.format(needed),
                    provider.format(balance),
                    provider.format(spawnCost));
            return TransactionResult.failure(message);
        }

        // Attempt to withdraw the money
        boolean success = provider.withdraw(player, spawnCost);
        if (!success) {
            return TransactionResult.failure("Transaction failed! Please try again or contact an administrator.");
        }

        // Transaction successful
        String message = String.format("Paid %s to spawn the Ender Dragon", provider.format(spawnCost));
        return TransactionResult.success(spawnCost, message);
    }

    /**
     * Refunds a spawn payment to a player.
     * Used for rollback scenarios when spawn fails after payment.
     *
     * @param player The player to refund
     * @param amount The amount to refund
     * @return true if refund was successful, false otherwise
     */
    @SuppressWarnings({"UnusedReturnValue", "unused"})
    public boolean refundSpawnPayment(Player player, double amount) {
        if (amount <= 0 || !provider.isAvailable()) {
            return true; // Nothing to refund
        }

        boolean success = provider.deposit(player, amount);
        if (success) {
            plugin.getLogger().info("Refunded " + provider.format(amount) + " to " + player.getName() + " (spawn failed)");
        } else {
            plugin.getLogger().severe("FAILED TO REFUND " + provider.format(amount) + " to " + player.getName() + "! Manual intervention required!");
        }
        return success;
    }

    /**
     * Gets the current spawn cost.
     *
     * @return The spawn cost
     */
    public double getSpawnCost() {
        return spawnCost;
    }

    /**
     * Formats a currency amount for display.
     *
     * @param amount The amount to format
     * @return Formatted currency string
     */
    public String formatCurrency(double amount) {
        return provider.format(amount);
    }

    /**
     * Checks if economy integration is enabled and available.
     *
     * @return true if economy is enabled and available
     */
    public boolean isEconomyEnabled() {
        return economyEnabled && provider.isAvailable() && spawnCost > 0;
    }

    /**
     * Gets the current economy provider name.
     *
     * @return The provider name
     */
    public String getProviderName() {
        return provider.getName();
    }

    /**
     * Checks if a player can afford to spawn the dragon.
     *
     * @param player The player to check
     * @return true if the player can afford it or has free permission
     */
    @SuppressWarnings("unused")
    public boolean canAffordSpawn(Player player) {
        if (player.hasPermission("obsidiandragon.spawn.free") || player.hasPermission("obsidiandragon.admin.menu")) {
            return true;
        }
        if (!economyEnabled || spawnCost <= 0 || !provider.isAvailable()) {
            return true;
        }
        return provider.hasAmount(player, spawnCost);
    }

    /**
     * Gets the player's current balance.
     *
     * @param player The player to check
     * @return The player's balance
     */
    public double getBalance(Player player) {
        return provider.getBalance(player);
    }

    /**
     * Reloads the economy configuration.
     */
    public void reload() {
        loadConfiguration();
        setupEconomyProvider();
    }

    /**
     * Represents the result of an economy transaction.
     */
    public static class TransactionResult {
        private final boolean success;
        private final double amount;
        private final String message;

        private TransactionResult(boolean success, double amount, String message) {
            this.success = success;
            this.amount = amount;
            this.message = message;
        }

        public static TransactionResult success(double amount, String message) {
            return new TransactionResult(true, amount, message);
        }

        public static TransactionResult failure(String message) {
            return new TransactionResult(false, 0, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public double getAmount() {
            return amount;
        }

        public String getMessage() {
            return message;
        }
    }
}

