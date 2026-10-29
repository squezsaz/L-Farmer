package com.squezsaz.lfarmer.hook;

import org.bukkit.Material;
import org.bukkit.entity.Player;

public interface PriceProvider {

    String getName();

    boolean isAvailable();

    double getSellPrice(Player player, Material material);
}
