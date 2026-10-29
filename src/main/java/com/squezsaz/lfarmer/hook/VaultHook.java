package com.squezsaz.lfarmer.hook;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook implements EconomyHook {

    private Economy economy;

    public VaultHook() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                this.economy = rsp.getProvider();
            }
        }
    }

    @Override
    public String getName() {
        return "Vault";
    }

    @Override
    public boolean isAvailable() {
        return economy != null;
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        if (!isAvailable() || player == null) return 0.0;
        return economy.getBalance(player);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        if (!isAvailable() || player == null) return false;
        return economy.has(player, amount);
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!isAvailable() || player == null) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        if (!isAvailable() || player == null) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }
}
