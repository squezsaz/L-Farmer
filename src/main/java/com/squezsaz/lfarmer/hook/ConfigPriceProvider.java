package com.squezsaz.lfarmer.hook;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

public class ConfigPriceProvider implements PriceProvider {

    private final Plugin plugin;
    private final Map<Material, Double> prices;

    public ConfigPriceProvider(Plugin plugin) {
        this.plugin = plugin;
        this.prices = new EnumMap<>(Material.class);
        load();
    }

    public void load() {
        prices.clear();
        File file = new File(plugin.getDataFolder(), "prices.yml");
        if (!file.exists()) {
            plugin.saveResource("prices.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("prices");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Material mat = Material.matchMaterial(key);
                if (mat != null) {
                    prices.put(mat, section.getDouble(key, 0.0));
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Config";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public double getSellPrice(Player player, Material material) {
        return prices.getOrDefault(material, 0.0);
    }
}
