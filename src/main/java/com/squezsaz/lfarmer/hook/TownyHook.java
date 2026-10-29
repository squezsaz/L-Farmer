package com.squezsaz.lfarmer.hook;

import com.squezsaz.lfarmer.LFarmer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class TownyHook implements ClaimHook {

    private final LFarmer plugin;
    private boolean available;
    private Object townyApi;
    private Method getTownBlockMethod;
    private Method getResidentMethod;

    public TownyHook(LFarmer plugin) {
        this.plugin = plugin;
        Plugin townyPlugin = Bukkit.getPluginManager().getPlugin("Towny");
        if (townyPlugin != null) {
            try {
                Class<?> apiClass = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
                Method getInstanceMethod = apiClass.getMethod("getInstance");
                this.townyApi = getInstanceMethod.invoke(null);
                this.getTownBlockMethod = apiClass.getMethod("getTownBlock", Location.class);
                this.getResidentMethod = apiClass.getMethod("getResident", Player.class);
                this.available = true;
            } catch (Exception ignored) {
                this.available = false;
            }
        }
    }

    @Override
    public String getName() {
        return "Towny";
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
        List<String> disabledWorlds = plugin.getConfig().getStringList("hooks.towny.disabled_worlds");
        for (String dw : disabledWorlds) {
            if (dw.equalsIgnoreCase(worldName)) {
                return "messages.disabled_world";
            }
        }

        try {
            Object townBlock = getTownBlockMethod.invoke(townyApi, location);
            if (townBlock == null) {
                boolean preventWild = plugin.getConfig().getBoolean("hooks.towny.prevent_wilderness", true);
                return preventWild ? "messages.claim_deny_wilderness" : null;
            }

            Method getTownMethod = townBlock.getClass().getMethod("getTown");
            Object town = getTownMethod.invoke(townBlock);
            if (town == null) {
                boolean preventWild = plugin.getConfig().getBoolean("hooks.towny.prevent_wilderness", true);
                return preventWild ? "messages.claim_deny_wilderness" : null;
            }

            Method getNameMethod = town.getClass().getMethod("getName");
            String townName = (String) getNameMethod.invoke(town);
            List<String> disabledTowns = plugin.getConfig().getStringList("hooks.towny.disabled_towns");
            for (String dt : disabledTowns) {
                if (dt.equalsIgnoreCase(townName)) {
                    return "messages.disabled_region";
                }
            }

            UUID owner = getClaimOwner(location);
            boolean allowMayor = plugin.getConfig().getBoolean("hooks.towny.allow_mayor", true);
            if (allowMayor && owner != null && owner.equals(player.getUniqueId())) {
                return null;
            }

            boolean allowResidents = plugin.getConfig().getBoolean("hooks.towny.allow_residents", true);
            if (allowResidents) {
                Object resident = getResidentMethod.invoke(townyApi, player);
                if (resident != null) {
                    Method hasResidentMethod = town.getClass().getMethod("hasResident", resident.getClass());
                    Object result = hasResidentMethod.invoke(town, resident);
                    if (Boolean.TRUE.equals(result)) {
                        return null;
                    }
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
            Object townBlock = getTownBlockMethod.invoke(townyApi, location);
            if (townBlock == null) return true;
            Method getTownMethod = townBlock.getClass().getMethod("getTown");
            Object town = getTownMethod.invoke(townBlock);
            if (town == null) return true;

            UUID owner = getClaimOwner(location);
            boolean allowMayor = plugin.getConfig().getBoolean("hooks.towny.allow_mayor", true);
            if (allowMayor && owner != null && owner.equals(player.getUniqueId())) {
                return true;
            }

            boolean allowResidents = plugin.getConfig().getBoolean("hooks.towny.allow_residents", true);
            if (allowResidents) {
                Object resident = getResidentMethod.invoke(townyApi, player);
                if (resident == null) return false;
                Method hasResidentMethod = town.getClass().getMethod("hasResident", resident.getClass());
                Object result = hasResidentMethod.invoke(town, resident);
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
            Object townBlock = getTownBlockMethod.invoke(townyApi, location);
            if (townBlock != null) {
                Method getTownMethod = townBlock.getClass().getMethod("getTown");
                Object town = getTownMethod.invoke(townBlock);
                if (town != null) {
                    Method getMayorMethod = town.getClass().getMethod("getMayor");
                    Object mayor = getMayorMethod.invoke(town);
                    if (mayor != null) {
                        Method getUuidMethod = mayor.getClass().getMethod("getUUID");
                        return (UUID) getUuidMethod.invoke(mayor);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
