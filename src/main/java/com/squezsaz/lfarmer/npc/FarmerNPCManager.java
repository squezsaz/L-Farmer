package com.squezsaz.lfarmer.npc;

import com.squezsaz.lfarmer.LFarmer;
import com.squezsaz.lfarmer.model.Farmer;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FarmerNPCManager {

    private final LFarmer plugin;
    private final Map<UUID, FarmerNPC> npcs;

    public FarmerNPCManager(LFarmer plugin) {
        this.plugin = plugin;
        this.npcs = new ConcurrentHashMap<>();
    }

    public void spawnNPC(Farmer farmer) {
        despawnNPC(farmer.getId());
        FarmerNPC npc = new FarmerNPC(plugin, farmer);
        npcs.put(farmer.getId(), npc);
        npc.spawn();
    }

    public void despawnNPC(UUID farmerId) {
        FarmerNPC npc = npcs.remove(farmerId);
        if (npc != null) {
            npc.despawn();
        }
    }

    public void updateHologram(UUID farmerId) {
        FarmerNPC npc = npcs.get(farmerId);
        if (npc != null) {
            npc.updateHologram();
        }
    }

    public FarmerNPC getNPCByEntity(Entity entity) {
        if (entity == null) return null;
        for (FarmerNPC npc : npcs.values()) {
            if (entity.equals(npc.getNpcEntity()) || entity.equals(npc.getHologramDisplay())) {
                return npc;
            }
        }
        return null;
    }

    public void onChunkLoad(Chunk chunk) {
        Farmer farmer = plugin.getFarmerManager().getFarmer(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        if (farmer != null) {
            spawnNPC(farmer);
        }
    }

    public void onChunkUnload(Chunk chunk) {
        Farmer farmer = plugin.getFarmerManager().getFarmer(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        if (farmer != null) {
            despawnNPC(farmer.getId());
        }
    }

    public void despawnAll() {
        for (FarmerNPC npc : npcs.values()) {
            npc.despawn();
        }
        npcs.clear();
    }
}
