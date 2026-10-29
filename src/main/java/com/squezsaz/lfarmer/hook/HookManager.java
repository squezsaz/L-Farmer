package com.squezsaz.lfarmer.hook;

import com.squezsaz.lfarmer.LFarmer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class HookManager {

    private final LFarmer plugin;
    private EconomyHook economyHook;
    private final List<PriceProvider> priceProviders;
    private final List<ClaimHook> claimHooks;
    private ItemsAdderHook itemsAdderHook;
    private ConfigPriceProvider configPriceProvider;

    public HookManager(LFarmer plugin) {
        this.plugin = plugin;
        this.priceProviders = new ArrayList<>();
        this.claimHooks = new ArrayList<>();
    }

    public void init() {
        priceProviders.clear();
        claimHooks.clear();

        if (plugin.getConfig().getBoolean("hooks.vault.enabled", true)) {
            VaultHook vaultHook = new VaultHook();
            if (vaultHook.isAvailable()) {
                this.economyHook = vaultHook;
            }
        }

        if (this.economyHook == null && plugin.getConfig().getBoolean("hooks.playerpoints.enabled", true)) {
            PlayerPointsHook pointsHook = new PlayerPointsHook();
            if (pointsHook.isAvailable()) {
                this.economyHook = pointsHook;
            }
        }

        if (plugin.getConfig().getBoolean("hooks.shopguiplus.enabled", true)) {
            ShopGUIPlusPriceHook sgp = new ShopGUIPlusPriceHook();
            if (sgp.isAvailable()) {
                priceProviders.add(sgp);
            }
        }

        if (plugin.getConfig().getBoolean("hooks.economyshopgui.enabled", true)) {
            EconomyShopGUIPriceHook esg = new EconomyShopGUIPriceHook();
            if (esg.isAvailable()) {
                priceProviders.add(esg);
            }
        }

        this.configPriceProvider = new ConfigPriceProvider(plugin);
        priceProviders.add(configPriceProvider);

        if (plugin.getConfig().getBoolean("hooks.superiorskyblock.enabled", true)) {
            SuperiorSkyblockHook ssb = new SuperiorSkyblockHook(plugin);
            if (ssb.isAvailable()) claimHooks.add(ssb);
        }

        if (plugin.getConfig().getBoolean("hooks.bentobox.enabled", true)) {
            BentoBoxHook bbox = new BentoBoxHook(plugin);
            if (bbox.isAvailable()) claimHooks.add(bbox);
        }

        if (plugin.getConfig().getBoolean("hooks.lands.enabled", true)) {
            LandsHook lands = new LandsHook(plugin);
            if (lands.isAvailable()) claimHooks.add(lands);
        }

        if (plugin.getConfig().getBoolean("hooks.griefprevention.enabled", true)) {
            GriefPreventionHook gp = new GriefPreventionHook(plugin);
            if (gp.isAvailable()) claimHooks.add(gp);
        }

        if (plugin.getConfig().getBoolean("hooks.worldguard.enabled", true)) {
            WorldGuardHook wg = new WorldGuardHook(plugin);
            if (wg.isAvailable()) claimHooks.add(wg);
        }

        if (plugin.getConfig().getBoolean("hooks.towny.enabled", true)) {
            TownyHook towny = new TownyHook(plugin);
            if (towny.isAvailable()) claimHooks.add(towny);
        }

        if (plugin.getConfig().getBoolean("hooks.plotsquared.enabled", true)) {
            PlotSquaredHook plot = new PlotSquaredHook(plugin);
            if (plot.isAvailable()) claimHooks.add(plot);
        }

        if (plugin.getConfig().getBoolean("hooks.lclaim.enabled", true)) {
            LClaimHook lclaim = new LClaimHook(plugin);
            if (lclaim.isAvailable()) claimHooks.add(lclaim);
        }

        if (plugin.getConfig().getBoolean("hooks.itemsadder.enabled", true) && plugin.getConfig().getBoolean("itemsadder.enabled", false)) {
            this.itemsAdderHook = new ItemsAdderHook();
        } else {
            this.itemsAdderHook = null;
        }

        if (plugin.getConfig().getBoolean("hooks.placeholderapi.enabled", true) && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new LFarmerExpansion(plugin).register();
        }
    }

    public EconomyHook getEconomyHook() {
        return economyHook;
    }

    public ItemsAdderHook getItemsAdderHook() {
        return itemsAdderHook;
    }

    public ConfigPriceProvider getConfigPriceProvider() {
        return configPriceProvider;
    }

    public double getPrice(Player player, Material material) {
        for (PriceProvider provider : priceProviders) {
            if (provider.isAvailable()) {
                double price = provider.getSellPrice(player, material);
                if (price > 0) {
                    return price;
                }
            }
        }
        return 0.0;
    }

    public String getPlaceDenyReason(Player player, Location location) {
        if (location == null || location.getWorld() == null) return null;
        String worldName = location.getWorld().getName();
        List<String> globalDisabledWorlds = plugin.getConfig().getStringList("disabled_worlds");
        for (String w : globalDisabledWorlds) {
            if (w.equalsIgnoreCase(worldName)) {
                return "messages.disabled_world";
            }
        }

        for (ClaimHook hook : claimHooks) {
            if (hook.isAvailable()) {
                String reason = hook.getPlaceDenyReason(player, location);
                if (reason != null) {
                    return reason;
                }
            }
        }
        return null;
    }

    public boolean canPlaceFarmer(Player player, Location location) {
        return getPlaceDenyReason(player, location) == null;
    }

    public boolean canAccessFarmer(Player player, Location location) {
        for (ClaimHook hook : claimHooks) {
            if (hook.isAvailable()) {
                if (!hook.canAccess(player, location)) {
                    return false;
                }
            }
        }
        return true;
    }
}
