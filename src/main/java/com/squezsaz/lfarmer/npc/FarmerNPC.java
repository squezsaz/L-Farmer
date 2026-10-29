package com.squezsaz.lfarmer.npc;

import com.squezsaz.lfarmer.LFarmer;
import com.squezsaz.lfarmer.hook.ItemsAdderHook;
import com.squezsaz.lfarmer.model.Farmer;
import com.squezsaz.lfarmer.util.ColorUtil;
import com.squezsaz.lfarmer.util.NumberUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;

import java.util.UUID;

public class FarmerNPC {

    private final LFarmer plugin;
    private final Farmer farmer;
    private Entity npcEntity;
    private TextDisplay hologramDisplay;

    public FarmerNPC(LFarmer plugin, Farmer farmer) {
        this.plugin = plugin;
        this.farmer = farmer;
    }

    public void spawn() {
        Location loc = farmer.getLocation();
        if (loc == null || loc.getWorld() == null) {
            return;
        }

        plugin.getScheduler().runAtLocation(loc, () -> {
            despawn();

            boolean itemsAdderEnabled = plugin.getConfig().getBoolean("itemsadder.enabled", false);
            String customModel = plugin.getConfig().getString("itemsadder.custom_model", "");
            ItemsAdderHook ia = plugin.getHookManager().getItemsAdderHook();
            if (itemsAdderEnabled && ia != null && ia.isAvailable() && !customModel.isEmpty()) {
                this.npcEntity = ia.spawnCustomFurniture(customModel, loc);
            }

            if (this.npcEntity == null) {
                String typeStr = plugin.getConfig().getString("npc.type", "VILLAGER").toUpperCase();
                EntityType type = EntityType.VILLAGER;
                try {
                    type = EntityType.valueOf(typeStr);
                } catch (IllegalArgumentException ignored) {
                }

                this.npcEntity = loc.getWorld().spawn(loc, type.getEntityClass(), entity -> {
                    entity.setPersistent(false);
                    entity.setSilent(true);
                    entity.setInvulnerable(true);
                    entity.setGravity(false);
                    if (entity instanceof LivingEntity living) {
                        living.setAI(false);
                        living.setCollidable(false);
                        living.setRemoveWhenFarAway(false);
                    }
                    if (entity instanceof Villager villager) {
                        villager.setProfession(Villager.Profession.FARMER);
                        villager.setVillagerLevel(farmer.getCapacityLevel());
                    }
                });
            }

            spawnHologram();
        });
    }

    private void spawnHologram() {
        Location loc = farmer.getLocation();
        if (loc == null || loc.getWorld() == null) {
            return;
        }

        Location holoLoc = loc.clone().add(0, 2.2, 0);
        this.hologramDisplay = holoLoc.getWorld().spawn(holoLoc, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setPersistent(false);
            display.text(buildHologramText());
            display.setDefaultBackground(false);
            display.setShadowed(true);
        });
    }

    public void updateHologram() {
        if (hologramDisplay != null && hologramDisplay.isValid()) {
            hologramDisplay.text(buildHologramText());
        }
    }

    private Component buildHologramText() {
        String template = plugin.getLanguageManager().getRaw("hologram.lines");
        if (template == null || template.isEmpty()) {
            template = "<gradient:#ffb347:#ffcc33><bold>{owner}'in Çiftçisi</bold></gradient>\n" +
                    "<gray>Seviye: <yellow>{level}</yellow></gray>\n" +
                    "<gray>Depo: <gold>{stored}/{max}</gold></gray>\n" +
                    "<green>● Çalışıyor</green>";
        }

        long maxCap = (long) plugin.getFarmerManager().getLevelValue("capacity", farmer.getCapacityLevel());
        long stored = farmer.getTotalStored();

        String status;
        if (plugin.getConfig().getBoolean("fuel.enabled", false) && farmer.getFuelAmount() <= 0) {
            status = plugin.getLanguageManager().getRaw("hologram.status_no_fuel");
        } else if (stored >= maxCap) {
            status = plugin.getLanguageManager().getRaw("hologram.status_full");
        } else {
            status = plugin.getLanguageManager().getRaw("hologram.status_working");
        }

        String rendered = template
                .replace("{owner}", farmer.getOwnerName())
                .replace("{level}", String.valueOf(farmer.getCapacityLevel()))
                .replace("{stored}", NumberUtil.formatCompact(stored))
                .replace("{max}", NumberUtil.formatCompact(maxCap))
                .replace("{status}", status);

        return ColorUtil.parse(rendered);
    }

    public void despawn() {
        if (hologramDisplay != null && hologramDisplay.isValid()) {
            hologramDisplay.remove();
            hologramDisplay = null;
        }
        if (npcEntity != null && npcEntity.isValid()) {
            ItemsAdderHook ia = plugin.getHookManager().getItemsAdderHook();
            if (ia != null && ia.isAvailable()) {
                ia.removeCustomFurniture(npcEntity);
            } else {
                npcEntity.remove();
            }
            npcEntity = null;
        }
    }

    public Entity getNpcEntity() {
        return npcEntity;
    }

    public TextDisplay getHologramDisplay() {
        return hologramDisplay;
    }

    public Farmer getFarmer() {
        return farmer;
    }
}
