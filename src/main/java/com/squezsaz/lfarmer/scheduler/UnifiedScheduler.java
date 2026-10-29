package com.squezsaz.lfarmer.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class UnifiedScheduler {

    private final Plugin plugin;
    private final boolean isFolia;

    public UnifiedScheduler(Plugin plugin) {
        this.plugin = plugin;
        boolean foliaCheck = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            foliaCheck = true;
        } catch (ClassNotFoundException ignored) {
        }
        this.isFolia = foliaCheck;
    }

    public boolean isFolia() {
        return isFolia;
    }

    public void runAsync(Runnable runnable) {
        if (isFolia) {
            try {
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Method runNowMethod = asyncScheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class);
                runNowMethod.invoke(asyncScheduler, plugin, (Consumer<Object>) task -> runnable.run());
            } catch (Exception e) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    public void runSync(Runnable runnable) {
        if (isFolia) {
            try {
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method executeMethod = globalScheduler.getClass().getMethod("execute", Plugin.class, Runnable.class);
                executeMethod.invoke(globalScheduler, plugin, runnable);
            } catch (Exception e) {
                runnable.run();
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public void runAtLocation(Location location, Runnable runnable) {
        if (isFolia) {
            try {
                Object regionScheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
                Method executeMethod = regionScheduler.getClass().getMethod("execute", Plugin.class, Location.class, Runnable.class);
                executeMethod.invoke(regionScheduler, plugin, location, runnable);
            } catch (Exception e) {
                runSync(runnable);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public Object runAtLocationRepeating(Location location, Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia) {
            try {
                Object regionScheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
                Method runAtFixedRateMethod = regionScheduler.getClass().getMethod(
                        "runAtFixedRate",
                        Plugin.class,
                        Location.class,
                        Consumer.class,
                        long.class,
                        long.class
                );
                return runAtFixedRateMethod.invoke(regionScheduler, plugin, location, (Consumer<Object>) task -> runnable.run(), Math.max(1, delayTicks), Math.max(1, periodTicks));
            } catch (Exception e) {
                return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
            }
        } else {
            return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
        }
    }

    public Object runGlobalRepeating(Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia) {
            try {
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method runAtFixedRateMethod = globalScheduler.getClass().getMethod(
                        "runAtFixedRate",
                        Plugin.class,
                        Consumer.class,
                        long.class,
                        long.class
                );
                return runAtFixedRateMethod.invoke(globalScheduler, plugin, (Consumer<Object>) task -> runnable.run(), Math.max(1, delayTicks), Math.max(1, periodTicks));
            } catch (Exception e) {
                return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
            }
        } else {
            return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
        }
    }

    public Object runAsyncRepeating(Runnable runnable, long delaySeconds, long periodSeconds) {
        if (isFolia) {
            try {
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Method runAtFixedRateMethod = asyncScheduler.getClass().getMethod(
                        "runAtFixedRate",
                        Plugin.class,
                        Consumer.class,
                        long.class,
                        long.class,
                        TimeUnit.class
                );
                return runAtFixedRateMethod.invoke(asyncScheduler, plugin, (Consumer<Object>) task -> runnable.run(), delaySeconds, periodSeconds, TimeUnit.SECONDS);
            } catch (Exception e) {
                return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delaySeconds * 20L, periodSeconds * 20L);
            }
        } else {
            return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delaySeconds * 20L, periodSeconds * 20L);
        }
    }

    public void cancelTask(Object taskHandle) {
        if (taskHandle == null) return;
        if (taskHandle instanceof BukkitTask bukkitTask) {
            bukkitTask.cancel();
            return;
        }
        try {
            Method cancelMethod = taskHandle.getClass().getMethod("cancel");
            cancelMethod.invoke(taskHandle);
        } catch (Exception ignored) {
        }
    }
}
