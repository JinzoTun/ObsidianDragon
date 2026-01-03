package com.obsidian.dragon.logic;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.EnderCrystal;

import java.util.ArrayList;
import java.util.List;

public class DragonRespawnManager {

    private final World endWorld;
    private final DragonBattle battle;

    public DragonRespawnManager(String endWorldName) {
        this.endWorld = Bukkit.getWorld(endWorldName);
        if (endWorld == null) {
            throw new IllegalArgumentException("End world not loaded: " + endWorldName);
        }
        this.battle = endWorld.getEnderDragonBattle();
        if (battle == null) {
            throw new IllegalStateException("No DragonBattle found in world: " + endWorldName);
        }
    }

    public boolean spawnDragon() {
        // Null check for battle
        if (battle == null) {
            return false;
        }

        // Ensure portal exists
        battle.generateEndPortal(true);

        // Mark as previously killed if needed
        if (!battle.hasBeenPreviouslyKilled()) {
            battle.setPreviouslyKilled(true);
        }

        // Safety checks
        if (battle.getEnderDragon() != null) return false; // dragon alive
        if (battle.getRespawnPhase() != DragonBattle.RespawnPhase.NONE) return false; // respawn in progress

        Location center = battle.getEndPortalLocation();
        if (center == null) return false;

        // Null check for world
        if (endWorld == null) {
            return false;
        }

        // Spawn End Crystals at N/S/E/W pillars
        double y = center.getY() + 1;
        double offset = 3;

        List<EnderCrystal> crystals = new ArrayList<>();
        try {
            crystals.add(endWorld.spawn(center.clone().add(offset + 0.5, 0, 0 + 0.5), EnderCrystal.class));
            crystals.add(endWorld.spawn(center.clone().add(-offset + 0.5, 0, 0 + 0.5), EnderCrystal.class));
            crystals.add(endWorld.spawn(center.clone().add(0 + 0.5, 0, offset + 0.5), EnderCrystal.class));
            crystals.add(endWorld.spawn(center.clone().add(0 + 0.5, 0, -offset + 0.5), EnderCrystal.class));

            crystals.forEach(c -> {
                if (c != null) {
                    Location loc = c.getLocation().clone(); // clone original location
                    loc.setY(y);                             // set Y
                    c.teleport(loc);                         // teleport entity
                }
            });

            return battle.initiateRespawn(crystals);
        } catch (Exception e) {
            // Clean up crystals if spawn fails
            crystals.forEach(c -> {
                if (c != null && !c.isDead()) {
                    c.remove();
                }
            });
            return false;
        }
    }
}
