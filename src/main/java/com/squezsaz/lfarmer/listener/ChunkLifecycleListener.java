package com.squezsaz.lfarmer.listener;

import com.squezsaz.lfarmer.LFarmer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkLifecycleListener implements Listener {

    private final LFarmer plugin;

    public ChunkLifecycleListener(LFarmer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        plugin.getNpcManager().onChunkLoad(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        plugin.getNpcManager().onChunkUnload(event.getChunk());
    }
}
