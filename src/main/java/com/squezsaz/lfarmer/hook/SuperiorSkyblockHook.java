package com.squezsaz.lfarmer.hook;

import com.squezsaz.lfarmer.LFarmer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class SuperiorSkyblockHook implements ClaimHook {

    private final LFarmer plugin;
    private boolean available;
    private Method getIslandAtMethod;
    private Method getOwnerMethod;
    private Method getUniqueIdMethod;
    private Method isMemberMethod;

    public SuperiorSkyblockHook(LFarmer plugin) {
        this.plugin = plugin;
        if (Bukkit.getPluginManager().getPlugin("SuperiorSkyblock2") != null) {
            try {
                Class<?> apiClass = Class.forName("com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI");
                this.getIslandAtMethod = apiClass.getMethod("getIslandAt", Location.class);
                Class<?> islandClass = Class.forName("com.bgsoftware.superiorskyblock.api.island.Island");
                this.getOwnerMethod = islandClass.getMethod("getOwner");
                Class<?> playerClass = Class.forName("com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer");
                this.getUniqueIdMethod = playerClass.getMethod("getUniqueId");
                this.isMemberMethod = islandClass.getMethod("isMember", playerClass);
                this.available = true;
            } catch (Exception ignored) {
                this.available = false;
            }
        }
    }

    @Override
    public String getName() {
        return "SuperiorSkyblock2";
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String getPlaceDenyReason(Player player, Location location) {
        if (!isAvailable() || location == null || location.getWorld() == null) {
            return null;
        }

        String worldName = location.getWorld().getName();
        List<String> disabledWorlds = plugin.getConfig().getStringList("hooks.superiorskyblock.disabled_worlds");
        for (String dw : disabledWorlds) {
            if (dw.equalsIgnoreCase(worldName)) {
                return "messages.disabled_world";
            }
        }

        try {
            Object island = getIslandAtMethod.invoke(null, location);
            if (island == null) {
                boolean preventWild = plugin.getConfig().getBoolean("hooks.superiorskyblock.prevent_wilderness", true);
                return preventWild ? "messages.claim_deny_wilderness" : null;
            }

            UUID owner = getClaimOwner(location);
            boolean allowLeader = plugin.getConfig().getBoolean("hooks.superiorskyblock.allow_leader", true);
            if (allowLeader && owner != null && owner.equals(player.getUniqueId())) {
                return null;
            }

            boolean allowMembers = plugin.getConfig().getBoolean("hooks.superiorskyblock.allow_members", true);
            if (allowMembers) {
                Class<?> apiClass = Class.forName("com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI");
                Method getPlayerMethod = apiClass.getMethod("getPlayer", Player.class);
                Object sPlayer = getPlayerMethod.invoke(null, player);
                Object result = isMemberMethod.invoke(island, sPlayer);
                if (Boolean.TRUE.equals(result)) {
                    return null;
                }
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
        try {
            Object island = getIslandAtMethod.invoke(null, location);
            if (island == null) return true;

            UUID owner = getClaimOwner(location);
            boolean allowLeader = plugin.getConfig().getBoolean("hooks.superiorskyblock.allow_leader", true);
            if (allowLeader && owner != null && owner.equals(player.getUniqueId())) {
                return true;
            }

            boolean allowMembers = plugin.getConfig().getBoolean("hooks.superiorskyblock.allow_members", true);
            if (allowMembers) {
                Class<?> apiClass = Class.forName("com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI");
                Method getPlayerMethod = apiClass.getMethod("getPlayer", Player.class);
                Object sPlayer = getPlayerMethod.invoke(null, player);
                Object result = isMemberMethod.invoke(island, sPlayer);
                if (result instanceof Boolean b) {
                    return b;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public UUID getClaimOwner(Location location) {
        if (!isAvailable()) return null;
        try {
            Object island = getIslandAtMethod.invoke(null, location);
            if (island != null) {
                Object sPlayer = getOwnerMethod.invoke(island);
                if (sPlayer != null) {
                    return (UUID) getUniqueIdMethod.invoke(sPlayer);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
