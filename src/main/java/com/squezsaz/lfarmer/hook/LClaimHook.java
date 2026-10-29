package com.squezsaz.lfarmer.hook;

import com.squezsaz.lfarmer.LFarmer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class LClaimHook implements ClaimHook {

    private final LFarmer plugin;
    private boolean available;
    private Class<?> apiClass;
    private Class<?> townInfoClass;
    private Class<?> claimInfoClass;
    private Class<?> permissionClass;
    private Object permBuild;
    private Object permInteract;
    private Object permContainer;

    public LClaimHook(LFarmer plugin) {
        this.plugin = plugin;
        if (Bukkit.getPluginManager().getPlugin("L-Claim") != null) {
            try {
                this.apiClass = Class.forName("com.lclaim.api.LClaimAPI");
                this.townInfoClass = Class.forName("com.lclaim.api.TownInfo");
                this.claimInfoClass = Class.forName("com.lclaim.api.ClaimInfo");
                this.permissionClass = Class.forName("com.lclaim.api.Permission");
                for (Object constant : permissionClass.getEnumConstants()) {
                    String name = ((Enum<?>) constant).name();
                    if ("BUILD".equalsIgnoreCase(name)) this.permBuild = constant;
                    else if ("INTERACT".equalsIgnoreCase(name)) this.permInteract = constant;
                    else if ("CONTAINER".equalsIgnoreCase(name)) this.permContainer = constant;
                }
                this.available = true;
            } catch (Exception ignored) {
                this.available = false;
            }
        }
    }

    private Object getApi() {
        if (!available || apiClass == null) return null;
        try {
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(apiClass);
            if (rsp != null) {
                return rsp.getProvider();
            }
            Method getMethod = apiClass.getMethod("get");
            return getMethod.invoke(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public String getName() {
        return "L-Claim";
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
        List<String> disabledWorlds = plugin.getConfig().getStringList("hooks.lclaim.disabled_worlds");
        for (String dw : disabledWorlds) {
            if (dw.equalsIgnoreCase(worldName)) {
                return "messages.disabled_world";
            }
        }

        try {
            Object api = getApi();
            if (api == null) return null;

            Method isLoadedMethod = apiClass.getMethod("isDataLoaded");
            if (!((Boolean) isLoadedMethod.invoke(api))) return null;

            Chunk chunk = location.getChunk();
            Method isClaimedMethod = apiClass.getMethod("isClaimed", Chunk.class);
            boolean claimed = (Boolean) isClaimedMethod.invoke(api, chunk);
            if (!claimed) {
                boolean preventWild = plugin.getConfig().getBoolean("hooks.lclaim.prevent_wilderness", true);
                return preventWild ? "messages.claim_deny_wilderness" : null;
            }

            Method getTownAtMethod = apiClass.getMethod("getTownAt", Chunk.class);
            Object town = getTownAtMethod.invoke(api, chunk);
            if (town == null) {
                boolean preventWild = plugin.getConfig().getBoolean("hooks.lclaim.prevent_wilderness", true);
                return preventWild ? "messages.claim_deny_wilderness" : null;
            }

            Method getNameMethod = townInfoClass.getMethod("getName");
            String townName = (String) getNameMethod.invoke(town);
            List<String> disabledTowns = plugin.getConfig().getStringList("hooks.lclaim.disabled_towns");
            for (String dt : disabledTowns) {
                if (dt.equalsIgnoreCase(townName)) {
                    return "messages.disabled_region";
                }
            }

            UUID uuid = player.getUniqueId();
            boolean allowMayor = plugin.getConfig().getBoolean("hooks.lclaim.allow_mayor", true);
            Method isMayorMethod = townInfoClass.getMethod("isMayor", UUID.class);
            if (allowMayor && (Boolean) isMayorMethod.invoke(town, uuid)) {
                return null;
            }

            boolean allowTrusted = plugin.getConfig().getBoolean("hooks.lclaim.allow_trusted", true);
            Method isTrustedMethod = townInfoClass.getMethod("isTrusted", UUID.class);
            if (allowTrusted && (Boolean) isTrustedMethod.invoke(town, uuid)) {
                return null;
            }

            boolean allowPlots = plugin.getConfig().getBoolean("hooks.lclaim.allow_plot_permissions", true);
            if (allowPlots) {
                Method getClaimAtMethod = apiClass.getMethod("getClaimAt", Chunk.class);
                Object claim = getClaimAtMethod.invoke(api, chunk);
                if (claim != null && permBuild != null) {
                    Method hasPlotPermMethod = claimInfoClass.getMethod("hasPlotPermission", UUID.class, permissionClass);
                    if ((Boolean) hasPlotPermMethod.invoke(claim, uuid, permBuild)) {
                        return null;
                    }
                }
            }

            boolean allowMembersBuild = plugin.getConfig().getBoolean("hooks.lclaim.allow_members_with_build", true);
            if (allowMembersBuild) {
                Method isMemberMethod = townInfoClass.getMethod("isMember", UUID.class);
                if ((Boolean) isMemberMethod.invoke(town, uuid)) {
                    if (permBuild != null) {
                        Method hasPermMethod = townInfoClass.getMethod("hasPermission", UUID.class, permissionClass);
                        if ((Boolean) hasPermMethod.invoke(town, uuid, permBuild)) {
                            return null;
                        }
                    }
                }
            }

            return "messages.claim_deny_place";
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public boolean canPlace(Player player, Location location) {
        return getPlaceDenyReason(player, location) == null;
    }

    @Override
    public boolean canAccess(Player player, Location location) {
        if (!isAvailable() || location == null || location.getWorld() == null) {
            return true;
        }

        try {
            Object api = getApi();
            if (api == null) return true;

            Method isLoadedMethod = apiClass.getMethod("isDataLoaded");
            if (!((Boolean) isLoadedMethod.invoke(api))) return true;

            Chunk chunk = location.getChunk();
            Method isClaimedMethod = apiClass.getMethod("isClaimed", Chunk.class);
            if (!((Boolean) isClaimedMethod.invoke(api, chunk))) {
                return true;
            }

            Method getTownAtMethod = apiClass.getMethod("getTownAt", Chunk.class);
            Object town = getTownAtMethod.invoke(api, chunk);
            if (town == null) {
                return true;
            }

            UUID uuid = player.getUniqueId();
            boolean allowMayor = plugin.getConfig().getBoolean("hooks.lclaim.allow_mayor", true);
            Method isMayorMethod = townInfoClass.getMethod("isMayor", UUID.class);
            if (allowMayor && (Boolean) isMayorMethod.invoke(town, uuid)) {
                return true;
            }

            boolean allowTrusted = plugin.getConfig().getBoolean("hooks.lclaim.allow_trusted", true);
            Method isTrustedMethod = townInfoClass.getMethod("isTrusted", UUID.class);
            if (allowTrusted && (Boolean) isTrustedMethod.invoke(town, uuid)) {
                return true;
            }

            boolean reqInteract = plugin.getConfig().getBoolean("hooks.lclaim.require_interact_for_access", true);
            boolean reqContainer = plugin.getConfig().getBoolean("hooks.lclaim.require_container_for_access", true);

            boolean allowPlots = plugin.getConfig().getBoolean("hooks.lclaim.allow_plot_permissions", true);
            if (allowPlots) {
                Method getClaimAtMethod = apiClass.getMethod("getClaimAt", Chunk.class);
                Object claim = getClaimAtMethod.invoke(api, chunk);
                if (claim != null) {
                    Method hasPlotPermMethod = claimInfoClass.getMethod("hasPlotPermission", UUID.class, permissionClass);
                    boolean hasInteract = permInteract != null && (Boolean) hasPlotPermMethod.invoke(claim, uuid, permInteract);
                    boolean hasContainer = permContainer != null && (Boolean) hasPlotPermMethod.invoke(claim, uuid, permContainer);
                    if (reqInteract && reqContainer) {
                        if (hasInteract && hasContainer) return true;
                    } else if (reqInteract) {
                        if (hasInteract) return true;
                    } else if (reqContainer) {
                        if (hasContainer) return true;
                    } else {
                        return true;
                    }
                }
            }

            Method isMemberMethod = townInfoClass.getMethod("isMember", UUID.class);
            if ((Boolean) isMemberMethod.invoke(town, uuid)) {
                Method hasPermMethod = townInfoClass.getMethod("hasPermission", UUID.class, permissionClass);
                boolean hasInteract = permInteract != null && (Boolean) hasPermMethod.invoke(town, uuid, permInteract);
                boolean hasContainer = permContainer != null && (Boolean) hasPermMethod.invoke(town, uuid, permContainer);
                if (reqInteract && reqContainer) {
                    if (hasInteract && hasContainer) return true;
                } else if (reqInteract) {
                    if (hasInteract) return true;
                } else if (reqContainer) {
                    if (hasContainer) return true;
                } else {
                    return true;
                }
            }

            return false;
        } catch (Throwable ignored) {
            return true;
        }
    }

    @Override
    public UUID getClaimOwner(Location location) {
        if (!isAvailable() || location == null || location.getWorld() == null) {
            return null;
        }

        try {
            Object api = getApi();
            if (api == null) return null;
            Method getTownAtMethod = apiClass.getMethod("getTownAt", Chunk.class);
            Object town = getTownAtMethod.invoke(api, location.getChunk());
            if (town == null) return null;
            Method getMayorMethod = townInfoClass.getMethod("getMayor");
            return (UUID) getMayorMethod.invoke(town);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
