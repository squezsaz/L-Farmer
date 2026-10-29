package com.squezsaz.lfarmer.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {

    private final ItemStack itemStack;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.itemStack = new ItemStack(material, amount);
        this.meta = itemStack.getItemMeta();
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        this.meta = this.itemStack.getItemMeta();
    }

    public ItemBuilder name(String name) {
        if (meta != null) {
            meta.displayName(ColorUtil.parse(name));
        }
        return this;
    }

    public ItemBuilder lore(List<String> lore) {
        if (meta != null && lore != null) {
            List<Component> componentLore = new ArrayList<>();
            for (String line : lore) {
                componentLore.add(ColorUtil.parse(line));
            }
            meta.lore(componentLore);
        }
        return this;
    }

    public ItemBuilder addLoreLine(String line) {
        if (meta != null) {
            List<Component> current = meta.lore();
            if (current == null) {
                current = new ArrayList<>();
            }
            current.add(ColorUtil.parse(line));
            meta.lore(current);
        }
        return this;
    }

    public ItemBuilder customModelData(int customModelData) {
        if (meta != null && customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        if (meta != null) {
            meta.addItemFlags(flags);
        }
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        if (meta != null && glow) {
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }
}
