package com.squezsaz.lfarmer.gui;

import com.squezsaz.lfarmer.model.Farmer;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class FarmerGUIHolder implements InventoryHolder {

    public enum Type {
        MAIN,
        STORAGE,
        UPGRADES,
        SETTINGS,
        ITEM_FILTER,
        FUEL,
        MEMBERS,
        ADD_MEMBER,
        ADMIN_FARMERS,
        STATS,
        LOGS
    }

    private final Farmer farmer;
    private final Type type;
    private final int page;
    private final Map<Integer, Consumer<InventoryClickEvent>> clickActions;
    private Inventory inventory;

    public FarmerGUIHolder(Farmer farmer, Type type, int page) {
        this.farmer = farmer;
        this.type = type;
        this.page = page;
        this.clickActions = new HashMap<>();
    }

    public Farmer getFarmer() {
        return farmer;
    }

    public Type getType() {
        return type;
    }

    public int getPage() {
        return page;
    }

    public void setAction(int slot, Consumer<InventoryClickEvent> action) {
        clickActions.put(slot, action);
    }

    public void handleClick(InventoryClickEvent event) {
        Consumer<InventoryClickEvent> action = clickActions.get(event.getRawSlot());
        if (action != null) {
            action.accept(event);
        }
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
