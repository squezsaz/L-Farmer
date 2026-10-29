package com.squezsaz.lfarmer.service;

import com.squezsaz.lfarmer.LFarmer;
import com.squezsaz.lfarmer.model.Farmer;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;

public class GroundCollectService implements Listener {

    private final LFarmer plugin;
    private Object taskHandle;

    public GroundCollectService(LFarmer plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        long intervalTicks = plugin.getConfig().getLong("ground_collect.check_interval_ticks", 20L);
        this.taskHandle = plugin.getScheduler().runGlobalRepeating(this::tickGroundCollect, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (taskHandle != null) {
            plugin.getScheduler().cancelTask(taskHandle);
            taskHandle = null;
        }
    }

    private void tickGroundCollect() {
        for (Farmer farmer : plugin.getFarmerManager().getAllFarmers().values()) {
            if (!farmer.isGroundCollect()) {
                continue;
            }

            Location loc = farmer.getLocation();
            if (loc == null || loc.getWorld() == null) {
                continue;
            }

            plugin.getScheduler().runAtLocation(loc, () -> collectGroundItems(farmer));
        }
    }

    public void collectGroundItems(Farmer farmer) {
        Location loc = farmer.getLocation();
        if (loc == null || loc.getWorld() == null) return;

        World world = loc.getWorld();
        int baseChunkX = farmer.getChunkX();
        int baseChunkZ = farmer.getChunkZ();

        int radius = (int) plugin.getFarmerManager().getLevelValue("range", farmer.getRangeLevel());
        long maxCapacity = (long) plugin.getFarmerManager().getLevelValue("capacity", farmer.getCapacityLevel());

        if (farmer.getTotalStored() >= maxCapacity) {
            return;
        }

        boolean fuelEnabled = plugin.getConfig().getBoolean("fuel.enabled", false);
        double fuelCost = plugin.getConfig().getDouble("fuel.cost_per_ground_pickup", 0.02);

        long totalCollected = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = baseChunkX + dx;
                int cz = baseChunkZ + dz;
                if (!world.isChunkLoaded(cx, cz)) {
                    continue;
                }

                Chunk chunk = world.getChunkAt(cx, cz);
                for (Entity entity : chunk.getEntities()) {
                    if (!(entity instanceof Item itemEntity)) {
                        continue;
                    }

                    if (!itemEntity.isValid() || itemEntity.isDead()) {
                        continue;
                    }

                    ItemStack stack = itemEntity.getItemStack();
                    if (stack == null || stack.getType().isAir()) {
                        continue;
                    }

                    Material mat = stack.getType();
                    if (!farmer.isItemAllowed(mat)) {
                        continue;
                    }

                    long remaining = maxCapacity - farmer.getTotalStored();
                    if (remaining <= 0) {
                        break;
                    }

                    if (fuelEnabled && fuelCost > 0) {
                        if (!farmer.consumeFuel(fuelCost)) {
                            return;
                        }
                        plugin.getStatsService().recordFuel(farmer.getOwnerUuid(), fuelCost);
                    }

                    int amount = stack.getAmount();
                    int toAdd = (int) Math.min((long) amount, remaining);
                    if (toAdd <= 0) {
                        break;
                    }

                    farmer.addStorage(mat, toAdd, maxCapacity);
                    totalCollected += toAdd;

                    if (toAdd >= amount) {
                        itemEntity.remove();
                    } else {
                        stack.setAmount(amount - toAdd);
                        itemEntity.setItemStack(stack);
                    }
                }
            }
        }

        if (totalCollected > 0) {
            plugin.getStatsService().recordHarvest(farmer.getOwnerUuid(), totalCollected);
            plugin.getNpcManager().updateHologram(farmer.getId());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item itemEntity = event.getEntity();
        Location loc = itemEntity.getLocation();
        if (loc.getWorld() == null) return;

        Chunk chunk = loc.getChunk();
        Farmer farmer = plugin.getFarmerManager().getFarmerCoveringChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        if (farmer == null || !farmer.isGroundCollect()) {
            return;
        }

        ItemStack stack = itemEntity.getItemStack();
        if (stack == null || stack.getType().isAir() || !farmer.isItemAllowed(stack.getType())) {
            return;
        }

        plugin.getScheduler().runAtLocation(loc, () -> {
            if (!itemEntity.isValid() || itemEntity.isDead() || !farmer.isGroundCollect()) return;
            long maxCapacity = (long) plugin.getFarmerManager().getLevelValue("capacity", farmer.getCapacityLevel());
            long remaining = maxCapacity - farmer.getTotalStored();
            if (remaining <= 0) return;

            ItemStack currentStack = itemEntity.getItemStack();
            if (currentStack == null || currentStack.getType().isAir() || !farmer.isItemAllowed(currentStack.getType())) return;

            boolean fuelEnabled = plugin.getConfig().getBoolean("fuel.enabled", false);
            double fuelCost = plugin.getConfig().getDouble("fuel.cost_per_ground_pickup", 0.02);
            if (fuelEnabled && fuelCost > 0) {
                if (!farmer.consumeFuel(fuelCost)) return;
                plugin.getStatsService().recordFuel(farmer.getOwnerUuid(), fuelCost);
            }

            int amount = currentStack.getAmount();
            int toAdd = (int) Math.min((long) amount, remaining);
            if (toAdd <= 0) return;

            farmer.addStorage(currentStack.getType(), toAdd, maxCapacity);
            plugin.getStatsService().recordHarvest(farmer.getOwnerUuid(), toAdd);
            plugin.getNpcManager().updateHologram(farmer.getId());

            if (toAdd >= amount) {
                itemEntity.remove();
            } else {
                currentStack.setAmount(amount - toAdd);
                itemEntity.setItemStack(currentStack);
            }
        });
    }
}
