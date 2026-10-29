package com.squezsaz.lfarmer.service;

import com.squezsaz.lfarmer.LFarmer;
import com.squezsaz.lfarmer.model.Farmer;
import com.squezsaz.lfarmer.model.PlayerStats;
import com.squezsaz.lfarmer.model.StatCategory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StatsService {

    private final LFarmer plugin;
    private final Map<UUID, Map<String, PlayerStats>> cache;
    private final Set<String> dirtyKeys;

    public StatsService(LFarmer plugin) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
        this.dirtyKeys = ConcurrentHashMap.newKeySet();
    }

    private String getToday() {
        return LocalDate.now().toString();
    }

    private PlayerStats getOrCreateTodayStats(UUID playerUuid) {
        String today = getToday();
        Map<String, PlayerStats> playerMap = cache.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>());
        return playerMap.computeIfAbsent(today, k -> {
            PlayerStats loaded = plugin.getDatabaseManager().loadPlayerStatsForDate(playerUuid, today);
            return loaded != null ? loaded : new PlayerStats();
        });
    }

    private void markDirty(UUID playerUuid, String date) {
        dirtyKeys.add(playerUuid.toString() + ";" + date);
    }

    public synchronized void recordHarvest(UUID playerUuid, long amount) {
        if (playerUuid == null || amount <= 0) return;
        PlayerStats stats = getOrCreateTodayStats(playerUuid);
        stats.addCropsHarvested(amount);
        markDirty(playerUuid, getToday());
    }

    public synchronized void recordMobCollect(UUID playerUuid, long amount) {
        if (playerUuid == null || amount <= 0) return;
        PlayerStats stats = getOrCreateTodayStats(playerUuid);
        stats.addMobsCollected(amount);
        markDirty(playerUuid, getToday());
    }

    public synchronized void recordSale(UUID playerUuid, long count, double money) {
        if (playerUuid == null || (count <= 0 && money <= 0)) return;
        PlayerStats stats = getOrCreateTodayStats(playerUuid);
        if (count > 0) stats.addItemsSold(count);
        if (money > 0) stats.addMoneyEarned(money);
        markDirty(playerUuid, getToday());
    }

    public synchronized void recordFuel(UUID playerUuid, double fuel) {
        if (playerUuid == null || fuel <= 0) return;
        PlayerStats stats = getOrCreateTodayStats(playerUuid);
        stats.addFuelUsed(fuel);
        markDirty(playerUuid, getToday());
    }

    public PlayerStats getStats(UUID playerUuid, StatCategory category) {
        if (playerUuid == null) return new PlayerStats();
        LocalDate now = LocalDate.now();
        PlayerStats aggregate = new PlayerStats();

        if (category == StatCategory.DAILY) {
            PlayerStats today = getOrCreateTodayStats(playerUuid);
            aggregate.merge(today);
            return aggregate;
        }

        if (category == StatCategory.WEEKLY) {
            String startDate = now.minusDays(6).toString();
            PlayerStats dbStats = plugin.getDatabaseManager().loadAggregatedPlayerStats(playerUuid, startDate);
            aggregate.merge(dbStats);

            Map<String, PlayerStats> playerMap = cache.get(playerUuid);
            if (playerMap != null) {
                PlayerStats today = playerMap.get(getToday());
                if (today != null && dirtyKeys.contains(playerUuid.toString() + ";" + getToday())) {
                    aggregate.merge(today);
                }
            }
            return aggregate;
        }

        if (category == StatCategory.MONTHLY) {
            String startDate = now.minusDays(29).toString();
            PlayerStats dbStats = plugin.getDatabaseManager().loadAggregatedPlayerStats(playerUuid, startDate);
            aggregate.merge(dbStats);

            Map<String, PlayerStats> playerMap = cache.get(playerUuid);
            if (playerMap != null) {
                PlayerStats today = playerMap.get(getToday());
                if (today != null && dirtyKeys.contains(playerUuid.toString() + ";" + getToday())) {
                    aggregate.merge(today);
                }
            }
            return aggregate;
        }

        PlayerStats allTimeDb = plugin.getDatabaseManager().loadAggregatedPlayerStats(playerUuid, null);
        aggregate.merge(allTimeDb);

        Map<String, PlayerStats> playerMap = cache.get(playerUuid);
        if (playerMap != null) {
            PlayerStats today = playerMap.get(getToday());
            if (today != null && dirtyKeys.contains(playerUuid.toString() + ";" + getToday())) {
                aggregate.merge(today);
            }
        }

        List<Farmer> farmers = plugin.getFarmerManager().getFarmersByOwner(playerUuid);
        long cumulativeCollected = 0;
        double cumulativeEarned = 0;
        for (Farmer f : farmers) {
            cumulativeCollected += f.getTotalCollected();
            cumulativeEarned += f.getTotalEarned();
        }
        if (aggregate.getCropsHarvested() + aggregate.getMobsCollected() < cumulativeCollected) {
            long diff = cumulativeCollected - (aggregate.getCropsHarvested() + aggregate.getMobsCollected());
            aggregate.addCropsHarvested(diff);
        }
        if (aggregate.getMoneyEarned() < cumulativeEarned) {
            double diffMoney = cumulativeEarned - aggregate.getMoneyEarned();
            aggregate.addMoneyEarned(diffMoney);
        }

        return aggregate;
    }

    public synchronized void flushDirty() {
        if (dirtyKeys.isEmpty()) return;

        for (String key : dirtyKeys) {
            String[] parts = key.split(";");
            if (parts.length != 2) continue;
            UUID playerUuid = UUID.fromString(parts[0]);
            String date = parts[1];

            Map<String, PlayerStats> playerMap = cache.get(playerUuid);
            if (playerMap != null) {
                PlayerStats stats = playerMap.get(date);
                if (stats != null) {
                    plugin.getDatabaseManager().savePlayerStats(playerUuid, date, stats);
                }
            }
        }
        dirtyKeys.clear();
    }
}
