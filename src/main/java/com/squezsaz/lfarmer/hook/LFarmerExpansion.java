package com.squezsaz.lfarmer.hook;

import com.squezsaz.lfarmer.LFarmer;
import com.squezsaz.lfarmer.model.Farmer;
import com.squezsaz.lfarmer.util.NumberUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LFarmerExpansion extends PlaceholderExpansion {

    private final LFarmer plugin;

    public LFarmerExpansion(LFarmer plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "lfarmer";
    }

    @Override
    public @NotNull String getAuthor() {
        return "squezsaz";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        if (params.equalsIgnoreCase("placed")) {
            List<Farmer> farmers = plugin.getFarmerManager().getFarmersByOwner(player.getUniqueId());
            return String.valueOf(farmers.size());
        }

        if (params.equalsIgnoreCase("max")) {
            return String.valueOf(plugin.getFarmerManager().getFarmerLimit(player));
        }

        if (params.equalsIgnoreCase("storage_total")) {
            List<Farmer> farmers = plugin.getFarmerManager().getFarmersByOwner(player.getUniqueId());
            long sum = 0;
            for (Farmer f : farmers) {
                sum += f.getTotalStored();
            }
            return NumberUtil.formatCompact(sum);
        }

        if (params.equalsIgnoreCase("total_earned")) {
            List<Farmer> farmers = plugin.getFarmerManager().getFarmersByOwner(player.getUniqueId());
            double sum = 0;
            for (Farmer f : farmers) {
                sum += f.getTotalEarned();
            }
            return NumberUtil.formatCompact(sum);
        }

        if (params.equalsIgnoreCase("total_collected")) {
            List<Farmer> farmers = plugin.getFarmerManager().getFarmersByOwner(player.getUniqueId());
            long sum = 0;
            for (Farmer f : farmers) {
                sum += f.getTotalCollected();
            }
            return NumberUtil.formatCompact(sum);
        }

        if (params.equalsIgnoreCase("chunk_has_farmer")) {
            Chunk chunk = player.getLocation().getChunk();
            Farmer farmer = plugin.getFarmerManager().getFarmer(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            return farmer != null ? "true" : "false";
        }

        if (params.equalsIgnoreCase("chunk_farmer_level")) {
            Chunk chunk = player.getLocation().getChunk();
            Farmer farmer = plugin.getFarmerManager().getFarmer(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            return farmer != null ? String.valueOf(farmer.getCapacityLevel()) : "0";
        }

        if (params.equalsIgnoreCase("chunk_farmer_owner")) {
            Chunk chunk = player.getLocation().getChunk();
            Farmer farmer = plugin.getFarmerManager().getFarmer(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            return farmer != null ? farmer.getOwnerName() : "";
        }

        return null;
    }
}
