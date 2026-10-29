package com.squezsaz.lfarmer.hook;

import com.squezsaz.lfarmer.LFarmer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class LandsHook implements ClaimHook {

    private final LFarmer plugin;
    private boolean available;
    private Object landsIntegration;
    private Method getAreaMethod;
    private Method getOwnerUidMethod;
    private Method isTrustedMethod;
    private Method getLandMethod;

    public LandsHook(LFarmer plugin) {
        this.plugin = plugin;
        if (Bukkit.getPluginManager().getPlugin("Lands") != null) {
            try {
                Class<?> integrationClass = Class.forName("me.angeschossen.lands.api.LandsIntegration");
                Method ofMethod = integrationClass.getMethod("of", Plugin.class);
                this.landsIntegration = ofMethod.invoke(null, plugin);
                this.getAreaMethod = integrationClass.getMethod("getArea", Location.class);

                Class<?> areaClass = Class.forName("me.angeschossen.lands.api.land.Area");
                this.getOwnerUidMethod = areaClass.getMethod("getOwnerUID");
                this.isTrustedMethod = areaClass.getMethod("isTrusted", UUID.class);
                try {
                    this.getLandMethod = areaClass.getMethod("getLand");
                } catch (Exception ignored) {
                }
                this.available = true;
            } catch (Exception ignored) {
                this.available = false;
            }
        }
    }

    @Override
    public String getName() {
        return "Lands";
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
        List<String> disabledWorlds = plugin.getConfig().getStringList("hooks.lands.disabled_worlds");
        for (String dw : disabledWorlds) {
            if (dw.equalsIgnoreCase(worldName)) {
                return "messages.disabled_world";
            }
        }

        try {
            Object area = getAreaMethod.invoke(landsIntegration, location);
            if (area == null) {
                boolean preventWild = plugin.getConfig().getBoolean("hooks.lands.prevent_wilderness", true);
                return preventWild ? "messages.claim_deny_wilderness" : null;
            }

            if (getLandMethod != null) {
                Object land = getLandMethod.invoke(area);
                if (land != null) {
                    Method getNameMethod = land.getClass().getMethod("getName");
                    String landName = (String) getNameMethod.invoke(land);
                    List<String> disabledLands = plugin.getConfig().getStringList("hooks.lands.disabled_lands");
                    for (String dl : disabledLands) {
                        if (dl.equalsIgnoreCase(landName)) {
                            return "messages.disabled_region";
                        }
                    }
                }
            }

            UUID owner = (UUID) getOwnerUidMethod.invoke(area);
            boolean allowOwners = plugin.getConfig().getBoolean("hooks.lands.allow_owners", true);
            if (allowOwners && owner != null && owner.equals(player.getUniqueId())) {
                return null;
            }

            boolean allowTrusted = plugin.getConfig().getBoolean("hooks.lands.allow_trusted", true);
            boolean allowMembers = plugin.getConfig().getBoolean("hooks.lands.allow_members", true);
            if (allowTrusted || allowMembers) {
                Object trusted = isTrustedMethod.invoke(area, player.getUniqueId());
                if (Boolean.TRUE.equals(trusted)) {
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
            Object area = getAreaMethod.invoke(landsIntegration, location);
            if (area == null) return true;

            UUID owner = (UUID) getOwnerUidMethod.invoke(area);
            boolean allowOwners = plugin.getConfig().getBoolean("hooks.lands.allow_owners", true);
            if (allowOwners && owner != null && owner.equals(player.getUniqueId())) {
                return true;
            }

            boolean allowTrusted = plugin.getConfig().getBoolean("hooks.lands.allow_trusted", true);
            boolean allowMembers = plugin.getConfig().getBoolean("hooks.lands.allow_members", true);
            if (allowTrusted || allowMembers) {
                Object trusted = isTrustedMethod.invoke(area, player.getUniqueId());
                if (Boolean.TRUE.equals(trusted)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ignored) {
        }
        return true;
    }

    @Override
    public UUID getClaimOwner(Location location) {
        if (!isAvailable()) return null;
        try {
            Object area = getAreaMethod.invoke(landsIntegration, location);
            if (area != null) {
                return (UUID) getOwnerUidMethod.invoke(area);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
