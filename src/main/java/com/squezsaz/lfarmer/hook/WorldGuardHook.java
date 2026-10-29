package com.squezsaz.lfarmer.hook;

import com.squezsaz.lfarmer.LFarmer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class WorldGuardHook implements ClaimHook {

    private final LFarmer plugin;
    private boolean available;
    private Object regionContainer;
    private Method wrapPlayerMethod;

    public WorldGuardHook(LFarmer plugin) {
        this.plugin = plugin;
        Plugin wgPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (wgPlugin != null) {
            try {
                Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
                Method getInstanceMethod = wgClass.getMethod("getInstance");
                Object wgInstance = getInstanceMethod.invoke(null);
                Method getPlatformMethod = wgClass.getMethod("getPlatform");
                Object platform = getPlatformMethod.invoke(wgInstance);
                Method getRegionContainerMethod = platform.getClass().getMethod("getRegionContainer");
                this.regionContainer = getRegionContainerMethod.invoke(platform);

                Class<?> wgPluginClass = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
                this.wrapPlayerMethod = wgPluginClass.getMethod("wrapPlayer", Player.class);

                this.available = true;
            } catch (Exception ignored) {
                this.available = false;
            }
        }
    }

    @Override
    public String getName() {
        return "WorldGuard";
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
        List<String> disabledWorlds = plugin.getConfig().getStringList("hooks.worldguard.disabled_worlds");
        for (String dw : disabledWorlds) {
            if (dw.equalsIgnoreCase(worldName)) {
                return "messages.disabled_world";
            }
        }

        try {
            Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Method adaptLocation = bukkitAdapter.getMethod("adapt", Location.class);
            Object weLoc = adaptLocation.invoke(null, location);

            Method createQueryMethod = regionContainer.getClass().getMethod("createQuery");
            Object query = createQueryMethod.invoke(regionContainer);

            Method getApplicable = query.getClass().getMethod("getApplicableRegions", weLoc.getClass());
            Object set = getApplicable.invoke(query, weLoc);

            if (set != null) {
                Method sizeMethod = set.getClass().getMethod("size");
                int size = (Integer) sizeMethod.invoke(set);

                if (size == 0) {
                    boolean preventWild = plugin.getConfig().getBoolean("hooks.worldguard.prevent_wilderness", true);
                    return preventWild ? "messages.claim_deny_wilderness" : null;
                }

                Method getRegionsMethod = set.getClass().getMethod("getRegions");
                Object regionsObj = getRegionsMethod.invoke(set);
                if (regionsObj instanceof Collection<?> regions) {
                    List<String> disabledRegions = plugin.getConfig().getStringList("hooks.worldguard.disabled_regions");
                    for (Object region : regions) {
                        Method getIdMethod = region.getClass().getMethod("getId");
                        String regionId = (String) getIdMethod.invoke(region);
                        for (String dr : disabledRegions) {
                            if (dr.equalsIgnoreCase(regionId)) {
                                return "messages.disabled_region";
                            }
                        }
                    }
                }

                Object wrappedPlayer = wrapPlayerMethod.invoke(null, player);
                boolean requireBuild = plugin.getConfig().getBoolean("hooks.worldguard.require_build_flag", true);
                if (requireBuild) {
                    Class<?> flagsClass = Class.forName("com.sk89q.worldguard.protection.flags.Flags");
                    Object buildFlag = flagsClass.getField("BUILD").get(null);

                    Method testStateMethod = query.getClass().getMethod("testState", weLoc.getClass(), Class.forName("com.sk89q.worldguard.protection.association.RegionAssociable"), buildFlag.getClass().getSuperclass());
                    Object result = testStateMethod.invoke(query, weLoc, wrappedPlayer, buildFlag);
                    if (result instanceof Boolean b && !b) {
                        return "messages.claim_deny_place";
                    }
                } else {
                    boolean allowOwners = plugin.getConfig().getBoolean("hooks.worldguard.allow_owners", true);
                    boolean allowMembers = plugin.getConfig().getBoolean("hooks.worldguard.allow_members", true);

                    Method isOwnerOfAll = set.getClass().getMethod("isOwnerOfAll", Class.forName("com.sk89q.worldguard.LocalPlayer"));
                    boolean isOwner = allowOwners && Boolean.TRUE.equals(isOwnerOfAll.invoke(set, wrappedPlayer));

                    Method isMemberOfAll = set.getClass().getMethod("isMemberOfAll", Class.forName("com.sk89q.worldguard.LocalPlayer"));
                    boolean isMember = allowMembers && Boolean.TRUE.equals(isMemberOfAll.invoke(set, wrappedPlayer));

                    if (!isOwner && !isMember) {
                        return "messages.claim_deny_place";
                    }
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    @Override
    public boolean canPlace(Player player, Location location) {
        return getPlaceDenyReason(player, location) == null;
    }

    @Override
    public boolean canAccess(Player player, Location location) {
        if (!isAvailable()) return true;
        try {
            Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Method adaptLocation = bukkitAdapter.getMethod("adapt", Location.class);
            Object weLoc = adaptLocation.invoke(null, location);

            Method createQueryMethod = regionContainer.getClass().getMethod("createQuery");
            Object query = createQueryMethod.invoke(regionContainer);

            Object wrappedPlayer = wrapPlayerMethod.invoke(null, player);

            boolean requireBuild = plugin.getConfig().getBoolean("hooks.worldguard.require_build_flag", true);
            if (requireBuild) {
                Class<?> flagsClass = Class.forName("com.sk89q.worldguard.protection.flags.Flags");
                Object buildFlag = flagsClass.getField("BUILD").get(null);

                Method testStateMethod = query.getClass().getMethod("testState", weLoc.getClass(), Class.forName("com.sk89q.worldguard.protection.association.RegionAssociable"), buildFlag.getClass().getSuperclass());
                Object result = testStateMethod.invoke(query, weLoc, wrappedPlayer, buildFlag);
                if (result instanceof Boolean b) {
                    return b;
                }
            } else {
                boolean allowOwners = plugin.getConfig().getBoolean("hooks.worldguard.allow_owners", true);
                boolean allowMembers = plugin.getConfig().getBoolean("hooks.worldguard.allow_members", true);

                Method getApplicable = query.getClass().getMethod("getApplicableRegions", weLoc.getClass());
                Object set = getApplicable.invoke(query, weLoc);
                if (set != null) {
                    Method isOwnerOfAll = set.getClass().getMethod("isOwnerOfAll", Class.forName("com.sk89q.worldguard.LocalPlayer"));
                    if (allowOwners && Boolean.TRUE.equals(isOwnerOfAll.invoke(set, wrappedPlayer))) {
                        return true;
                    }
                    Method isMemberOfAll = set.getClass().getMethod("isMemberOfAll", Class.forName("com.sk89q.worldguard.LocalPlayer"));
                    if (allowMembers && Boolean.TRUE.equals(isMemberOfAll.invoke(set, wrappedPlayer))) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return true;
    }

    @Override
    public UUID getClaimOwner(Location location) {
        return null;
    }
}
