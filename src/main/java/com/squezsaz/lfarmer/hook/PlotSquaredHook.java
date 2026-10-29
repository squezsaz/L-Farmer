package com.squezsaz.lfarmer.hook;

import com.squezsaz.lfarmer.LFarmer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class PlotSquaredHook implements ClaimHook {

    private final LFarmer plugin;
    private boolean available;
    private Method getPlotMethod;
    private Method isOwnerMethod;
    private Method isAddedMethod;
    private Method getOwnerMethod;

    public PlotSquaredHook(LFarmer plugin) {
        this.plugin = plugin;
        Plugin psPlugin = Bukkit.getPluginManager().getPlugin("PlotSquared");
        if (psPlugin != null) {
            try {
                Class<?> plotLocClass = Class.forName("com.plotsquared.core.location.Location");
                this.getPlotMethod = plotLocClass.getMethod("getPlot");
                Class<?> plotClass = Class.forName("com.plotsquared.core.plot.Plot");
                this.isOwnerMethod = plotClass.getMethod("isOwner", UUID.class);
                this.isAddedMethod = plotClass.getMethod("isAdded", UUID.class);
                this.getOwnerMethod = plotClass.getMethod("getOwner");
                this.available = true;
            } catch (Exception ignored) {
                this.available = false;
            }
        }
    }

    @Override
    public String getName() {
        return "PlotSquared";
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    private Object getPlotAt(Location location) {
        try {
            Class<?> plotLocClass = Class.forName("com.plotsquared.core.location.Location");
            Method atMethod = plotLocClass.getMethod("at", String.class, int.class, int.class, int.class);
            Object plotLoc = atMethod.invoke(null, location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
            return getPlotMethod.invoke(plotLoc);
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public String getPlaceDenyReason(Player player, Location location) {
        if (!isAvailable() || location == null || location.getWorld() == null) {
            return null;
        }

        String worldName = location.getWorld().getName();
        List<String> disabledWorlds = plugin.getConfig().getStringList("hooks.plotsquared.disabled_worlds");
        for (String dw : disabledWorlds) {
            if (dw.equalsIgnoreCase(worldName)) {
                return "messages.disabled_world";
            }
        }

        try {
            Object plot = getPlotAt(location);
            if (plot == null) {
                boolean preventWild = plugin.getConfig().getBoolean("hooks.plotsquared.prevent_wilderness", true);
                return preventWild ? "messages.claim_deny_wilderness" : null;
            }

            UUID owner = getClaimOwner(location);
            boolean allowOwners = plugin.getConfig().getBoolean("hooks.plotsquared.allow_owners", true);
            if (allowOwners && owner != null && owner.equals(player.getUniqueId())) {
                return null;
            }

            boolean allowTrusted = plugin.getConfig().getBoolean("hooks.plotsquared.allow_trusted", true);
            if (allowTrusted) {
                Object isOwner = isOwnerMethod.invoke(plot, player.getUniqueId());
                if (Boolean.TRUE.equals(isOwner)) return null;
                Object isAdded = isAddedMethod.invoke(plot, player.getUniqueId());
                if (Boolean.TRUE.equals(isAdded)) return null;
            }

            return "messages.claim_deny_place";
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public boolean canPlace(Player player, Location location) {
        return getPlaceDenyReason(player, location) == null;
    }

    @Override
    public boolean canAccess(Player player, Location location) {
        if (!isAvailable()) return true;
        Object plot = getPlotAt(location);
        if (plot == null) return true;
        try {
            UUID owner = getClaimOwner(location);
            boolean allowOwners = plugin.getConfig().getBoolean("hooks.plotsquared.allow_owners", true);
            if (allowOwners && owner != null && owner.equals(player.getUniqueId())) {
                return true;
            }

            boolean allowTrusted = plugin.getConfig().getBoolean("hooks.plotsquared.allow_trusted", true);
            if (allowTrusted) {
                Object isOwner = isOwnerMethod.invoke(plot, player.getUniqueId());
                if (Boolean.TRUE.equals(isOwner)) return true;
                Object isAdded = isAddedMethod.invoke(plot, player.getUniqueId());
                return Boolean.TRUE.equals(isAdded);
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public UUID getClaimOwner(Location location) {
        if (!isAvailable()) return null;
        Object plot = getPlotAt(location);
        if (plot == null) return null;
        try {
            return (UUID) getOwnerMethod.invoke(plot);
        } catch (Exception ignored) {
        }
        return null;
    }
}
