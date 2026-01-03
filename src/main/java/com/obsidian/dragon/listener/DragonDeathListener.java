package com.obsidian.dragon.listener;

import com.obsidian.dragon.logic.LootManager;
import org.bukkit.Location;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Listens for Ender Dragon death events to trigger custom loot drops.
 */
public class DragonDeathListener implements Listener {

    private final LootManager lootManager;

    public DragonDeathListener(LootManager lootManager) {
        this.lootManager = lootManager;
    }

    /**
     * Handles the Ender Dragon death event.
     * Spawns custom loot at the dragon's death location.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDragonDeath(EntityDeathEvent event) {
        // Check if the entity is an Ender Dragon
        if (!(event.getEntity() instanceof EnderDragon dragon)) {
            return;
        }

        // Get death location
        Location deathLocation = dragon.getLocation();

        // Clear default drops if you want only custom loot
        // Uncomment the line below to remove vanilla drops
        // event.getDrops().clear();

        // Spawn custom loot
        lootManager.spawnLoot(deathLocation);
    }
}

