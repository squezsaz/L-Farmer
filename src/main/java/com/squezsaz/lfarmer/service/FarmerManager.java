package com.squezsaz.lfarmer.service;

import com.squezsaz.lfarmer.LFarmer;
import com.squezsaz.lfarmer.model.Farmer;
import com.squezsaz.lfarmer.model.FarmerLevel;
import com.squezsaz.lfarmer.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FarmerManager {

    private final LFarmer plugin;
    private final Map<UUID, Farmer> farmersById;
    private final Map<String, Farmer> farmersByChunk;
    private final Map<String, Map<Integer, FarmerLevel>> upgradeLevels;
    private final NamespacedKey farmerItemKey;
    private final NamespacedKey farmerUuidKey;

    public FarmerManager(LFarmer plugin) {
        this.plugin = plugin;
        this.farmersById = new ConcurrentHashMap<>();
        this.farmersByChunk = new ConcurrentHashMap<>();
        this.upgradeLevels = new HashMap<>();
        this.farmerItemKey = new NamespacedKey(plugin, "farmer_item");
        this.farmerUuidKey = new NamespacedKey(plugin, "farmer_uuid");
    }

    public void loadLevels(FileConfiguration config) {
        upgradeLevels.clear();
        ConfigurationSection upgradesSec = config.getConfigurationSection("upgrades");
        if (upgradesSec == null) return;

        for (String upgradeKey : upgradesSec.getKeys(false)) {
            ConfigurationSection sec = upgradesSec.getConfigurationSection(upgradeKey);
            if (sec != null) {
                Map<Integer, FarmerLevel> levels = new HashMap<>();
                for (String levelKeyStr : sec.getKeys(false)) {
                    try {
                        int lvl = Integer.parseInt(levelKeyStr);
                        double cost = sec.getDouble(levelKeyStr + ".cost", 0.0);
                        double val = sec.getDouble(levelKeyStr + ".value", 0.0);
                        levels.put(lvl, new FarmerLevel(lvl, cost, val));
                    } catch (NumberFormatException ignored) {
                    }
                }
                upgradeLevels.put(upgradeKey.toLowerCase(), levels);
            }
        }
    }

    public void registerFarmer(Farmer farmer) {
        farmersById.put(farmer.getId(), farmer);
        if (farmer.getWorld() != null && !farmer.getWorld().equals("unplaced")) {
            farmersByChunk.put(farmer.getChunkKey(), farmer);
        }
    }

    public Farmer createFarmer(Player owner, Location location) {
        Chunk chunk = location.getChunk();
        Farmer farmer = new Farmer(
                UUID.randomUUID(),
                chunk.getWorld().getName(),
                chunk.getX(),
                chunk.getZ(),
                location,
                owner.getUniqueId(),
                owner.getName()
        );
        farmer.setServerName(plugin.getConfig().getString("server_name", "Survival-1"));
        registerFarmer(farmer);
        plugin.getDatabaseManager().saveFarmer(farmer, true);
        plugin.getNpcManager().spawnNPC(farmer);
        return farmer;
    }

    public void removeFarmer(Farmer farmer) {
        farmersById.remove(farmer.getId());
        farmersByChunk.remove(farmer.getChunkKey());
        plugin.getNpcManager().despawnNPC(farmer.getId());
        plugin.getDatabaseManager().deleteFarmer(farmer.getId(), true);
    }

    public Farmer getFarmer(UUID id) {
        return farmersById.get(id);
    }

    public Farmer getFarmer(String world, int chunkX, int chunkZ) {
        return farmersByChunk.get(world + ";" + chunkX + ";" + chunkZ);
    }

    public Farmer getFarmerCoveringChunk(String world, int chunkX, int chunkZ) {
        Farmer direct = getFarmer(world, chunkX, chunkZ);
        if (direct != null) return direct;

        for (Farmer farmer : farmersById.values()) {
            if (!farmer.getWorld().equalsIgnoreCase(world)) continue;
            int radius = (int) getLevelValue("range", farmer.getRangeLevel());
            if (radius <= 0) continue;
            if (Math.abs(farmer.getChunkX() - chunkX) <= radius && Math.abs(farmer.getChunkZ() - chunkZ) <= radius) {
                return farmer;
            }
        }
        return null;
    }

    public Farmer getFarmerAtLocation(Location location) {
        if (location == null || location.getWorld() == null) return null;
        Chunk chunk = location.getChunk();
        return getFarmer(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public List<Farmer> getFarmersByOwner(UUID ownerUuid) {
        List<Farmer> list = new ArrayList<>();
        for (Farmer farmer : farmersById.values()) {
            if (farmer.getOwnerUuid().equals(ownerUuid)) {
                list.add(farmer);
            }
        }
        return list;
    }

    public int getFarmerLimit(Player player) {
        if (player.hasPermission("farmer.limit.admin") || player.isOp()) {
            return 999;
        }
        int max = plugin.getConfig().getInt("default_limit", 1);
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("farmer.limit." + i)) {
                return Math.max(max, i);
            }
        }
        return max;
    }

    public boolean canPlaceMore(Player player) {
        int limit = getFarmerLimit(player);
        int current = getFarmersByOwner(player.getUniqueId()).size();
        return current < limit;
    }

    public FarmerLevel getLevelData(String upgradeType, int level) {
        Map<Integer, FarmerLevel> map = upgradeLevels.get(upgradeType.toLowerCase());
        if (map == null) return null;
        return map.get(level);
    }

    public double getLevelValue(String upgradeType, int level) {
        FarmerLevel data = getLevelData(upgradeType, level);
        if (data != null) {
            return data.getValue();
        }
        if (upgradeType.equalsIgnoreCase("capacity")) return 5000;
        if (upgradeType.equalsIgnoreCase("speed")) return 100;
        if (upgradeType.equalsIgnoreCase("tax")) return 20;
        return 1.0;
    }

    public int getMaxLevel(String upgradeType) {
        Map<Integer, FarmerLevel> map = upgradeLevels.get(upgradeType.toLowerCase());
        if (map == null || map.isEmpty()) return 1;
        int max = 1;
        for (Integer lvl : map.keySet()) {
            if (lvl > max) max = lvl;
        }
        return max;
    }

    public ItemStack getFarmerSpawnItem(int amount) {
        String matStr = plugin.getConfig().getString("farmer_item.material", "VILLAGER_SPAWN_EGG");
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.VILLAGER_SPAWN_EGG;

        String name = plugin.getConfig().getString("farmer_item.name", "<gold><bold>Çiftçi Eşyası</bold></gold>");
        List<String> lore = plugin.getConfig().getStringList("farmer_item.lore");
        int customModelData = plugin.getConfig().getInt("farmer_item.custom_model_data", 0);

        ItemStack stack = new ItemBuilder(mat, amount)
                .name(name)
                .lore(lore)
                .customModelData(customModelData)
                .glow(true)
                .build();

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(farmerItemKey, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public boolean isFarmerItem(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(farmerItemKey, PersistentDataType.BYTE);
    }

    public UUID getSavedFarmerUuid(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(farmerUuidKey, PersistentDataType.STRING)) {
            try {
                return UUID.fromString(pdc.get(farmerUuidKey, PersistentDataType.STRING));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    public ItemStack createSavedFarmerItem(Farmer farmer) {
        String matStr = plugin.getConfig().getString("farmer_item.material", "VILLAGER_SPAWN_EGG");
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.VILLAGER_SPAWN_EGG;

        double maxStorage = getLevelValue("capacity", farmer.getCapacityLevel());
        long stored = farmer.getTotalStored();
        int itemCount = farmer.getStorage().size();

        ItemStack stack = new ItemBuilder(mat)
                .name(plugin.getLanguageManager().format("items.farmer_item_saved_name", "{owner}", farmer.getOwnerName()))
                .lore(plugin.getLanguageManager().getStringList(
                        "items.farmer_item_saved_lore",
                        "{owner}", farmer.getOwnerName(),
                        "{stored}", com.squezsaz.lfarmer.util.NumberUtil.format(stored),
                        "{max}", com.squezsaz.lfarmer.util.NumberUtil.format(maxStorage),
                        "{capacity_level}", String.valueOf(farmer.getCapacityLevel()),
                        "{speed_level}", String.valueOf(farmer.getSpeedLevel()),
                        "{range_level}", String.valueOf(farmer.getRangeLevel()),
                        "{fuel}", com.squezsaz.lfarmer.util.NumberUtil.format(farmer.getFuelAmount()),
                        "{item_count}", String.valueOf(itemCount)
                ))
                .glow(true)
                .build();

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(farmerItemKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(farmerUuidKey, PersistentDataType.STRING, farmer.getId().toString());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public void pickupFarmer(Farmer farmer, Player player) {
        farmersByChunk.remove(farmer.getChunkKey());
        plugin.getNpcManager().despawnNPC(farmer.getId());
        farmer.setWorld("unplaced");
        farmer.setLocation(null);
        farmer.setChunkX(0);
        farmer.setChunkZ(0);
        farmer.setDirty(true);
        plugin.getDatabaseManager().saveFarmer(farmer, true);

        ItemStack item = createSavedFarmerItem(farmer);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        if (!overflow.isEmpty()) {
            for (ItemStack left : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
        }
    }

    public void pickupFarmerAtLocation(Farmer farmer, Location dropLoc) {
        farmersByChunk.remove(farmer.getChunkKey());
        plugin.getNpcManager().despawnNPC(farmer.getId());
        farmer.setWorld("unplaced");
        farmer.setLocation(null);
        farmer.setChunkX(0);
        farmer.setChunkZ(0);
        farmer.setDirty(true);
        plugin.getDatabaseManager().saveFarmer(farmer, true);

        ItemStack item = createSavedFarmerItem(farmer);
        if (dropLoc != null && dropLoc.getWorld() != null) {
            dropLoc.getWorld().dropItemNaturally(dropLoc, item);
        }
    }

    public void placeExistingFarmer(Farmer farmer, Player owner, Location location) {
        Chunk chunk = location.getChunk();
        farmer.setWorld(chunk.getWorld().getName());
        farmer.setChunkX(chunk.getX());
        farmer.setChunkZ(chunk.getZ());
        farmer.setLocation(location);
        farmer.setDirty(true);
        registerFarmer(farmer);
        plugin.getDatabaseManager().saveFarmer(farmer, true);
        plugin.getNpcManager().spawnNPC(farmer);
    }

    public NamespacedKey getFarmerItemKey() {
        return farmerItemKey;
    }

    public NamespacedKey getFarmerUuidKey() {
        return farmerUuidKey;
    }

    public Map<UUID, Farmer> getAllFarmers() {
        return farmersById;
    }
}
