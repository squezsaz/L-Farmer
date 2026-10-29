package com.squezsaz.lfarmer.hook;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

public class ShopGUIPlusPriceHook implements PriceProvider {

    private Method getItemStackPriceSellMethod;
    private boolean available;

    public ShopGUIPlusPriceHook() {
        if (Bukkit.getPluginManager().getPlugin("ShopGUIPlus") != null) {
            try {
                Class<?> apiClass = Class.forName("net.brcdev.shopgui.ShopGuiPlusApi");
                this.getItemStackPriceSellMethod = apiClass.getMethod("getItemStackPriceSell", Player.class, ItemStack.class);
                this.available = true;
            } catch (Exception ignored) {
                this.available = false;
            }
        }
    }

    @Override
    public String getName() {
        return "ShopGUIPlus";
    }

    @Override
    public boolean isAvailable() {
        return available && getItemStackPriceSellMethod != null;
    }

    @Override
    public double getSellPrice(Player player, Material material) {
        if (!isAvailable()) return 0.0;
        try {
            ItemStack stack = new ItemStack(material, 1);
            Object result = getItemStackPriceSellMethod.invoke(null, player, stack);
            if (result instanceof Number num) {
                double val = num.doubleValue();
                return val > 0 ? val : 0.0;
            }
        } catch (Exception ignored) {
        }
        return 0.0;
    }
}
