package com.squezsaz.lfarmer.hook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class ItemsAdderHook {

    private boolean available;
    private Method spawnFurnitureMethod;
    private Method removeFurnitureMethod;

    public ItemsAdderHook() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ItemsAdder");
        if (plugin != null) {
            try {
                Class<?> furnitureClass = Class.forName("dev.lone.itemsadder.api.CustomFurniture");
                this.spawnFurnitureMethod = furnitureClass.getMethod("spawn", String.class, Location.class);
                this.removeFurnitureMethod = furnitureClass.getMethod("remove", Entity.class, boolean.class);
                this.available = true;
            } catch (Exception ignored) {
                this.available = false;
            }
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public Entity spawnCustomFurniture(String id, Location location) {
        if (!isAvailable() || id == null || id.isEmpty()) return null;
        try {
            Object customFurniture = spawnFurnitureMethod.invoke(null, id, location);
            if (customFurniture != null) {
                Method getArmorstandMethod = customFurniture.getClass().getMethod("getArmorstand");
                Object entity = getArmorstandMethod.invoke(customFurniture);
                if (entity instanceof Entity e) {
                    return e;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public void removeCustomFurniture(Entity entity) {
        if (!isAvailable() || entity == null) return;
        try {
            removeFurnitureMethod.invoke(null, entity, true);
        } catch (Exception ignored) {
            entity.remove();
        }
    }
}
