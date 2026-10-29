package com.squezsaz.lfarmer.service;

import com.squezsaz.lfarmer.LFarmer;
import com.squezsaz.lfarmer.model.Farmer;
import com.squezsaz.lfarmer.model.FarmerLogEntry;
import org.bukkit.Location;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class LogService {

    private final LFarmer plugin;
    private final File logsFolder;
    private final DateTimeFormatter timeFormatter;
    private final DateTimeFormatter dateFormatter;

    public LogService(LFarmer plugin) {
        this.plugin = plugin;
        this.logsFolder = new File(plugin.getDataFolder(), "logs");
        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
        }
        this.timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        startAutoPurgeTask();
    }

    public void log(Farmer farmer, String actorName, String action, String details) {
        if (farmer == null) return;
        Location loc = farmer.getLocation();
        if (loc == null || loc.getWorld() == null) {
            loc = farmer.getLastKnownLocation();
        }
        log(farmer.getOwnerUuid(), farmer.getOwnerName(), actorName, action, details, loc);
    }

    public void log(Farmer farmer, String actorName, String action, String details, Location loc) {
        if (farmer == null) return;
        if (loc == null || loc.getWorld() == null) {
            loc = farmer.getLocation();
            if (loc == null || loc.getWorld() == null) {
                loc = farmer.getLastKnownLocation();
            }
        }
        log(farmer.getOwnerUuid(), farmer.getOwnerName(), actorName, action, details, loc);
    }

    public void log(UUID playerUuid, String playerName, String actorName, String action, String details, Location loc) {
        if (!plugin.getConfig().getBoolean("logging.enabled", true)) {
            return;
        }

        if (loc == null || loc.getWorld() == null) {
            org.bukkit.entity.Player actor = org.bukkit.Bukkit.getPlayer(actorName);
            if (actor != null && actor.isOnline() && actor.getWorld() != null) {
                loc = actor.getLocation();
            } else if (playerUuid != null) {
                org.bukkit.entity.Player owner = org.bukkit.Bukkit.getPlayer(playerUuid);
                if (owner != null && owner.isOnline() && owner.getWorld() != null) {
                    loc = owner.getLocation();
                }
            }
        }

        long now = System.currentTimeMillis();
        LocalDateTime localNow = LocalDateTime.now();
        String dateStr = localNow.format(dateFormatter);
        String timeStr = localNow.format(timeFormatter);

        String serverName = plugin.getConfig().getString("server_name", "Survival-1");
        String locStr = loc != null && loc.getWorld() != null
                ? loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")"
                : "World Unknown";

        String cleanPlayerName = playerName != null ? playerName : (playerUuid != null ? playerUuid.toString() : "Unknown");
        String cleanActor = actorName != null ? actorName : "SYSTEM";

        FarmerLogEntry entry = new FarmerLogEntry(
                0,
                now,
                dateStr,
                playerUuid != null ? playerUuid : new UUID(0L, 0L),
                cleanPlayerName,
                cleanActor,
                action,
                details,
                serverName,
                locStr
        );

        plugin.getScheduler().runAsync(() -> {
            plugin.getDatabaseManager().saveLogEntry(entry);
            writeLogToFile(cleanPlayerName, dateStr, timeStr, cleanActor, action, serverName, locStr, details);
        });
    }

    private void writeLogToFile(String playerName, String dateStr, String timeStr, String actor, String action, String server, String loc, String details) {
        File playerDir = new File(logsFolder, playerName);
        if (!playerDir.exists()) {
            playerDir.mkdirs();
        }

        File logFile = new File(playerDir, dateStr + ".log");
        String line = "[" + timeStr + "] [" + action + "] [Actor: " + actor + "] [Server: " + server + "] [" + loc + "] " + details;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write log file for " + playerName + ": " + e.getMessage());
        }
    }

    private void startAutoPurgeTask() {
        plugin.getScheduler().runAsyncRepeating(this::purgeOldLogs, 60L, 43200L);
    }

    public void purgeOldLogs() {
        int purgeDays = plugin.getConfig().getInt("logging.purge_after_days", 15);
        if (purgeDays <= 0) return;

        long cutoffMillis = System.currentTimeMillis() - (purgeDays * 86400000L);
        plugin.getDatabaseManager().purgeOldLogs(cutoffMillis);

        LocalDate cutoffDate = LocalDate.now().minusDays(purgeDays);
        if (!logsFolder.exists() || !logsFolder.isDirectory()) return;

        File[] playerDirs = logsFolder.listFiles(File::isDirectory);
        if (playerDirs == null) return;

        for (File pDir : playerDirs) {
            File[] files = pDir.listFiles((dir, name) -> name.endsWith(".log"));
            if (files != null) {
                for (File f : files) {
                    String name = f.getName().replace(".log", "");
                    try {
                        LocalDate fileDate = LocalDate.parse(name, dateFormatter);
                        if (fileDate.isBefore(cutoffDate)) {
                            f.delete();
                        }
                    } catch (Exception e) {
                        if (f.lastModified() < cutoffMillis) {
                            f.delete();
                        }
                    }
                }
            }
            File[] remaining = pDir.listFiles();
            if (remaining == null || remaining.length == 0) {
                pDir.delete();
            }
        }
    }
}
