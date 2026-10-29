package com.squezsaz.lfarmer.service;

import com.squezsaz.lfarmer.LFarmer;
import com.squezsaz.lfarmer.hook.EconomyHook;
import com.squezsaz.lfarmer.model.Farmer;
import com.squezsaz.lfarmer.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class AutoSellService {

    private final LFarmer plugin;
    private Object taskHandle;

    public AutoSellService(LFarmer plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        this.taskHandle = plugin.getScheduler().runAsyncRepeating(this::tickAutoSell, 5L, 5L);
    }

    public void stop() {
        if (taskHandle != null) {
            plugin.getScheduler().cancelTask(taskHandle);
            taskHandle = null;
        }
    }

    private void tickAutoSell() {
        EconomyHook eco = plugin.getHookManager().getEconomyHook();
        if (eco == null || !eco.isAvailable()) {
            return;
        }

        long now = System.currentTimeMillis();

        for (Farmer farmer : plugin.getFarmerManager().getAllFarmers().values()) {
            if (!farmer.isAutoSell()) {
                continue;
            }

            long intervalMs = farmer.getAutoSellInterval() * 1000L;
            if (now - farmer.getLastAutoSellTime() < intervalMs) {
                continue;
            }
            farmer.setLastAutoSellTime(now);

            OfflinePlayer owner = Bukkit.getOfflinePlayer(farmer.getOwnerUuid());
            Player onlinePlayer = owner.getPlayer();

            Map<Material, Long> snapshot = new HashMap<>(farmer.getStorage());
            double totalGross = 0.0;
            long totalSoldItems = 0;

            for (Map.Entry<Material, Long> entry : snapshot.entrySet()) {
                Material mat = entry.getKey();
                long amount = entry.getValue();
                if (amount <= 0) continue;

                double unitPrice = plugin.getHookManager().getPrice(onlinePlayer, mat);
                if (unitPrice > 0) {
                    farmer.withdrawStorage(mat, amount);
                    totalGross += unitPrice * amount;
                    totalSoldItems += amount;
                }
            }

            if (totalGross > 0) {
                double taxPercent = plugin.getFarmerManager().getLevelValue("tax", farmer.getTaxLevel());
                double taxAmount = totalGross * (taxPercent / 100.0);
                double totalNet = totalGross - taxAmount;

                eco.deposit(owner, totalNet);
                farmer.addEarned(totalNet);
                plugin.getStatsService().recordSale(farmer.getOwnerUuid(), totalSoldItems, totalNet);
                plugin.getLogService().log(farmer, "AUTO_SELL", "AUTO_SELL", plugin.getLanguageManager().format("logs.details.auto_sell", "{amount}", String.valueOf(totalSoldItems), "{net}", NumberUtil.format(totalNet), "{tax}", NumberUtil.format(taxAmount)));

                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                    if (plugin.getConfig().getBoolean("auto_sell.notify_actionbar", true)) {
                        plugin.getLanguageManager().sendActionBar(
                                onlinePlayer,
                                "auto_sell.actionbar_sold",
                                "{amount}", NumberUtil.formatCompact(totalSoldItems),
                                "{net}", NumberUtil.format(totalNet),
                                "{tax}", NumberUtil.format(taxAmount)
                        );
                    }
                }

                plugin.getNpcManager().updateHologram(farmer.getId());
            }
        }
    }
}
