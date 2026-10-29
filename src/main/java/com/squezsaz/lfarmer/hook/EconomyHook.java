package com.squezsaz.lfarmer.hook;

import org.bukkit.OfflinePlayer;

import java.util.UUID;

public interface EconomyHook {

    String getName();

    boolean isAvailable();

    double getBalance(OfflinePlayer player);

    boolean has(OfflinePlayer player, double amount);

    boolean withdraw(OfflinePlayer player, double amount);

    boolean deposit(OfflinePlayer player, double amount);
}
