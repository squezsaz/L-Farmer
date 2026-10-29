package com.squezsaz.lfarmer.service;

import com.squezsaz.lfarmer.LFarmer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumMap;
import java.util.Map;

public class FuelService {

    private final LFarmer plugin;
    private final Map<Material, Double> fuelValues;

    public FuelService(LFarmer plugin) {
        this.plugin = plugin;
        this.fuelValues = new EnumMap<>(Material.class);
    }

    public void loadFuelConfig() {
        fuelValues.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("fuel.items");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Material mat = Material.matchMaterial(key);
                if (mat != null) {
                    fuelValues.put(mat, section.getDouble(key, 0.0));
                }
            }
        }
    }

    public boolean isFuelItem(Material material) {
        return fuelValues.containsKey(material);
    }

    public double getFuelValue(Material material) {
        return fuelValues.getOrDefault(material, 0.0);
    }

    public double getMaxFuel() {
        return plugin.getConfig().getDouble("fuel.max_capacity", 10000.0);
    }

    public double getSalaryCost() {
        return plugin.getConfig().getDouble("fuel.salary.cost", 500.0);
    }

    public double getSalaryFuelAmount() {
        return plugin.getConfig().getDouble("fuel.salary.fuel_reward", 1000.0);
    }
}
