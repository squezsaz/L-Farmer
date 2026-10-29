package com.squezsaz.lfarmer.service;

import com.squezsaz.lfarmer.LFarmer;
import com.squezsaz.lfarmer.model.Farmer;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.List;

public class MobCollectService implements Listener {

    private final LFarmer plugin;

    public MobCollectService(LFarmer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) {
            return;
        }

        Location loc = entity.getLocation();
        Chunk chunk = loc.getChunk();
        Farmer farmer = plugin.getFarmerManager().getFarmer(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        if (farmer == null || !farmer.isMobCollect()) {
            return;
        }

        boolean fuelEnabled = plugin.getConfig().getBoolean("fuel.enabled", false);
        double fuelCost = plugin.getConfig().getDouble("fuel.cost_per_mob", 0.05);
        if (fuelEnabled) {
            if (!farmer.consumeFuel(fuelCost)) {
                return;
            }
            plugin.getStatsService().recordFuel(farmer.getOwnerUuid(), fuelCost);
        }

        long maxCapacity = (long) plugin.getFarmerManager().getLevelValue("capacity", farmer.getCapacityLevel());
        List<ItemStack> drops = event.getDrops();
        Iterator<ItemStack> iterator = drops.iterator();

        long totalCollected = 0;
        while (iterator.hasNext()) {
            ItemStack drop = iterator.next();
            if (drop == null || drop.getType().isAir() || !farmer.isItemAllowed(drop.getType())) {
                continue;
            }

            int amount = drop.getAmount();
            boolean fullyAdded = farmer.addStorage(drop.getType(), amount, maxCapacity);
            if (fullyAdded) {
                iterator.remove();
                totalCollected += amount;
            }
        }

        if (totalCollected > 0) {
            plugin.getStatsService().recordMobCollect(farmer.getOwnerUuid(), totalCollected);
            plugin.getNpcManager().updateHologram(farmer.getId());
        }
    }
}
