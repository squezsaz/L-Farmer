package com.squezsaz.lfarmer.hook;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

public class EconomyShopGUIPriceHook implements PriceProvider {

    private Method getItemSellPriceMethod;
    private boolean available;

    public EconomyShopGUIPriceHook() {
        if (Bukkit.getPluginManager().getPlugin("EconomyShopGUI") != null || Bukkit.getPluginManager().getPlugin("EconomyShopGUI-Premium") != null) {
            try {
                Class<?> apiClass = Class.forName("me.gypopo.economyshopgui.api.EconomyShopGUIHook");
                this.getItemSellPriceMethod = apiClass.getMethod("getItemSellPrice", Player.class, ItemStack.class);
                this.available = true;
            } catch (Exception ignored) {
                this.available = false;
            }
        }
    }

    @Override
    public String getName() {
        return "EconomyShopGUI";
    }

    @Override
    public boolean isAvailable() {
        return available && getItemSellPriceMethod != null;
    }

    @Override
    public double getSellPrice(Player player, Material material) {
        if (!isAvailable()) return 0.0;
        try {
            ItemStack stack = new ItemStack(material, 1);
            Object result = getItemSellPriceMethod.invoke(null, player, stack);
            if (result instanceof Number num) {
                double val = num.doubleValue();
                return val > 0 ? val : 0.0;
            }
        } catch (Exception ignored) {
        }
        return 0.0;
    }
}
