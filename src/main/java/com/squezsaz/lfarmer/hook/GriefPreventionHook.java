package com.squezsaz.lfarmer.hook;

import com.squezsaz.lfarmer.LFarmer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class GriefPreventionHook implements ClaimHook {

    private final LFarmer plugin;
    private boolean available;
    private Object dataStore;
    private Method getClaimAtMethod;
    private Field ownerIdField;
    private Method allowAccessMethod;
    private Method allowBuildMethod;

    public GriefPreventionHook(LFarmer plugin) {
        this.plugin = plugin;
        Plugin gpPlugin = Bukkit.getPluginManager().getPlugin("GriefPrevention");
        if (gpPlugin != null) {
            try {
                Class<?> gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
                Field instanceField = gpClass.getField("instance");
                Object gpInstance = instanceField.get(null);
                Field dataStoreField = gpClass.getField("dataStore");
                this.dataStore = dataStoreField.get(gpInstance);

                Class<?> dsClass = Class.forName("me.ryanhamshire.GriefPrevention.DataStore");
                Class<?> claimClass = Class.forName("me.ryanhamshire.GriefPrevention.Claim");
                this.getClaimAtMethod = dsClass.getMethod("getClaimAt", Location.class, boolean.class, claimClass);
                this.ownerIdField = claimClass.getField("ownerID");
                this.allowAccessMethod = claimClass.getMethod("allowAccess", Player.class);
                try {
                    this.allowBuildMethod = claimClass.getMethod("allowBuild", Player.class, Material.class);
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
        return "GriefPrevention";
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
        List<String> disabledWorlds = plugin.getConfig().getStringList("hooks.griefprevention.disabled_worlds");
        for (String dw : disabledWorlds) {
            if (dw.equalsIgnoreCase(worldName)) {
                return "messages.disabled_world";
            }
        }

        try {
            Object claim = getClaimAtMethod.invoke(dataStore, location, false, null);
            if (claim == null) {
                boolean preventWild = plugin.getConfig().getBoolean("hooks.griefprevention.prevent_wilderness", true);
                return preventWild ? "messages.claim_deny_wilderness" : null;
            }

            UUID owner = (UUID) ownerIdField.get(claim);
            boolean allowOwners = plugin.getConfig().getBoolean("hooks.griefprevention.allow_owners", true);
            if (allowOwners && owner != null && owner.equals(player.getUniqueId())) {
                return null;
            }

            boolean allowTrusted = plugin.getConfig().getBoolean("hooks.griefprevention.allow_trusted", true);
            if (allowTrusted && allowBuildMethod != null) {
                Object result = allowBuildMethod.invoke(claim, player, Material.AIR);
                if (result == null) return null;
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
            Object claim = getClaimAtMethod.invoke(dataStore, location, false, null);
            if (claim == null) return true;

            UUID owner = (UUID) ownerIdField.get(claim);
            boolean allowOwners = plugin.getConfig().getBoolean("hooks.griefprevention.allow_owners", true);
            if (allowOwners && owner != null && owner.equals(player.getUniqueId())) {
                return true;
            }

            boolean allowTrusted = plugin.getConfig().getBoolean("hooks.griefprevention.allow_trusted", true);
            if (allowTrusted) {
                Object result = allowAccessMethod.invoke(claim, player);
                if (result == null) return true;
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
            Object claim = getClaimAtMethod.invoke(dataStore, location, false, null);
            if (claim != null) {
                return (UUID) ownerIdField.get(claim);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
