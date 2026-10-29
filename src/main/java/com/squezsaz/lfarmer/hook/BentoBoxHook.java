package com.squezsaz.lfarmer.hook;

import com.squezsaz.lfarmer.LFarmer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class BentoBoxHook implements ClaimHook {

    private final LFarmer plugin;
    private boolean available;
    private Object islandsManager;
    private Method getIslandAtMethod;
    private Method getOwnerMethod;
    private Method getMemberSetMethod;

    public BentoBoxHook(LFarmer plugin) {
        this.plugin = plugin;
        if (Bukkit.getPluginManager().getPlugin("BentoBox") != null) {
            try {
                Class<?> bentoBoxClass = Class.forName("world.bentobox.bentobox.BentoBox");
                Method getInstanceMethod = bentoBoxClass.getMethod("getInstance");
                Object bentoBox = getInstanceMethod.invoke(null);
                Method getIslandsMethod = bentoBoxClass.getMethod("getIslands");
                this.islandsManager = getIslandsMethod.invoke(bentoBox);
                this.getIslandAtMethod = islandsManager.getClass().getMethod("getIslandAt", Location.class);
                Class<?> islandClass = Class.forName("world.bentobox.bentobox.database.objects.Island");
                this.getOwnerMethod = islandClass.getMethod("getOwner");
                this.getMemberSetMethod = islandClass.getMethod("getMemberSet");
                this.available = true;
            } catch (Exception ignored) {
                this.available = false;
            }
        }
    }

    @Override
    public String getName() {
        return "BentoBox";
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
        List<String> disabledWorlds = plugin.getConfig().getStringList("hooks.bentobox.disabled_worlds");
        for (String dw : disabledWorlds) {
            if (dw.equalsIgnoreCase(worldName)) {
                return "messages.disabled_world";
            }
        }

        try {
            Object opt = getIslandAtMethod.invoke(islandsManager, location);
            if (opt instanceof Optional<?> optional) {
                if (!optional.isPresent()) {
                    boolean preventWild = plugin.getConfig().getBoolean("hooks.bentobox.prevent_wilderness", true);
                    return preventWild ? "messages.claim_deny_wilderness" : null;
                }

                Object island = optional.get();
                UUID owner = (UUID) getOwnerMethod.invoke(island);

                boolean allowOwner = plugin.getConfig().getBoolean("hooks.bentobox.allow_owner", true);
                if (allowOwner && owner != null && owner.equals(player.getUniqueId())) {
                    return null;
                }

                boolean allowMembers = plugin.getConfig().getBoolean("hooks.bentobox.allow_members", true);
                if (allowMembers) {
                    Object members = getMemberSetMethod.invoke(island);
                    if (members instanceof Set<?> set && set.contains(player.getUniqueId())) {
                        return null;
                    }
                }

                return "messages.claim_deny_place";
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
            Object opt = getIslandAtMethod.invoke(islandsManager, location);
            if (opt instanceof Optional<?> optional) {
                if (optional.isPresent()) {
                    Object island = optional.get();
                    UUID owner = (UUID) getOwnerMethod.invoke(island);

                    boolean allowOwner = plugin.getConfig().getBoolean("hooks.bentobox.allow_owner", true);
                    if (allowOwner && owner != null && owner.equals(player.getUniqueId())) {
                        return true;
                    }

                    boolean allowMembers = plugin.getConfig().getBoolean("hooks.bentobox.allow_members", true);
                    if (allowMembers) {
                        Object members = getMemberSetMethod.invoke(island);
                        if (members instanceof Set<?> set) {
                            return set.contains(player.getUniqueId());
                        }
                    }
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
            Object opt = getIslandAtMethod.invoke(islandsManager, location);
            if (opt instanceof Optional<?> optional) {
                if (optional.isPresent()) {
                    Object island = optional.get();
                    return (UUID) getOwnerMethod.invoke(island);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
