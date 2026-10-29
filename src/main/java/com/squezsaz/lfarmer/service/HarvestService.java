package com.squezsaz.lfarmer.service;

import com.squezsaz.lfarmer.LFarmer;
import com.squezsaz.lfarmer.model.CropType;
import com.squezsaz.lfarmer.model.Farmer;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;

import java.util.Random;

public class HarvestService {

    private final LFarmer plugin;
    private final Random random;
    private Object taskHandle;

    public HarvestService(LFarmer plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    public void start() {
        stop();
        long intervalTicks = plugin.getConfig().getLong("harvest.check_interval_ticks", 60L);
        this.taskHandle = plugin.getScheduler().runGlobalRepeating(this::tickHarvest, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (taskHandle != null) {
            plugin.getScheduler().cancelTask(taskHandle);
            taskHandle = null;
        }
    }

    private void tickHarvest() {
        for (Farmer farmer : plugin.getFarmerManager().getAllFarmers().values()) {
            if (!farmer.isCropCollect()) {
                continue;
            }

            Location loc = farmer.getLocation();
            if (loc == null || loc.getWorld() == null) {
                continue;
            }

            plugin.getScheduler().runAtLocation(loc, () -> harvestFarmer(farmer));
        }
    }

    public void harvestFarmer(Farmer farmer) {
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
        double fuelCost = plugin.getConfig().getDouble("fuel.cost_per_harvest", 0.1);
        if (fuelEnabled) {
            if (!farmer.consumeFuel(fuelCost)) {
                return;
            }
            plugin.getStatsService().recordFuel(farmer.getOwnerUuid(), fuelCost);
        }

        int fortuneLevel = farmer.getFortuneLevel();
        double fortuneChance = plugin.getFarmerManager().getLevelValue("fortune", fortuneLevel) / 100.0;

        int minY = Math.max(world.getMinHeight(), loc.getBlockY() - 15);
        int maxY = Math.min(world.getMaxHeight(), loc.getBlockY() + 15);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = baseChunkX + dx;
                int cz = baseChunkZ + dz;
                if (!world.isChunkLoaded(cx, cz)) {
                    continue;
                }

                Chunk chunk = world.getChunkAt(cx, cz);
                scanChunkCrops(farmer, chunk, minY, maxY, maxCapacity, fortuneChance);
            }
        }

        plugin.getNpcManager().updateHologram(farmer.getId());
    }

    private void scanChunkCrops(Farmer farmer, Chunk chunk, int minY, int maxY, long maxCapacity, double fortuneChance) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (farmer.getTotalStored() >= maxCapacity) {
                        return;
                    }

                    Block block = chunk.getBlock(x, y, z);
                    Material type = block.getType();

                    if (!CropType.isHarvestable(type)) {
                        continue;
                    }

                    CropType crop = CropType.fromMaterial(type);
                    if (crop == null || !farmer.isItemAllowed(crop.getDropMaterial())) continue;

                    BlockData data = block.getBlockData();

                    if (type == Material.SUGAR_CANE || type == Material.CACTUS || type == Material.BAMBOO) {
                        Block below = block.getRelative(0, -1, 0);
                        if (below.getType() == type) {
                            harvestBlock(farmer, block, crop.getDropMaterial(), 1, fortuneChance, maxCapacity);
                            block.setType(Material.AIR);
                        }
                    } else if (type == Material.MELON || type == Material.PUMPKIN) {
                        int amount = (type == Material.MELON) ? (3 + random.nextInt(5)) : 1;
                        harvestBlock(farmer, block, crop.getDropMaterial(), amount, fortuneChance, maxCapacity);
                        block.setType(Material.AIR);
                    } else if (data instanceof Ageable ageable) {
                        if (ageable.getAge() == ageable.getMaximumAge()) {
                            int baseDrop = 1;
                            if (type == Material.CARROTS || type == Material.POTATOES || type == Material.BEETROOTS) {
                                baseDrop = 1 + random.nextInt(3);
                            } else if (type == Material.NETHER_WART) {
                                baseDrop = 2 + random.nextInt(3);
                            } else if (type == Material.SWEET_BERRY_BUSH) {
                                baseDrop = 2 + random.nextInt(2);
                            }

                            harvestBlock(farmer, block, crop.getDropMaterial(), baseDrop, fortuneChance, maxCapacity);

                            if (farmer.isAutoReplant()) {
                                ageable.setAge(0);
                                block.setBlockData(ageable);
                            } else {
                                block.setType(Material.AIR);
                            }
                        }
                    }
                }
            }
        }
    }

    private void harvestBlock(Farmer farmer, Block block, Material dropMaterial, int baseAmount, double fortuneChance, long maxCapacity) {
        int finalAmount = baseAmount;
        if (random.nextDouble() < fortuneChance) {
            finalAmount *= 2;
        }
        farmer.addStorage(dropMaterial, finalAmount, maxCapacity);
        plugin.getStatsService().recordHarvest(farmer.getOwnerUuid(), finalAmount);
    }
}
