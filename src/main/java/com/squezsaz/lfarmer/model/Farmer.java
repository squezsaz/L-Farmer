package com.squezsaz.lfarmer.model;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Farmer {

    public static final Set<Material> COLLECTABLE_MATERIALS = Collections.unmodifiableSet(EnumSet.of(
            Material.WHEAT, Material.WHEAT_SEEDS, Material.CARROT, Material.POTATO,
            Material.BEETROOT, Material.BEETROOT_SEEDS, Material.NETHER_WART, Material.COCOA_BEANS,
            Material.SUGAR_CANE, Material.CACTUS, Material.MELON_SLICE, Material.MELON,
            Material.PUMPKIN, Material.SWEET_BERRIES, Material.BAMBOO,
            Material.ROTTEN_FLESH, Material.BONE, Material.STRING, Material.SPIDER_EYE,
            Material.GUNPOWDER, Material.ENDER_PEARL, Material.BLAZE_ROD, Material.GHAST_TEAR,
            Material.MAGMA_CREAM, Material.SLIME_BALL, Material.LEATHER, Material.BEEF,
            Material.PORKCHOP, Material.CHICKEN, Material.MUTTON, Material.FEATHER,
            Material.EGG, Material.WHITE_WOOL, Material.IRON_INGOT, Material.POPPY
    ));

    private final UUID id;
    private String serverName;
    private String world;
    private int chunkX;
    private int chunkZ;
    private Location location;
    private Location lastKnownLocation;
    private UUID ownerUuid;
    private String ownerName;

    private int capacityLevel;
    private int rangeLevel;
    private int speedLevel;
    private int fortuneLevel;
    private int taxLevel;

    private double fuelAmount;
    private boolean autoSell;
    private boolean autoReplant;
    private boolean mobCollect;
    private boolean cropCollect;
    private boolean groundCollect;

    private double totalEarned;
    private long totalCollected;

    private int autoSellInterval;
    private long lastAutoSellTime;
    private final Set<Material> disabledItems;

    private final Map<Material, Long> storage;
    private final Map<UUID, FarmerPermission> members;

    private volatile boolean dirty;

    public Farmer(UUID id, String world, int chunkX, int chunkZ, Location location, UUID ownerUuid, String ownerName) {
        this.id = id;
        this.serverName = "local";
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.location = location;
        if (location != null) {
            this.lastKnownLocation = location.clone();
        }
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;

        this.capacityLevel = 1;
        this.rangeLevel = 1;
        this.speedLevel = 1;
        this.fortuneLevel = 1;
        this.taxLevel = 1;

        this.fuelAmount = 0.0;
        this.autoSell = false;
        this.autoSellInterval = 60;
        this.lastAutoSellTime = System.currentTimeMillis();
        this.autoReplant = true;
        this.mobCollect = true;
        this.cropCollect = true;
        this.groundCollect = true;

        this.totalEarned = 0.0;
        this.totalCollected = 0L;

        this.disabledItems = ConcurrentHashMap.newKeySet();
        this.storage = new ConcurrentHashMap<>();
        this.members = new ConcurrentHashMap<>();
        this.dirty = true;
    }

    public UUID getId() {
        return id;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
        this.dirty = true;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
        this.dirty = true;
    }

    public int getChunkX() {
        return chunkX;
    }

    public void setChunkX(int chunkX) {
        this.chunkX = chunkX;
        this.dirty = true;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public void setChunkZ(int chunkZ) {
        this.chunkZ = chunkZ;
        this.dirty = true;
    }

    public String getChunkKey() {
        return world + ";" + chunkX + ";" + chunkZ;
    }

    public Location getLocation() {
        return location;
    }

    public Location getLastKnownLocation() {
        return lastKnownLocation != null ? lastKnownLocation : location;
    }

    public void setLocation(Location location) {
        if (location != null) {
            this.lastKnownLocation = location.clone();
        }
        this.location = location;
        this.dirty = true;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
        this.dirty = true;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
        this.dirty = true;
    }

    public int getCapacityLevel() {
        return capacityLevel;
    }

    public void setCapacityLevel(int capacityLevel) {
        this.capacityLevel = capacityLevel;
        this.dirty = true;
    }

    public int getRangeLevel() {
        return rangeLevel;
    }

    public void setRangeLevel(int rangeLevel) {
        this.rangeLevel = rangeLevel;
        this.dirty = true;
    }

    public int getSpeedLevel() {
        return speedLevel;
    }

    public void setSpeedLevel(int speedLevel) {
        this.speedLevel = speedLevel;
        this.dirty = true;
    }

    public int getFortuneLevel() {
        return fortuneLevel;
    }

    public void setFortuneLevel(int fortuneLevel) {
        this.fortuneLevel = fortuneLevel;
        this.dirty = true;
    }

    public int getTaxLevel() {
        return taxLevel;
    }

    public void setTaxLevel(int taxLevel) {
        this.taxLevel = taxLevel;
        this.dirty = true;
    }

    public double getFuelAmount() {
        return fuelAmount;
    }

    public void setFuelAmount(double fuelAmount) {
        this.fuelAmount = Math.max(0.0, fuelAmount);
        this.dirty = true;
    }

    public void addFuel(double amount, double maxFuel) {
        this.fuelAmount = Math.min(maxFuel, Math.max(0.0, this.fuelAmount + amount));
        this.dirty = true;
    }

    public void addFuel(double amount) {
        this.fuelAmount = Math.max(0.0, this.fuelAmount + amount);
        this.dirty = true;
    }

    public boolean consumeFuel(double amount) {
        if (this.fuelAmount < amount) {
            return false;
        }
        this.fuelAmount -= amount;
        this.dirty = true;
        return true;
    }

    public boolean isAutoSell() {
        return autoSell;
    }

    public void setAutoSell(boolean autoSell) {
        this.autoSell = autoSell;
        this.dirty = true;
    }

    public int getAutoSellInterval() {
        return autoSellInterval <= 0 ? 60 : autoSellInterval;
    }

    public void setAutoSellInterval(int autoSellInterval) {
        this.autoSellInterval = autoSellInterval;
        this.dirty = true;
    }

    public long getLastAutoSellTime() {
        return lastAutoSellTime;
    }

    public void setLastAutoSellTime(long lastAutoSellTime) {
        this.lastAutoSellTime = lastAutoSellTime;
    }

    public Set<Material> getDisabledItems() {
        return disabledItems;
    }

    public static boolean isCollectable(Material material) {
        return material != null && COLLECTABLE_MATERIALS.contains(material);
    }

    public boolean isItemAllowed(Material material) {
        if (!isCollectable(material)) {
            return false;
        }
        return !disabledItems.contains(material);
    }

    public void toggleItemAllowed(Material material) {
        if (disabledItems.contains(material)) {
            disabledItems.remove(material);
        } else {
            disabledItems.add(material);
        }
        this.dirty = true;
    }

    public boolean isAutoReplant() {
        return autoReplant;
    }

    public void setAutoReplant(boolean autoReplant) {
        this.autoReplant = autoReplant;
        this.dirty = true;
    }

    public boolean isMobCollect() {
        return mobCollect;
    }

    public void setMobCollect(boolean mobCollect) {
        this.mobCollect = mobCollect;
        this.dirty = true;
    }

    public boolean isCropCollect() {
        return cropCollect;
    }

    public void setCropCollect(boolean cropCollect) {
        this.cropCollect = cropCollect;
        this.dirty = true;
    }

    public boolean isGroundCollect() {
        return groundCollect;
    }

    public void setGroundCollect(boolean groundCollect) {
        this.groundCollect = groundCollect;
        this.dirty = true;
    }

    public double getTotalEarned() {
        return totalEarned;
    }

    public void setTotalEarned(double totalEarned) {
        this.totalEarned = totalEarned;
        this.dirty = true;
    }

    public void addEarned(double amount) {
        this.totalEarned += amount;
        this.dirty = true;
    }

    public long getTotalCollected() {
        return totalCollected;
    }

    public void setTotalCollected(long totalCollected) {
        this.totalCollected = totalCollected;
        this.dirty = true;
    }

    public void addCollected(long amount) {
        this.totalCollected += amount;
        this.dirty = true;
    }

    public Map<Material, Long> getStorage() {
        return storage;
    }

    public long getStoredAmount(Material material) {
        return storage.getOrDefault(material, 0L);
    }

    public long getTotalStored() {
        long sum = 0L;
        for (Long val : storage.values()) {
            sum += val;
        }
        return sum;
    }

    public boolean addStorage(Material material, long amount, long maxCapacity) {
        long currentTotal = getTotalStored();
        if (currentTotal >= maxCapacity) {
            return false;
        }
        long allowable = Math.min(amount, maxCapacity - currentTotal);
        storage.merge(material, allowable, Long::sum);
        addCollected(allowable);
        this.dirty = true;
        return allowable == amount;
    }

    public long withdrawStorage(Material material, long amount) {
        long current = getStoredAmount(material);
        if (current <= 0) {
            return 0;
        }
        long toRemove = Math.min(current, amount);
        long remaining = current - toRemove;
        if (remaining <= 0) {
            storage.remove(material);
        } else {
            storage.put(material, remaining);
        }
        this.dirty = true;
        return toRemove;
    }

    public Map<UUID, FarmerPermission> getMembers() {
        return members;
    }

    public FarmerPermission getPermission(UUID uuid) {
        if (ownerUuid != null && ownerUuid.equals(uuid)) {
            return new FarmerPermission(true, true, true, true);
        }
        return members.get(uuid);
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
