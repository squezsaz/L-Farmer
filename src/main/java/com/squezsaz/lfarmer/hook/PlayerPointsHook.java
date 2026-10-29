package com.squezsaz.lfarmer.hook;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

public class PlayerPointsHook implements EconomyHook {

    private Object pointsApi;
    private Method lookMethod;
    private Method takeMethod;
    private Method giveMethod;

    public PlayerPointsHook() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (plugin != null) {
            try {
                Method getApiMethod = plugin.getClass().getMethod("getAPI");
                this.pointsApi = getApiMethod.invoke(plugin);
                this.lookMethod = pointsApi.getClass().getMethod("look", UUID.class);
                this.takeMethod = pointsApi.getClass().getMethod("take", UUID.class, int.class);
                this.giveMethod = pointsApi.getClass().getMethod("give", UUID.class, int.class);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public String getName() {
        return "PlayerPoints";
    }

    @Override
    public boolean isAvailable() {
        return pointsApi != null;
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        if (!isAvailable() || player == null) return 0.0;
        try {
            Object result = lookMethod.invoke(pointsApi, player.getUniqueId());
            if (result instanceof Number num) {
                return num.doubleValue();
            }
        } catch (Exception ignored) {
        }
        return 0.0;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!isAvailable() || player == null) return false;
        try {
            Object result = takeMethod.invoke(pointsApi, player.getUniqueId(), (int) amount);
            if (result instanceof Boolean b) {
                return b;
            }
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        if (!isAvailable() || player == null) return false;
        try {
            Object result = giveMethod.invoke(pointsApi, player.getUniqueId(), (int) amount);
            if (result instanceof Boolean b) {
                return b;
            }
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }
}
