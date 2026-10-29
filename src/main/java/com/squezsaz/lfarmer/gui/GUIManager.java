package com.squezsaz.lfarmer.gui;

import com.squezsaz.lfarmer.LFarmer;
import com.squezsaz.lfarmer.hook.EconomyHook;
import com.squezsaz.lfarmer.model.CropType;
import com.squezsaz.lfarmer.model.Farmer;
import com.squezsaz.lfarmer.model.FarmerLevel;
import com.squezsaz.lfarmer.model.FarmerLogEntry;
import com.squezsaz.lfarmer.model.FarmerPermission;
import com.squezsaz.lfarmer.model.PlayerStats;
import com.squezsaz.lfarmer.model.StatCategory;
import com.squezsaz.lfarmer.util.ColorUtil;
import com.squezsaz.lfarmer.util.ItemBuilder;
import com.squezsaz.lfarmer.util.NumberUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GUIManager {

    private final LFarmer plugin;

    public GUIManager(LFarmer plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player, Farmer farmer) {
        FarmerGUIHolder holder = new FarmerGUIHolder(farmer, FarmerGUIHolder.Type.MAIN, 1);
        Component title = plugin.getLanguageManager().getComponent("gui.main.title", "{owner}", farmer.getOwnerName());
        Inventory inv = Bukkit.createInventory(holder, 45, title);
        holder.setInventory(inv);

        fillBorders(inv, Material.ORANGE_STAINED_GLASS_PANE);

        long stored = farmer.getTotalStored();
        long maxCapacity = (long) plugin.getFarmerManager().getLevelValue("capacity", farmer.getCapacityLevel());

        ItemStack storageItem = new ItemBuilder(Material.BARREL)
                .name(plugin.getLanguageManager().getRaw("gui.main.storage_name"))
                .lore(plugin.getLanguageManager().getStringList(
                        "gui.main.storage_lore",
                        "{stored}", NumberUtil.formatCompact(stored),
                        "{max}", NumberUtil.formatCompact(maxCapacity)
                ))
                .glow(true)
                .build();
        inv.setItem(11, storageItem);
        holder.setAction(11, e -> {
            plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
            openStorageMenu(player, farmer, 1);
        });

        ItemStack upgradeItem = new ItemBuilder(Material.NETHER_STAR)
                .name(plugin.getLanguageManager().getRaw("gui.main.upgrades_name"))
                .lore(plugin.getLanguageManager().getStringList(
                        "gui.main.upgrades_lore",
                        "{capacity_lvl}", String.valueOf(farmer.getCapacityLevel()),
                        "{range_lvl}", String.valueOf(farmer.getRangeLevel()),
                        "{speed_lvl}", String.valueOf(farmer.getSpeedLevel())
                ))
                .glow(true)
                .build();
        inv.setItem(13, upgradeItem);
        holder.setAction(13, e -> {
            if (!farmer.getPermission(player.getUniqueId()).canUpgrade() && !player.hasPermission("farmer.admin")) {
                plugin.getLanguageManager().sendMessage(player, "messages.no_permission");
                return;
            }
            plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
            openUpgradeMenu(player, farmer);
        });

        String statusOn = plugin.getLanguageManager().getRaw("gui.common.status_on");
        String statusOff = plugin.getLanguageManager().getRaw("gui.common.status_off");

        ItemStack settingsItem = new ItemBuilder(Material.COMPARATOR)
                .name(plugin.getLanguageManager().getRaw("gui.main.settings_name"))
                .lore(plugin.getLanguageManager().getStringList(
                        "gui.main.settings_lore",
                        "{autosell_status}", farmer.isAutoSell() ? statusOn : statusOff,
                        "{replant_status}", farmer.isAutoReplant() ? statusOn : statusOff,
                        "{mob_status}", farmer.isMobCollect() ? statusOn : statusOff,
                        "{crop_status}", farmer.isCropCollect() ? statusOn : statusOff,
                        "{ground_status}", farmer.isGroundCollect() ? statusOn : statusOff
                ))
                .build();
        inv.setItem(15, settingsItem);
        holder.setAction(15, e -> {
            plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
            openSettingsMenu(player, farmer);
        });

        int radius = (int) plugin.getFarmerManager().getLevelValue("range", farmer.getRangeLevel());
        int totalChunks = (radius * 2 + 1) * (radius * 2 + 1);

        ItemStack rangeViewItem = new ItemBuilder(Material.SPYGLASS)
                .name(plugin.getLanguageManager().getRaw("gui.main.range_view_name"))
                .lore(plugin.getLanguageManager().getStringList(
                        "gui.main.range_view_lore",
                        "{level}", String.valueOf(farmer.getRangeLevel()),
                        "{radius}", String.valueOf(radius),
                        "{chunks}", String.valueOf(totalChunks)
                ))
                .glow(true)
                .build();
        inv.setItem(22, rangeViewItem);
        holder.setAction(22, e -> {
            player.closeInventory();
            showFarmerRangeParticles(player, farmer);
        });

        ItemStack fuelItem = new ItemBuilder(Material.BLAZE_POWDER)
                .name(plugin.getLanguageManager().getRaw("gui.main.fuel_name"))
                .lore(plugin.getLanguageManager().getStringList(
                        "gui.main.fuel_lore",
                        "{fuel}", NumberUtil.format(farmer.getFuelAmount()),
                        "{max}", NumberUtil.format(plugin.getFuelService().getMaxFuel())
                ))
                .build();
        inv.setItem(29, fuelItem);
        holder.setAction(29, e -> {
            plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
            openFuelMenu(player, farmer);
        });

        ItemStack sellAllItem = new ItemBuilder(Material.GOLD_BLOCK)
                .name(plugin.getLanguageManager().getRaw("gui.main.sell_all_name"))
                .lore(plugin.getLanguageManager().getStringList("gui.main.sell_all_lore"))
                .glow(true)
                .build();
        inv.setItem(31, sellAllItem);
        holder.setAction(31, e -> {
            if (!farmer.getPermission(player.getUniqueId()).canSell() && !player.hasPermission("farmer.admin")) {
                plugin.getLanguageManager().sendMessage(player, "messages.no_permission");
                return;
            }
            sellAllStorage(player, farmer);
            openMainMenu(player, farmer);
        });

        ItemStack membersItem = new ItemBuilder(Material.PLAYER_HEAD)
                .name(plugin.getLanguageManager().getRaw("gui.main.members_name"))
                .lore(plugin.getLanguageManager().getStringList(
                        "gui.main.members_lore",
                        "{count}", String.valueOf(farmer.getMembers().size())
                ))
                .build();
        inv.setItem(33, membersItem);
        holder.setAction(33, e -> {
            if (!farmer.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("farmer.admin")) {
                plugin.getLanguageManager().sendMessage(player, "messages.only_owner");
                return;
            }
            plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
            openMembersMenu(player, farmer, 1);
        });

        ItemStack statsItem = new ItemBuilder(Material.WRITABLE_BOOK)
                .name(plugin.getLanguageManager().getRaw("gui.main.stats_name"))
                .lore(plugin.getLanguageManager().getStringList("gui.main.stats_lore"))
                .build();
        inv.setItem(37, statsItem);
        holder.setAction(37, e -> {
            plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
            openStatsMenu(player, farmer, StatCategory.DAILY);
        });

        ItemStack pickupItem = new ItemBuilder(Material.HOPPER_MINECART)
                .name(plugin.getLanguageManager().getRaw("gui.main.pickup_name"))
                .lore(plugin.getLanguageManager().getStringList("gui.main.pickup_lore"))
                .glow(true)
                .build();
        inv.setItem(43, pickupItem);
        holder.setAction(43, e -> {
            if (!farmer.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("farmer.admin")) {
                plugin.getLanguageManager().sendMessage(player, "messages.only_owner");
                return;
            }
            Location farmerLoc = farmer.getLocation();
            if (farmerLoc == null) {
                farmerLoc = farmer.getLastKnownLocation();
                if (farmerLoc == null) {
                    farmerLoc = player.getLocation();
                }
            }
            player.closeInventory();
            plugin.getFarmerManager().pickupFarmer(farmer, player);
            plugin.getLogService().log(farmer, player.getName(), "PICKUP_GUI", plugin.getLanguageManager().format("logs.details.pickup_gui"), farmerLoc);
            plugin.getLanguageManager().sendMessage(player, "messages.farmer_picked_up");
            plugin.getLanguageManager().playSound(player, "ENTITY_ITEM_PICKUP");
        });

        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name(plugin.getLanguageManager().getRaw("gui.common.close"))
                .build();
        inv.setItem(40, closeItem);
        holder.setAction(40, e -> player.closeInventory());

        player.openInventory(inv);
    }



    public void openStorageMenu(Player player, Farmer farmer, int page) {
        FarmerGUIHolder holder = new FarmerGUIHolder(farmer, FarmerGUIHolder.Type.STORAGE, page);
        Component title = plugin.getLanguageManager().getComponent("gui.storage.title", "{page}", String.valueOf(page));
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        fillBorders(inv, Material.YELLOW_STAINED_GLASS_PANE);

        List<Map.Entry<Material, Long>> storedList = new ArrayList<>(farmer.getStorage().entrySet());
        storedList.removeIf(e -> e.getValue() <= 0);

        int itemsPerPage = 28;
        int totalPages = Math.max(1, (int) Math.ceil((double) storedList.size() / itemsPerPage));
        int currentPage = Math.min(Math.max(1, page), totalPages);

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        int startIndex = (currentPage - 1) * itemsPerPage;
        for (int i = 0; i < itemsPerPage; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex >= storedList.size()) break;

            Map.Entry<Material, Long> entry = storedList.get(itemIndex);
            Material mat = entry.getKey();
            long count = entry.getValue();
            double unitPrice = plugin.getHookManager().getPrice(player, mat);
            double totalPrice = unitPrice * count;

            ItemStack stack = new ItemBuilder(mat)
                    .name("<gold><bold>" + formatMaterialName(mat) + "</bold></gold>")
                    .lore(plugin.getLanguageManager().getStringList(
                            "gui.storage.item_lore",
                            "{count}", NumberUtil.format(count),
                            "{unit_price}", NumberUtil.format(unitPrice),
                            "{total_price}", NumberUtil.format(totalPrice)
                    ))
                    .build();

            int slot = slots[i];
            inv.setItem(slot, stack);
            holder.setAction(slot, e -> {
                ClickType click = e.getClick();
                if (click.isLeftClick()) {
                    if (!farmer.getPermission(player.getUniqueId()).canWithdraw() && !player.hasPermission("farmer.admin")) {
                        plugin.getLanguageManager().sendMessage(player, "messages.no_permission");
                        return;
                    }
                    long toWithdraw = click.isShiftClick() ? count : Math.min(count, 64);
                    withdrawItem(player, farmer, mat, toWithdraw);
                    openStorageMenu(player, farmer, currentPage);
                } else if (click.isRightClick()) {
                    if (!farmer.getPermission(player.getUniqueId()).canSell() && !player.hasPermission("farmer.admin")) {
                        plugin.getLanguageManager().sendMessage(player, "messages.no_permission");
                        return;
                    }
                    sellItem(player, farmer, mat, count);
                    openStorageMenu(player, farmer, currentPage);
                }
            });
        }

        if (currentPage > 1) {
            ItemStack prev = new ItemBuilder(Material.ARROW)
                    .name(plugin.getLanguageManager().getRaw("gui.common.previous_page"))
                    .build();
            inv.setItem(48, prev);
            holder.setAction(48, e -> openStorageMenu(player, farmer, currentPage - 1));
        }

        ItemStack back = new ItemBuilder(Material.OAK_DOOR)
                .name(plugin.getLanguageManager().getRaw("gui.common.back"))
                .build();
        inv.setItem(49, back);
        holder.setAction(49, e -> openMainMenu(player, farmer));

        if (currentPage < totalPages) {
            ItemStack next = new ItemBuilder(Material.ARROW)
                    .name(plugin.getLanguageManager().getRaw("gui.common.next_page"))
                    .build();
            inv.setItem(50, next);
            holder.setAction(50, e -> openStorageMenu(player, farmer, currentPage + 1));
        }

        ItemStack sellAll = new ItemBuilder(Material.GOLD_INGOT)
                .name(plugin.getLanguageManager().getRaw("gui.storage.sell_all_button"))
                .lore(plugin.getLanguageManager().getStringList("gui.storage.sell_all_button_lore"))
                .glow(true)
                .build();
        inv.setItem(53, sellAll);
        holder.setAction(53, e -> {
            if (!farmer.getPermission(player.getUniqueId()).canSell() && !player.hasPermission("farmer.admin")) {
                plugin.getLanguageManager().sendMessage(player, "messages.no_permission");
                return;
            }
            sellAllStorage(player, farmer);
            openStorageMenu(player, farmer, 1);
        });

        player.openInventory(inv);
    }



    public void openUpgradeMenu(Player player, Farmer farmer) {
        FarmerGUIHolder holder = new FarmerGUIHolder(farmer, FarmerGUIHolder.Type.UPGRADES, 1);
        Component title = plugin.getLanguageManager().getComponent("gui.upgrades.title");
        Inventory inv = Bukkit.createInventory(holder, 45, title);
        holder.setInventory(inv);

        fillBorders(inv, Material.ORANGE_STAINED_GLASS_PANE);

        setupUpgradeButton(player, holder, inv, 11, "capacity", farmer.getCapacityLevel(), Material.CHEST, "gui.upgrades.capacity_name");
        setupUpgradeButton(player, holder, inv, 13, "range", farmer.getRangeLevel(), Material.COMPASS, "gui.upgrades.range_name");
        setupUpgradeButton(player, holder, inv, 15, "speed", farmer.getSpeedLevel(), Material.FEATHER, "gui.upgrades.speed_name");
        setupUpgradeButton(player, holder, inv, 29, "fortune", farmer.getFortuneLevel(), Material.EMERALD, "gui.upgrades.fortune_name");
        setupUpgradeButton(player, holder, inv, 33, "tax", farmer.getTaxLevel(), Material.GOLD_NUGGET, "gui.upgrades.tax_name");

        ItemStack back = new ItemBuilder(Material.OAK_DOOR)
                .name(plugin.getLanguageManager().getRaw("gui.common.back"))
                .build();
        inv.setItem(40, back);
        holder.setAction(40, e -> openMainMenu(player, farmer));

        player.openInventory(inv);
    }



    private void setupUpgradeButton(Player player, FarmerGUIHolder holder, Inventory inv, int slot, String type, int currentLevel, Material mat, String titleKey) {
        Farmer farmer = holder.getFarmer();
        int max = plugin.getFarmerManager().getMaxLevel(type);
        boolean isMax = currentLevel >= max;
        FarmerLevel nextData = isMax ? null : plugin.getFarmerManager().getLevelData(type, currentLevel + 1);
        double cost = (nextData != null) ? nextData.getCost() : 0.0;
        double currentVal = plugin.getFarmerManager().getLevelValue(type, currentLevel);
        double nextVal = (nextData != null) ? nextData.getValue() : currentVal;

        List<String> lore = isMax
                ? plugin.getLanguageManager().getStringList("gui.upgrades.lore_max", "{level}", String.valueOf(currentLevel), "{val}", NumberUtil.format(currentVal))
                : plugin.getLanguageManager().getStringList(
                "gui.upgrades.lore_upgrade",
                "{level}", String.valueOf(currentLevel),
                "{next_level}", String.valueOf(currentLevel + 1),
                "{val}", NumberUtil.format(currentVal),
                "{next_val}", NumberUtil.format(nextVal),
                "{cost}", NumberUtil.format(cost)
        );

        ItemStack stack = new ItemBuilder(mat)
                .name(plugin.getLanguageManager().getRaw(titleKey))
                .lore(lore)
                .glow(!isMax)
                .build();

        inv.setItem(slot, stack);
        holder.setAction(slot, e -> {
            if (isMax) {
                plugin.getLanguageManager().playSound(player, "ENTITY_VILLAGER_NO");
                return;
            }

            EconomyHook eco = plugin.getHookManager().getEconomyHook();
            if (eco != null && eco.isAvailable()) {
                if (!eco.has(player, cost)) {
                    plugin.getLanguageManager().sendMessage(player, "messages.not_enough_money", "{cost}", NumberUtil.format(cost));
                    plugin.getLanguageManager().playSound(player, "ENTITY_VILLAGER_NO");
                    return;
                }
                eco.withdraw(player, cost);
            }

            if (type.equalsIgnoreCase("capacity")) farmer.setCapacityLevel(currentLevel + 1);
            else if (type.equalsIgnoreCase("range")) farmer.setRangeLevel(currentLevel + 1);
            else if (type.equalsIgnoreCase("speed")) farmer.setSpeedLevel(currentLevel + 1);
            else if (type.equalsIgnoreCase("fortune")) farmer.setFortuneLevel(currentLevel + 1);
            else if (type.equalsIgnoreCase("tax")) farmer.setTaxLevel(currentLevel + 1);

            plugin.getNpcManager().updateHologram(farmer.getId());
            String typeName = plugin.getLanguageManager().getRaw("gui.upgrades." + type.toLowerCase() + "_name");
            plugin.getLogService().log(farmer, player.getName(), "UPGRADE", plugin.getLanguageManager().format("logs.details.upgrade", "{type}", typeName, "{level}", String.valueOf(currentLevel + 1), "{cost}", NumberUtil.format(cost)));
            plugin.getLanguageManager().playSound(player, "ENTITY_PLAYER_LEVELUP");
            plugin.getLanguageManager().sendMessage(player, "messages.upgrade_success", "{type}", type, "{level}", String.valueOf(currentLevel + 1));
            openUpgradeMenu(player, farmer);
        });
    }

    private static final List<Material> COLLECTABLE_MATERIALS = Arrays.asList(
            Material.WHEAT, Material.WHEAT_SEEDS, Material.CARROT, Material.POTATO,
            Material.BEETROOT, Material.BEETROOT_SEEDS, Material.NETHER_WART, Material.COCOA_BEANS,
            Material.SUGAR_CANE, Material.CACTUS, Material.MELON_SLICE, Material.MELON,
            Material.PUMPKIN, Material.SWEET_BERRIES, Material.BAMBOO,
            Material.ROTTEN_FLESH, Material.BONE, Material.STRING, Material.SPIDER_EYE,
            Material.GUNPOWDER, Material.ENDER_PEARL, Material.BLAZE_ROD, Material.GHAST_TEAR,
            Material.MAGMA_CREAM, Material.SLIME_BALL, Material.LEATHER, Material.BEEF,
            Material.PORKCHOP, Material.CHICKEN, Material.MUTTON, Material.FEATHER,
            Material.EGG, Material.WHITE_WOOL, Material.IRON_INGOT, Material.POPPY
    );

    public void openSettingsMenu(Player player, Farmer farmer) {
        FarmerGUIHolder holder = new FarmerGUIHolder(farmer, FarmerGUIHolder.Type.SETTINGS, 1);
        Component title = plugin.getLanguageManager().getComponent("gui.settings.title");
        Inventory inv = Bukkit.createInventory(holder, 45, title);
        holder.setInventory(inv);

        fillBorders(inv, Material.GRAY_STAINED_GLASS_PANE);

        String statusOn = plugin.getLanguageManager().getRaw("gui.common.status_on");
        String statusOff = plugin.getLanguageManager().getRaw("gui.common.status_off");

        String autoSellStatus = farmer.isAutoSell() ? statusOn : statusOff;
        ItemStack autoSellItem = new ItemBuilder(Material.GOLD_INGOT)
                .name(plugin.getLanguageManager().getRaw("gui.settings.autosell_name"))
                .lore(plugin.getLanguageManager().getStringList(
                        "gui.settings.autosell_lore",
                        "{status}", autoSellStatus,
                        "{interval}", String.valueOf(farmer.getAutoSellInterval())
                ))
                .glow(farmer.isAutoSell())
                .build();
        inv.setItem(11, autoSellItem);
        holder.setAction(11, e -> {
            if (e.getClick().isRightClick()) {
                int[] intervals = {30, 60, 120, 300, 600};
                int current = farmer.getAutoSellInterval();
                int next = intervals[0];
                for (int i = 0; i < intervals.length; i++) {
                    if (intervals[i] == current) {
                        next = intervals[(i + 1) % intervals.length];
                        break;
                    }
                }
                farmer.setAutoSellInterval(next);
                plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
                plugin.getLanguageManager().sendMessage(player, "messages.autosell_interval_changed", "{interval}", String.valueOf(next));
            } else {
                farmer.setAutoSell(!farmer.isAutoSell());
                plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
            }
            openSettingsMenu(player, farmer);
        });

        String replantStatus = farmer.isAutoReplant() ? statusOn : statusOff;
        ItemStack replantItem = new ItemBuilder(Material.WHEAT_SEEDS)
                .name(plugin.getLanguageManager().getRaw("gui.settings.autoreplant_name"))
                .lore(plugin.getLanguageManager().getStringList("gui.settings.autoreplant_lore", "{status}", replantStatus))
                .glow(farmer.isAutoReplant())
                .build();
        inv.setItem(13, replantItem);
        holder.setAction(13, e -> {
            farmer.setAutoReplant(!farmer.isAutoReplant());
            plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
            openSettingsMenu(player, farmer);
        });

        String mobStatus = farmer.isMobCollect() ? statusOn : statusOff;
        ItemStack mobItem = new ItemBuilder(Material.ROTTEN_FLESH)
                .name(plugin.getLanguageManager().getRaw("gui.settings.mobcollect_name"))
                .lore(plugin.getLanguageManager().getStringList("gui.settings.mobcollect_lore", "{status}", mobStatus))
                .glow(farmer.isMobCollect())
                .build();
        inv.setItem(15, mobItem);
        holder.setAction(15, e -> {
            farmer.setMobCollect(!farmer.isMobCollect());
            plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
            openSettingsMenu(player, farmer);
        });

        String cropStatus = farmer.isCropCollect() ? statusOn : statusOff;
        ItemStack cropItem = new ItemBuilder(Material.IRON_HOE)
                .name(plugin.getLanguageManager().getRaw("gui.settings.cropharvest_name"))
                .lore(plugin.getLanguageManager().getStringList("gui.settings.cropharvest_lore", "{status}", cropStatus))
                .glow(farmer.isCropCollect())
                .build();
        inv.setItem(29, cropItem);
        holder.setAction(29, e -> {
            farmer.setCropCollect(!farmer.isCropCollect());
            plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
            openSettingsMenu(player, farmer);
        });

        String groundStatus = farmer.isGroundCollect() ? statusOn : statusOff;
        ItemStack groundItem = new ItemBuilder(Material.HOPPER_MINECART)
                .name(plugin.getLanguageManager().getRaw("gui.settings.groundcollect_name"))
                .lore(plugin.getLanguageManager().getStringList("gui.settings.groundcollect_lore", "{status}", groundStatus))
                .glow(farmer.isGroundCollect())
                .build();
        inv.setItem(31, groundItem);
        holder.setAction(31, e -> {
            farmer.setGroundCollect(!farmer.isGroundCollect());
            plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
            openSettingsMenu(player, farmer);
        });

        ItemStack filterItem = new ItemBuilder(Material.HOPPER)
                .name(plugin.getLanguageManager().getRaw("gui.settings.filter_name"))
                .lore(plugin.getLanguageManager().getStringList("gui.settings.filter_lore"))
                .glow(true)
                .build();
        inv.setItem(33, filterItem);
        holder.setAction(33, e -> {
            plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
            openItemFilterMenu(player, farmer, 1);
        });

        ItemStack back = new ItemBuilder(Material.OAK_DOOR)
                .name(plugin.getLanguageManager().getRaw("gui.common.back"))
                .build();
        inv.setItem(40, back);
        holder.setAction(40, e -> openMainMenu(player, farmer));

        player.openInventory(inv);
    }



    public void openItemFilterMenu(Player player, Farmer farmer, int page) {
        int itemsPerPage = 28;
        int totalPages = Math.max(1, (int) Math.ceil((double) COLLECTABLE_MATERIALS.size() / itemsPerPage));
        int currentPage = Math.min(Math.max(1, page), totalPages);

        FarmerGUIHolder holder = new FarmerGUIHolder(farmer, FarmerGUIHolder.Type.ITEM_FILTER, currentPage);
        Component title = plugin.getLanguageManager().getComponent("gui.filter.title", "{page}", String.valueOf(currentPage));
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        fillBorders(inv, Material.ORANGE_STAINED_GLASS_PANE);

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        String statusCollecting = plugin.getLanguageManager().getRaw("gui.common.status_collecting");
        String statusNotCollecting = plugin.getLanguageManager().getRaw("gui.common.status_not_collecting");

        int startIndex = (currentPage - 1) * itemsPerPage;
        for (int i = 0; i < itemsPerPage; i++) {
            int index = startIndex + i;
            if (index >= COLLECTABLE_MATERIALS.size()) break;

            Material mat = COLLECTABLE_MATERIALS.get(index);
            boolean allowed = farmer.isItemAllowed(mat);
            String status = allowed ? statusCollecting : statusNotCollecting;

            ItemStack stack = new ItemBuilder(mat)
                    .name("<gold><bold>" + formatMaterialName(mat) + "</bold></gold>")
                    .lore(plugin.getLanguageManager().getStringList("gui.filter.item_lore", "{status}", status))
                    .glow(allowed)
                    .build();

            int slot = slots[i];
            inv.setItem(slot, stack);
            holder.setAction(slot, e -> {
                farmer.toggleItemAllowed(mat);
                plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
                openItemFilterMenu(player, farmer, currentPage);
            });
        }

        ItemStack enableAll = new ItemBuilder(Material.LIME_DYE)
                .name(plugin.getLanguageManager().getRaw("gui.filter.enable_all_name"))
                .lore(plugin.getLanguageManager().getStringList("gui.filter.enable_all_lore"))
                .build();
        inv.setItem(46, enableAll);
        holder.setAction(46, e -> {
            farmer.getDisabledItems().clear();
            farmer.setDirty(true);
            plugin.getLanguageManager().playSound(player, "ENTITY_EXPERIENCE_ORB_PICKUP");
            openItemFilterMenu(player, farmer, currentPage);
        });

        if (currentPage > 1) {
            ItemStack prev = new ItemBuilder(Material.ARROW)
                    .name(plugin.getLanguageManager().getRaw("gui.common.previous_page"))
                    .build();
            inv.setItem(48, prev);
            holder.setAction(48, e -> openItemFilterMenu(player, farmer, currentPage - 1));
        }

        ItemStack back = new ItemBuilder(Material.OAK_DOOR)
                .name(plugin.getLanguageManager().getRaw("gui.common.back"))
                .build();
        inv.setItem(49, back);
        holder.setAction(49, e -> openSettingsMenu(player, farmer));

        if (currentPage < totalPages) {
            ItemStack next = new ItemBuilder(Material.ARROW)
                    .name(plugin.getLanguageManager().getRaw("gui.common.next_page"))
                    .build();
            inv.setItem(50, next);
            holder.setAction(50, e -> openItemFilterMenu(player, farmer, currentPage + 1));
        }

        ItemStack disableAll = new ItemBuilder(Material.GRAY_DYE)
                .name(plugin.getLanguageManager().getRaw("gui.filter.disable_all_name"))
                .lore(plugin.getLanguageManager().getStringList("gui.filter.disable_all_lore"))
                .build();
        inv.setItem(52, disableAll);
        holder.setAction(52, e -> {
            farmer.getDisabledItems().addAll(COLLECTABLE_MATERIALS);
            farmer.setDirty(true);
            plugin.getLanguageManager().playSound(player, "ENTITY_ITEM_BREAK");
            openItemFilterMenu(player, farmer, currentPage);
        });

        player.openInventory(inv);
    }



    public void openFuelMenu(Player player, Farmer farmer) {
        FarmerGUIHolder holder = new FarmerGUIHolder(farmer, FarmerGUIHolder.Type.FUEL, 1);
        Component title = plugin.getLanguageManager().getComponent("gui.fuel.title");
        Inventory inv = Bukkit.createInventory(holder, 45, title);
        holder.setInventory(inv);

        fillBorders(inv, Material.BLACK_STAINED_GLASS_PANE);

        double fuel = farmer.getFuelAmount();
        double maxFuel = plugin.getFuelService().getMaxFuel();
        double percent = (fuel / maxFuel) * 100.0;

        ItemStack gauge = new ItemBuilder(Material.BLAZE_POWDER)
                .name(plugin.getLanguageManager().getRaw("gui.fuel.gauge_name"))
                .lore(plugin.getLanguageManager().getStringList(
                        "gui.fuel.gauge_lore",
                        "{fuel}", NumberUtil.format(fuel),
                        "{max}", NumberUtil.format(maxFuel),
                        "{percent}", String.format("%.1f", percent)
                ))
                .glow(true)
                .build();
        inv.setItem(13, gauge);

        ItemStack addAll = new ItemBuilder(Material.COAL_BLOCK)
                .name(plugin.getLanguageManager().getRaw("gui.fuel.add_all_name"))
                .lore(plugin.getLanguageManager().getStringList("gui.fuel.add_all_lore"))
                .build();
        inv.setItem(21, addAll);
        holder.setAction(21, e -> {
            addFuelFromInventory(player, farmer);
            openFuelMenu(player, farmer);
        });

        ItemStack paySalary = new ItemBuilder(Material.GOLD_INGOT)
                .name(plugin.getLanguageManager().getRaw("gui.fuel.salary_name"))
                .lore(plugin.getLanguageManager().getStringList(
                        "gui.fuel.salary_lore",
                        "{cost}", NumberUtil.format(plugin.getFuelService().getSalaryCost()),
                        "{fuel}", NumberUtil.format(plugin.getFuelService().getSalaryFuelAmount())
                ))
                .build();
        inv.setItem(23, paySalary);
        holder.setAction(23, e -> {
            paySalaryAction(player, farmer);
            openFuelMenu(player, farmer);
        });

        ItemStack back = new ItemBuilder(Material.OAK_DOOR)
                .name(plugin.getLanguageManager().getRaw("gui.common.back"))
                .build();
        inv.setItem(40, back);
        holder.setAction(40, e -> openMainMenu(player, farmer));

        player.openInventory(inv);
    }



    private void addFuelFromInventory(Player player, Farmer farmer) {
        double maxFuel = plugin.getFuelService().getMaxFuel();
        double current = farmer.getFuelAmount();
        if (current >= maxFuel) {
            plugin.getLanguageManager().sendMessage(player, "messages.fuel_full");
            plugin.getLanguageManager().playSound(player, "ENTITY_VILLAGER_NO");
            return;
        }

        double added = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && plugin.getFuelService().isFuelItem(item.getType())) {
                double valPerItem = plugin.getFuelService().getFuelValue(item.getType());
                int amount = item.getAmount();
                for (int i = 0; i < amount; i++) {
                    if (farmer.getFuelAmount() + valPerItem <= maxFuel) {
                        farmer.addFuel(valPerItem, maxFuel);
                        added += valPerItem;
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        break;
                    }
                }
            }
        }

        if (added > 0) {
            plugin.getLogService().log(farmer, player.getName(), "FUEL_ADD", plugin.getLanguageManager().format("logs.details.fuel_add", "{amount}", NumberUtil.format(added)));
            plugin.getLanguageManager().playSound(player, "ITEM_FIRECHARGE_USE");
            plugin.getLanguageManager().sendMessage(player, "messages.fuel_added", "{amount}", NumberUtil.format(added));
        } else {
            plugin.getLanguageManager().sendMessage(player, "messages.no_fuel_in_inventory");
        }
    }

    private void paySalaryAction(Player player, Farmer farmer) {
        double maxFuel = plugin.getFuelService().getMaxFuel();
        if (farmer.getFuelAmount() >= maxFuel) {
            plugin.getLanguageManager().sendMessage(player, "messages.fuel_full");
            plugin.getLanguageManager().playSound(player, "ENTITY_VILLAGER_NO");
            return;
        }

        double cost = plugin.getFuelService().getSalaryCost();
        double fuelReward = plugin.getFuelService().getSalaryFuelAmount();

        EconomyHook eco = plugin.getHookManager().getEconomyHook();
        if (eco == null || !eco.isAvailable()) {
            return;
        }

        if (!eco.has(player, cost)) {
            plugin.getLanguageManager().sendMessage(player, "messages.not_enough_money", "{cost}", NumberUtil.format(cost));
            plugin.getLanguageManager().playSound(player, "ENTITY_VILLAGER_NO");
            return;
        }

        double availableSpace = maxFuel - farmer.getFuelAmount();
        double toAdd = Math.min(fuelReward, availableSpace);

        eco.withdraw(player, cost);
        farmer.addFuel(toAdd, maxFuel);
        plugin.getLogService().log(farmer, player.getName(), "SALARY_PAID", plugin.getLanguageManager().format("logs.details.salary_paid", "{cost}", NumberUtil.format(cost), "{amount}", NumberUtil.format(toAdd)));
        plugin.getLanguageManager().playSound(player, "ENTITY_EXPERIENCE_ORB_PICKUP");
        plugin.getLanguageManager().sendMessage(player, "messages.salary_paid", "{cost}", NumberUtil.format(cost), "{fuel}", NumberUtil.format(toAdd));
    }

    public void openMembersMenu(Player player, Farmer farmer, int page) {
        FarmerGUIHolder holder = new FarmerGUIHolder(farmer, FarmerGUIHolder.Type.MEMBERS, page);
        Component title = plugin.getLanguageManager().getComponent("gui.members.title");
        Inventory inv = Bukkit.createInventory(holder, 45, title);
        holder.setInventory(inv);

        fillBorders(inv, Material.BROWN_STAINED_GLASS_PANE);

        List<UUID> memberList = new ArrayList<>(farmer.getMembers().keySet());
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

        for (int i = 0; i < Math.min(memberList.size(), slots.length); i++) {
            UUID memberUuid = memberList.get(i);
            OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(memberUuid);
            FarmerPermission perm = farmer.getPermission(memberUuid);

            ItemStack head = new ItemBuilder(Material.PLAYER_HEAD)
                    .name("<yellow>" + (offPlayer.getName() != null ? offPlayer.getName() : memberUuid.toString()) + "</yellow>")
                    .lore(plugin.getLanguageManager().getStringList(
                            "gui.members.member_lore",
                            "{can_open}", perm.canOpen() ? "<green>✔</green>" : "<red>✖</red>",
                            "{can_withdraw}", perm.canWithdraw() ? "<green>✔</green>" : "<red>✖</red>",
                            "{can_sell}", perm.canSell() ? "<green>✔</green>" : "<red>✖</red>",
                            "{can_upgrade}", perm.canUpgrade() ? "<green>✔</green>" : "<red>✖</red>"
                    ))
                    .build();

            int slot = slots[i];
            inv.setItem(slot, head);
            holder.setAction(slot, e -> {
                ClickType click = e.getClick();
                if (click.isShiftClick() && click.isRightClick()) {
                    farmer.getMembers().remove(memberUuid);
                    farmer.setDirty(true);
                    String targetName = Bukkit.getOfflinePlayer(memberUuid).getName();
                    if (targetName == null) targetName = memberUuid.toString();
                    plugin.getLogService().log(farmer, player.getName(), "MEMBER_REMOVE", plugin.getLanguageManager().format("logs.details.member_remove", "{player}", targetName));
                    plugin.getLanguageManager().playSound(player, "ENTITY_ITEM_BREAK");
                    openMembersMenu(player, farmer, page);
                } else if (click.isLeftClick()) {
                    perm.setCanWithdraw(!perm.canWithdraw());
                    farmer.setDirty(true);
                    plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
                    openMembersMenu(player, farmer, page);
                } else if (click.isRightClick()) {
                    perm.setCanSell(!perm.canSell());
                    farmer.setDirty(true);
                    plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
                    openMembersMenu(player, farmer, page);
                }
            });
        }

        ItemStack addMember = new ItemBuilder(Material.EMERALD)
                .name(plugin.getLanguageManager().getRaw("gui.members.add_member_name"))
                .lore(plugin.getLanguageManager().getStringList("gui.members.add_member_lore"))
                .glow(true)
                .build();
        inv.setItem(31, addMember);
        holder.setAction(31, e -> {
            plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
            openAddMemberMenu(player, farmer, 1);
        });

        ItemStack back = new ItemBuilder(Material.OAK_DOOR)
                .name(plugin.getLanguageManager().getRaw("gui.common.back"))
                .build();
        inv.setItem(40, back);
        holder.setAction(40, e -> openMainMenu(player, farmer));

        player.openInventory(inv);
    }



    public void openAddMemberMenu(Player player, Farmer farmer, int page) {
        List<Player> eligiblePlayers = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(farmer.getOwnerUuid()) && !farmer.getMembers().containsKey(online.getUniqueId())) {
                eligiblePlayers.add(online);
            }
        }

        int itemsPerPage = 28;
        int totalPages = Math.max(1, (int) Math.ceil((double) eligiblePlayers.size() / itemsPerPage));
        int currentPage = Math.min(Math.max(1, page), totalPages);

        FarmerGUIHolder holder = new FarmerGUIHolder(farmer, FarmerGUIHolder.Type.ADD_MEMBER, currentPage);
        Component title = plugin.getLanguageManager().getComponent("gui.members.add_title", "{page}", String.valueOf(currentPage));
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        fillBorders(inv, Material.LIME_STAINED_GLASS_PANE);

        if (eligiblePlayers.isEmpty()) {
            ItemStack noPlayer = new ItemBuilder(Material.BARRIER)
                    .name(plugin.getLanguageManager().getRaw("gui.members.no_eligible_name"))
                    .lore(plugin.getLanguageManager().getStringList("gui.members.no_eligible_lore"))
                    .build();
            inv.setItem(22, noPlayer);
        } else {
            int[] slots = {
                    10, 11, 12, 13, 14, 15, 16,
                    19, 20, 21, 22, 23, 24, 25,
                    28, 29, 30, 31, 32, 33, 34,
                    37, 38, 39, 40, 41, 42, 43
            };

            int startIndex = (currentPage - 1) * itemsPerPage;
            for (int i = 0; i < itemsPerPage; i++) {
                int index = startIndex + i;
                if (index >= eligiblePlayers.size()) break;

                Player target = eligiblePlayers.get(index);
                ItemStack skull = new ItemBuilder(Material.PLAYER_HEAD)
                        .name("<yellow><bold>" + target.getName() + "</bold></yellow>")
                        .lore(plugin.getLanguageManager().getStringList("gui.members.candidate_lore", "{player}", target.getName()))
                        .build();

                int slot = slots[i];
                inv.setItem(slot, skull);
                holder.setAction(slot, e -> {
                    farmer.getMembers().put(target.getUniqueId(), new FarmerPermission(true, false, false, false));
                    farmer.setDirty(true);
                    plugin.getLogService().log(farmer, player.getName(), "MEMBER_ADD", plugin.getLanguageManager().format("logs.details.member_add", "{player}", target.getName()));
                    plugin.getLanguageManager().playSound(player, "ENTITY_PLAYER_LEVELUP");
                    plugin.getLanguageManager().sendMessage(player, "messages.member_added", "{player}", target.getName());
                    openMembersMenu(player, farmer, 1);
                });
            }
        }

        if (currentPage > 1) {
            ItemStack prev = new ItemBuilder(Material.ARROW)
                    .name(plugin.getLanguageManager().getRaw("gui.common.previous_page"))
                    .build();
            inv.setItem(48, prev);
            holder.setAction(48, e -> openAddMemberMenu(player, farmer, currentPage - 1));
        }

        ItemStack back = new ItemBuilder(Material.OAK_DOOR)
                .name(plugin.getLanguageManager().getRaw("gui.common.back"))
                .build();
        inv.setItem(49, back);
        holder.setAction(49, e -> openMembersMenu(player, farmer, 1));

        if (currentPage < totalPages) {
            ItemStack next = new ItemBuilder(Material.ARROW)
                    .name(plugin.getLanguageManager().getRaw("gui.common.next_page"))
                    .build();
            inv.setItem(50, next);
            holder.setAction(50, e -> openAddMemberMenu(player, farmer, currentPage + 1));
        }

        player.openInventory(inv);
    }



    private void withdrawItem(Player player, Farmer farmer, Material mat, long amount) {
        long withdrawn = farmer.withdrawStorage(mat, amount);
        if (withdrawn <= 0) return;

        ItemStack stack = new ItemStack(mat, (int) withdrawn);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        for (ItemStack left : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), left);
        }

        plugin.getLanguageManager().playSound(player, "ENTITY_ITEM_PICKUP");
        plugin.getLogService().log(farmer, player.getName(), "WITHDRAW", plugin.getLanguageManager().format("logs.details.withdraw", "{amount}", String.valueOf(withdrawn), "{item}", formatMaterialName(mat)));
        plugin.getNpcManager().updateHologram(farmer.getId());
    }

    private void sellItem(Player player, Farmer farmer, Material mat, long amount) {
        double unitPrice = plugin.getHookManager().getPrice(player, mat);
        if (unitPrice <= 0) {
            plugin.getLanguageManager().sendMessage(player, "messages.item_not_sellable");
            return;
        }

        long soldAmount = farmer.withdrawStorage(mat, amount);
        if (soldAmount <= 0) return;

        double gross = unitPrice * soldAmount;
        double taxRate = plugin.getFarmerManager().getLevelValue("tax", farmer.getTaxLevel());
        double tax = gross * (taxRate / 100.0);
        double net = gross - tax;

        EconomyHook eco = plugin.getHookManager().getEconomyHook();
        if (eco != null && eco.isAvailable()) {
            eco.deposit(player, net);
        }

        farmer.addEarned(net);
        plugin.getStatsService().recordSale(farmer.getOwnerUuid(), soldAmount, net);
        plugin.getLogService().log(farmer, player.getName(), "SELL_ITEM", plugin.getLanguageManager().format("logs.details.sell_item", "{amount}", String.valueOf(soldAmount), "{item}", formatMaterialName(mat), "{net}", NumberUtil.format(net)));
        plugin.getLanguageManager().playSound(player, "ENTITY_EXPERIENCE_ORB_PICKUP");
        plugin.getLanguageManager().sendMessage(
                player,
                "messages.sold_item",
                "{amount}", NumberUtil.format(soldAmount),
                "{item}", formatMaterialName(mat),
                "{net}", NumberUtil.format(net),
                "{tax}", NumberUtil.format(tax)
        );
        plugin.getNpcManager().updateHologram(farmer.getId());
    }

    private void sellAllStorage(Player player, Farmer farmer) {
        EconomyHook eco = plugin.getHookManager().getEconomyHook();
        if (eco == null || !eco.isAvailable()) return;

        double totalGross = 0;
        long totalSold = 0;

        for (Map.Entry<Material, Long> entry : new ArrayList<>(farmer.getStorage().entrySet())) {
            Material mat = entry.getKey();
            long amount = entry.getValue();
            if (amount <= 0) continue;

            double price = plugin.getHookManager().getPrice(player, mat);
            if (price > 0) {
                farmer.withdrawStorage(mat, amount);
                totalGross += price * amount;
                totalSold += amount;
            }
        }

        if (totalSold > 0) {
            double taxRate = plugin.getFarmerManager().getLevelValue("tax", farmer.getTaxLevel());
            double tax = totalGross * (taxRate / 100.0);
            double net = totalGross - tax;

            eco.deposit(player, net);
            farmer.addEarned(net);
            plugin.getStatsService().recordSale(farmer.getOwnerUuid(), totalSold, net);
            plugin.getLogService().log(farmer, player.getName(), "SELL_ALL", plugin.getLanguageManager().format("logs.details.sell_all", "{amount}", String.valueOf(totalSold), "{net}", NumberUtil.format(net)));

            plugin.getLanguageManager().playSound(player, "ENTITY_PLAYER_LEVELUP");
            plugin.getLanguageManager().sendMessage(
                    player,
                    "messages.sold_all",
                    "{amount}", NumberUtil.format(totalSold),
                    "{net}", NumberUtil.format(net),
                    "{tax}", NumberUtil.format(tax)
            );
            plugin.getNpcManager().updateHologram(farmer.getId());
        } else {
            plugin.getLanguageManager().sendMessage(player, "messages.nothing_to_sell");
        }
    }

    public void openAdminFarmersMenu(Player player, int page) {
        openAdminFarmersMenu(player, null, page);
    }

    public void openAdminFarmersMenu(Player player, String targetPlayerFilter, int page) {
        List<Farmer> allFarmers = new ArrayList<>(plugin.getFarmerManager().getAllFarmers().values());
        if (targetPlayerFilter != null && !targetPlayerFilter.isEmpty()) {
            allFarmers.removeIf(f -> !f.getOwnerName().equalsIgnoreCase(targetPlayerFilter));
        }

        int itemsPerPage = 28;
        int totalPages = Math.max(1, (int) Math.ceil((double) allFarmers.size() / itemsPerPage));
        int currentPage = Math.min(Math.max(1, page), totalPages);

        FarmerGUIHolder holder = new FarmerGUIHolder(null, FarmerGUIHolder.Type.ADMIN_FARMERS, currentPage);
        Component title = (targetPlayerFilter != null && !targetPlayerFilter.isEmpty())
                ? plugin.getLanguageManager().getComponent("gui.admin.player_title", "{player}", targetPlayerFilter, "{page}", String.valueOf(currentPage), "{total_pages}", String.valueOf(totalPages))
                : plugin.getLanguageManager().getComponent("gui.admin.title", "{page}", String.valueOf(currentPage), "{total_pages}", String.valueOf(totalPages));
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        fillBorders(inv, Material.PURPLE_STAINED_GLASS_PANE);

        long globalStored = 0;
        double globalEarned = 0;
        for (Farmer f : allFarmers) {
            globalStored += f.getTotalStored();
            globalEarned += f.getTotalEarned();
        }

        String serverName = plugin.getConfig().getString("server_name", "Survival-1");

        ItemStack infoItem = new ItemBuilder(Material.NETHER_STAR)
                .name(plugin.getLanguageManager().getRaw("gui.admin.info_name"))
                .lore(plugin.getLanguageManager().getStringList(
                        "gui.admin.info_lore",
                        "{count}", String.valueOf(allFarmers.size()),
                        "{stored}", NumberUtil.format(globalStored),
                        "{earned}", NumberUtil.format(globalEarned),
                        "{server}", serverName
                ))
                .glow(true)
                .build();
        inv.setItem(4, infoItem);

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        int startIndex = (currentPage - 1) * itemsPerPage;
        String statusOn = plugin.getLanguageManager().getRaw("gui.common.status_on");
        String statusOff = plugin.getLanguageManager().getRaw("gui.common.status_off");
        for (int i = 0; i < itemsPerPage; i++) {
            int index = startIndex + i;
            if (index >= allFarmers.size()) break;

            Farmer targetFarmer = allFarmers.get(index);
            Location loc = targetFarmer.getLocation();
            String world = targetFarmer.getWorld();
            int x = loc != null ? loc.getBlockX() : 0;
            int y = loc != null ? loc.getBlockY() : 0;
            int z = loc != null ? loc.getBlockZ() : 0;
            long maxCap = (long) plugin.getFarmerManager().getLevelValue("capacity", targetFarmer.getCapacityLevel());

            ItemStack farmerItem = new ItemBuilder(Material.PLAYER_HEAD)
                    .name(plugin.getLanguageManager().format("gui.admin.farmer_name", "{owner}", targetFarmer.getOwnerName()))
                    .lore(plugin.getLanguageManager().getStringList(
                            "gui.admin.farmer_lore",
                            "{server}", targetFarmer.getServerName() != null ? targetFarmer.getServerName() : serverName,
                            "{world}", world,
                            "{x}", String.valueOf(x),
                            "{y}", String.valueOf(y),
                            "{z}", String.valueOf(z),
                            "{chunk_x}", String.valueOf(targetFarmer.getChunkX()),
                            "{chunk_z}", String.valueOf(targetFarmer.getChunkZ()),
                            "{stored}", NumberUtil.formatCompact(targetFarmer.getTotalStored()),
                            "{max}", NumberUtil.formatCompact(maxCap),
                            "{fuel}", NumberUtil.format(targetFarmer.getFuelAmount()),
                            "{autosell}", targetFarmer.isAutoSell() ? statusOn : statusOff,
                            "{earned}", NumberUtil.format(targetFarmer.getTotalEarned())
                    ))
                    .build();

            int slot = slots[i];
            inv.setItem(slot, farmerItem);

            holder.setAction(slot, e -> {
                ClickType click = e.getClick();
                if (click.isShiftClick() && click.isRightClick()) {
                    Location targetLoc = targetFarmer.getLocation();
                    if (targetLoc == null) {
                        targetLoc = targetFarmer.getLastKnownLocation();
                        if (targetLoc == null) {
                            targetLoc = player.getLocation();
                        }
                    }
                    plugin.getLogService().log(targetFarmer, player.getName(), "ADMIN_DELETE_FARMER", plugin.getLanguageManager().format("logs.details.admin_delete_farmer"), targetLoc);
                    plugin.getFarmerManager().removeFarmer(targetFarmer);
                    plugin.getLanguageManager().playSound(player, "ENTITY_ITEM_BREAK");
                    plugin.getLanguageManager().sendMessage(player, "messages.admin_removed_farmer", "{owner}", targetFarmer.getOwnerName());
                    openAdminFarmersMenu(player, targetPlayerFilter, currentPage);
                } else if (click.isShiftClick() && click.isLeftClick()) {
                    plugin.getLogService().log(targetFarmer, player.getName(), "ADMIN_CLEAR_STORAGE", plugin.getLanguageManager().format("logs.details.admin_clear_storage"));
                    targetFarmer.getStorage().clear();
                    targetFarmer.setDirty(true);
                    plugin.getNpcManager().updateHologram(targetFarmer.getId());
                    plugin.getLanguageManager().playSound(player, "ENTITY_ITEM_PICKUP");
                    plugin.getLanguageManager().sendMessage(player, "messages.admin_cleared_storage", "{owner}", targetFarmer.getOwnerName());
                    openAdminFarmersMenu(player, targetPlayerFilter, currentPage);
                } else if (click.isLeftClick()) {
                    if (loc != null && loc.getWorld() != null) {
                        player.teleportAsync(loc);
                        plugin.getLanguageManager().playSound(player, "ENTITY_ENDERMAN_TELEPORT");
                        plugin.getLanguageManager().sendMessage(player, "messages.admin_teleported", "{owner}", targetFarmer.getOwnerName());
                        player.closeInventory();
                    } else {
                        plugin.getLanguageManager().sendMessage(player, "messages.admin_world_not_loaded");
                    }
                } else if (click.isRightClick()) {
                    plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
                    openMainMenu(player, targetFarmer);
                }
            });
        }

        if (currentPage > 1) {
            ItemStack prev = new ItemBuilder(Material.ARROW)
                    .name(plugin.getLanguageManager().getRaw("gui.common.previous_page"))
                    .build();
            inv.setItem(48, prev);
            holder.setAction(48, e -> openAdminFarmersMenu(player, targetPlayerFilter, currentPage - 1));
        }

        ItemStack refresh = new ItemBuilder(Material.EMERALD)
                .name(plugin.getLanguageManager().getRaw("gui.admin.refresh"))
                .build();
        inv.setItem(49, refresh);
        holder.setAction(49, e -> {
            plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
            openAdminFarmersMenu(player, targetPlayerFilter, currentPage);
        });

        if (currentPage < totalPages) {
            ItemStack next = new ItemBuilder(Material.ARROW)
                    .name(plugin.getLanguageManager().getRaw("gui.common.next_page"))
                    .build();
            inv.setItem(50, next);
            holder.setAction(50, e -> openAdminFarmersMenu(player, targetPlayerFilter, currentPage + 1));
        }

        ItemStack close = new ItemBuilder(Material.BARRIER)
                .name(plugin.getLanguageManager().getRaw("gui.common.close"))
                .build();
        inv.setItem(45, close);
        holder.setAction(45, e -> player.closeInventory());

        player.openInventory(inv);
    }



    public void openPlayerLogsMenu(Player viewer, String targetPlayerName, UUID targetUuid, int page) {
        int itemsPerPage = 28;
        int totalLogs = plugin.getDatabaseManager().countPlayerLogs(targetUuid);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalLogs / itemsPerPage));
        int currentPage = Math.min(Math.max(1, page), totalPages);

        FarmerGUIHolder holder = new FarmerGUIHolder(null, FarmerGUIHolder.Type.LOGS, currentPage);
        Component title = plugin.getLanguageManager().getComponent(
                "gui.logs.title",
                "{player}", targetPlayerName,
                "{page}", String.valueOf(currentPage),
                "{total_pages}", String.valueOf(totalPages)
        );
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        fillBorders(inv, Material.BLUE_STAINED_GLASS_PANE);

        int purgeDays = plugin.getConfig().getInt("logging.purge_after_days", 15);
        ItemStack header = new ItemBuilder(Material.BOOK)
                .name(plugin.getLanguageManager().format("gui.logs.header_name", "{player}", targetPlayerName))
                .lore(plugin.getLanguageManager().getStringList(
                        "gui.logs.header_lore",
                        "{count}", String.valueOf(totalLogs),
                        "{days}", String.valueOf(purgeDays),
                        "{player}", targetPlayerName
                ))
                .glow(true)
                .build();
        inv.setItem(4, header);

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        int offset = (currentPage - 1) * itemsPerPage;
        List<FarmerLogEntry> logs = plugin.getDatabaseManager().loadPlayerLogs(targetUuid, offset, itemsPerPage);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (int i = 0; i < Math.min(logs.size(), slots.length); i++) {
            FarmerLogEntry entry = logs.get(i);
            Material icon;
            String act = entry.getAction();
            if (act.contains("SELL")) {
                icon = Material.EMERALD;
            } else if (act.contains("UPGRADE")) {
                icon = Material.NETHER_STAR;
            } else if (act.contains("FUEL") || act.contains("SALARY")) {
                icon = Material.BLAZE_POWDER;
            } else if (act.contains("WITHDRAW")) {
                icon = Material.CHEST;
            } else if (act.contains("PLACE")) {
                icon = Material.FARMLAND;
            } else if (act.contains("PICKUP")) {
                icon = Material.HOPPER_MINECART;
            } else if (act.contains("MEMBER")) {
                icon = Material.PLAYER_HEAD;
            } else if (act.contains("ADMIN")) {
                icon = Material.BARRIER;
            } else {
                icon = Material.PAPER;
            }

            String formattedTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(entry.getTimestamp()), ZoneId.systemDefault()).format(timeFormatter);

            String locationDisplay = entry.getLocationStr();
            if (locationDisplay == null || locationDisplay.equalsIgnoreCase("World Unknown") || locationDisplay.isBlank()) {
                List<Farmer> targetFarmers = plugin.getFarmerManager().getFarmersByOwner(targetUuid);
                if (!targetFarmers.isEmpty()) {
                    Farmer first = targetFarmers.get(0);
                    Location fl = first.getLocation() != null ? first.getLocation() : first.getLastKnownLocation();
                    if (fl != null && fl.getWorld() != null) {
                        locationDisplay = fl.getWorld().getName() + " (" + fl.getBlockX() + ", " + fl.getBlockY() + ", " + fl.getBlockZ() + ")";
                    }
                }
                if (locationDisplay == null || locationDisplay.equalsIgnoreCase("World Unknown") || locationDisplay.isBlank()) {
                    if (viewer.getWorld() != null) {
                        Location vl = viewer.getLocation();
                        locationDisplay = vl.getWorld().getName() + " (" + vl.getBlockX() + ", " + vl.getBlockY() + ", " + vl.getBlockZ() + ")";
                    }
                }
            }
            if (locationDisplay == null || locationDisplay.equalsIgnoreCase("World Unknown") || locationDisplay.isBlank()) {
                locationDisplay = plugin.getLanguageManager().getRaw("gui.logs.unknown_location");
            }

            String rawAction = entry.getAction();
            String actionKey = "gui.logs.actions." + (rawAction != null ? rawAction.toLowerCase() : "unknown");
            String actionDisplay = plugin.getLanguageManager().getRaw(actionKey);
            if (actionDisplay == null || actionDisplay.startsWith("gui.logs.actions.")) {
                actionDisplay = rawAction != null ? rawAction : "UNKNOWN";
            }

            ItemStack logItem = new ItemBuilder(icon)
                    .name(plugin.getLanguageManager().format("gui.logs.entry_name", "{time}", formattedTime, "{action}", actionDisplay))
                    .lore(plugin.getLanguageManager().getStringList(
                            "gui.logs.entry_lore",
                            "{time}", formattedTime,
                            "{actor}", entry.getActorName(),
                            "{server}", entry.getServerName(),
                            "{location}", locationDisplay,
                            "{details}", formatLogDetails(entry)
                    ))
                    .build();

            inv.setItem(slots[i], logItem);
        }

        if (currentPage > 1) {
            ItemStack prev = new ItemBuilder(Material.ARROW)
                    .name(plugin.getLanguageManager().getRaw("gui.common.previous_page"))
                    .build();
            inv.setItem(48, prev);
            holder.setAction(48, e -> openPlayerLogsMenu(viewer, targetPlayerName, targetUuid, currentPage - 1));
        }

        ItemStack refresh = new ItemBuilder(Material.EMERALD)
                .name(plugin.getLanguageManager().getRaw("gui.admin.refresh"))
                .build();
        inv.setItem(49, refresh);
        holder.setAction(49, e -> {
            plugin.getLanguageManager().playSound(viewer, "UI_BUTTON_CLICK");
            openPlayerLogsMenu(viewer, targetPlayerName, targetUuid, currentPage);
        });

        if (currentPage < totalPages) {
            ItemStack next = new ItemBuilder(Material.ARROW)
                    .name(plugin.getLanguageManager().getRaw("gui.common.next_page"))
                    .build();
            inv.setItem(50, next);
            holder.setAction(50, e -> openPlayerLogsMenu(viewer, targetPlayerName, targetUuid, currentPage + 1));
        }

        ItemStack close = new ItemBuilder(Material.BARRIER)
                .name(plugin.getLanguageManager().getRaw("gui.common.close"))
                .build();
        inv.setItem(45, close);
        holder.setAction(45, e -> viewer.closeInventory());

        viewer.openInventory(inv);
    }

    public void openStatsMenu(Player player, Farmer farmer, StatCategory category) {
        FarmerGUIHolder holder = new FarmerGUIHolder(farmer, FarmerGUIHolder.Type.STATS, 1);
        String categoryName = plugin.getLanguageManager().getRaw("gui.stats.category_" + category.name().toLowerCase());
        Component title = plugin.getLanguageManager().getComponent("gui.stats.title", "{category}", categoryName);
        Inventory inv = Bukkit.createInventory(holder, 45, title);
        holder.setInventory(inv);

        fillBorders(inv, Material.CYAN_STAINED_GLASS_PANE);

        PlayerStats stats = plugin.getStatsService().getStats(player.getUniqueId(), category);

        List<Farmer> farmers = plugin.getFarmerManager().getFarmersByOwner(player.getUniqueId());
        long totalStored = 0;
        double totalEarnedAll = 0.0;
        for (Farmer f : farmers) {
            totalStored += f.getTotalStored();
            totalEarnedAll += f.getTotalEarned();
        }

        ItemStack cropsItem = new ItemBuilder(Material.WHEAT)
                .name(plugin.getLanguageManager().getRaw("gui.stats.crops_name"))
                .lore(plugin.getLanguageManager().getStringList(
                        "gui.stats.crops_lore",
                        "{category}", categoryName,
                        "{crops}", NumberUtil.format(stats.getCropsHarvested())
                ))
                .glow(stats.getCropsHarvested() > 0)
                .build();
        inv.setItem(10, cropsItem);

        ItemStack mobsItem = new ItemBuilder(Material.ROTTEN_FLESH)
                .name(plugin.getLanguageManager().getRaw("gui.stats.mobs_name"))
                .lore(plugin.getLanguageManager().getStringList(
                        "gui.stats.mobs_lore",
                        "{category}", categoryName,
                        "{mobs}", NumberUtil.format(stats.getMobsCollected())
                ))
                .glow(stats.getMobsCollected() > 0)
                .build();
        inv.setItem(12, mobsItem);

        ItemStack salesItem = new ItemBuilder(Material.EMERALD)
                .name(plugin.getLanguageManager().getRaw("gui.stats.sales_name"))
                .lore(plugin.getLanguageManager().getStringList(
                        "gui.stats.sales_lore",
                        "{category}", categoryName,
                        "{items}", NumberUtil.format(stats.getItemsSold()),
                        "{money}", NumberUtil.format(stats.getMoneyEarned())
                ))
                .glow(stats.getMoneyEarned() > 0)
                .build();
        inv.setItem(14, salesItem);

        ItemStack fuelItem = new ItemBuilder(Material.BLAZE_POWDER)
                .name(plugin.getLanguageManager().getRaw("gui.stats.fuel_name"))
                .lore(plugin.getLanguageManager().getStringList(
                        "gui.stats.fuel_lore",
                        "{category}", categoryName,
                        "{fuel}", NumberUtil.format(stats.getFuelUsed())
                ))
                .glow(stats.getFuelUsed() > 0)
                .build();
        inv.setItem(16, fuelItem);

        ItemStack profileItem = new ItemBuilder(Material.PLAYER_HEAD)
                .name(plugin.getLanguageManager().format("gui.stats.profile_name", "{player}", player.getName()))
                .lore(plugin.getLanguageManager().getStringList(
                        "gui.stats.profile_lore",
                        "{player}", player.getName(),
                        "{category}", categoryName,
                        "{farmers}", String.valueOf(farmers.size()),
                        "{limit}", String.valueOf(plugin.getFarmerManager().getFarmerLimit(player)),
                        "{stored}", NumberUtil.format(totalStored),
                        "{total_earned}", NumberUtil.format(totalEarnedAll)
                ))
                .build();
        inv.setItem(22, profileItem);

        ItemStack dailyBtn = new ItemBuilder(Material.CLOCK)
                .name(plugin.getLanguageManager().getRaw("gui.stats.btn_daily_name"))
                .lore(plugin.getLanguageManager().getStringList(
                        "gui.stats.btn_daily_lore",
                        "{status}", (category == StatCategory.DAILY ? "<green>✔ Seçili Kategori</green>" : "<yellow>► Görüntülemek için tıklayın.</yellow>")
                ))
                .glow(category == StatCategory.DAILY)
                .build();
        inv.setItem(29, dailyBtn);
        holder.setAction(29, e -> {
            plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
            openStatsMenu(player, farmer, StatCategory.DAILY);
        });

        ItemStack weeklyBtn = new ItemBuilder(Material.COMPASS)
                .name(plugin.getLanguageManager().getRaw("gui.stats.btn_weekly_name"))
                .lore(plugin.getLanguageManager().getStringList(
                        "gui.stats.btn_weekly_lore",
                        "{status}", (category == StatCategory.WEEKLY ? "<green>✔ Seçili Kategori</green>" : "<yellow>► Görüntülemek için tıklayın.</yellow>")
                ))
                .glow(category == StatCategory.WEEKLY)
                .build();
        inv.setItem(30, weeklyBtn);
        holder.setAction(30, e -> {
            plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
            openStatsMenu(player, farmer, StatCategory.WEEKLY);
        });

        ItemStack monthlyBtn = new ItemBuilder(Material.PAPER)
                .name(plugin.getLanguageManager().getRaw("gui.stats.btn_monthly_name"))
                .lore(plugin.getLanguageManager().getStringList(
                        "gui.stats.btn_monthly_lore",
                        "{status}", (category == StatCategory.MONTHLY ? "<green>✔ Seçili Kategori</green>" : "<yellow>► Görüntülemek için tıklayın.</yellow>")
                ))
                .glow(category == StatCategory.MONTHLY)
                .build();
        inv.setItem(32, monthlyBtn);
        holder.setAction(32, e -> {
            plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
            openStatsMenu(player, farmer, StatCategory.MONTHLY);
        });

        ItemStack allTimeBtn = new ItemBuilder(Material.NETHER_STAR)
                .name(plugin.getLanguageManager().getRaw("gui.stats.btn_all_time_name"))
                .lore(plugin.getLanguageManager().getStringList(
                        "gui.stats.btn_all_time_lore",
                        "{status}", (category == StatCategory.ALL_TIME ? "<green>✔ Seçili Kategori</green>" : "<yellow>► Görüntülemek için tıklayın.</yellow>")
                ))
                .glow(category == StatCategory.ALL_TIME)
                .build();
        inv.setItem(33, allTimeBtn);
        holder.setAction(33, e -> {
            plugin.getLanguageManager().playSound(player, "UI_BUTTON_CLICK");
            openStatsMenu(player, farmer, StatCategory.ALL_TIME);
        });

        ItemStack back = new ItemBuilder(farmer != null ? Material.OAK_DOOR : Material.BARRIER)
                .name(plugin.getLanguageManager().getRaw(farmer != null ? "gui.common.back" : "gui.common.close"))
                .build();
        inv.setItem(40, back);
        holder.setAction(40, e -> {
            if (farmer != null) {
                openMainMenu(player, farmer);
            } else {
                player.closeInventory();
            }
        });

        player.openInventory(inv);
    }



    public void showFarmerRangeParticles(Player player, Farmer farmer) {
        Location fLoc = farmer.getLocation();
        if (fLoc == null || fLoc.getWorld() == null) {
            fLoc = farmer.getLastKnownLocation();
            if (fLoc == null || fLoc.getWorld() == null) {
                fLoc = player.getLocation();
            }
        }
        World world = fLoc.getWorld();
        if (world == null || !player.getWorld().equals(world)) {
            plugin.getLanguageManager().sendMessage(player, "messages.admin_world_not_loaded");
            return;
        }

        plugin.getLanguageManager().playSound(player, "BLOCK_BEACON_ACTIVATE");
        plugin.getLanguageManager().sendMessage(player, "messages.range_view_started");

        int centerChunkX = farmer.getChunkX();
        int centerChunkZ = farmer.getChunkZ();
        int radius = (int) plugin.getFarmerManager().getLevelValue("range", farmer.getRangeLevel());

        int minChunkX = centerChunkX - radius;
        int maxChunkX = centerChunkX + radius;
        int minChunkZ = centerChunkZ - radius;
        int maxChunkZ = centerChunkZ + radius;

        double minX = minChunkX * 16.0;
        double maxX = (maxChunkX + 1) * 16.0;
        double minZ = minChunkZ * 16.0;
        double maxZ = (maxChunkZ + 1) * 16.0;

        Particle greenParticle;
        try {
            greenParticle = Particle.valueOf("HAPPY_VILLAGER");
        } catch (Throwable ignored) {
            try {
                greenParticle = Particle.valueOf("VILLAGER_HAPPY");
            } catch (Throwable ignored2) {
                greenParticle = Particle.COMPOSTER;
            }
        }

        final Particle particleToUse = greenParticle;
        final Object[] taskHandle = new Object[1];
        final AtomicInteger elapsedTicks = new AtomicInteger(0);

        Runnable particleRunnable = new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    plugin.getScheduler().cancelTask(taskHandle[0]);
                    return;
                }

                if (elapsedTicks.addAndGet(10) > 200) {
                    plugin.getScheduler().cancelTask(taskHandle[0]);
                    plugin.getLanguageManager().sendMessage(player, "messages.range_view_ended");
                    plugin.getLanguageManager().playSound(player, "BLOCK_BEACON_DEACTIVATE");
                    return;
                }

                if (!player.getWorld().equals(world)) return;

                double pY = player.getLocation().getY();
                double y1 = pY + 0.3;
                double y2 = pY + 1.2;

                for (double x = minX; x <= maxX; x += 1.5) {
                    player.spawnParticle(particleToUse, x, y1, minZ, 1, 0, 0, 0, 0);
                    player.spawnParticle(particleToUse, x, y1, maxZ, 1, 0, 0, 0, 0);
                    player.spawnParticle(particleToUse, x, y2, minZ, 1, 0, 0, 0, 0);
                    player.spawnParticle(particleToUse, x, y2, maxZ, 1, 0, 0, 0, 0);
                }

                for (double z = minZ; z <= maxZ; z += 1.5) {
                    player.spawnParticle(particleToUse, minX, y1, z, 1, 0, 0, 0, 0);
                    player.spawnParticle(particleToUse, maxX, y1, z, 1, 0, 0, 0, 0);
                    player.spawnParticle(particleToUse, minX, y2, z, 1, 0, 0, 0, 0);
                    player.spawnParticle(particleToUse, maxX, y2, z, 1, 0, 0, 0, 0);
                }

                for (double y = pY; y <= pY + 3.0; y += 0.6) {
                    player.spawnParticle(particleToUse, minX, y, minZ, 1, 0, 0, 0, 0);
                    player.spawnParticle(particleToUse, maxX, y, minZ, 1, 0, 0, 0, 0);
                    player.spawnParticle(particleToUse, minX, y, maxZ, 1, 0, 0, 0, 0);
                    player.spawnParticle(particleToUse, maxX, y, maxZ, 1, 0, 0, 0, 0);
                }

                if (radius > 0) {
                    for (int cx = minChunkX + 1; cx <= maxChunkX; cx++) {
                        double ix = cx * 16.0;
                        for (double z = minZ; z <= maxZ; z += 2.0) {
                            player.spawnParticle(particleToUse, ix, y1, z, 1, 0, 0, 0, 0);
                        }
                    }
                    for (int cz = minChunkZ + 1; cz <= maxChunkZ; cz++) {
                        double iz = cz * 16.0;
                        for (double x = minX; x <= maxX; x += 2.0) {
                            player.spawnParticle(particleToUse, x, y1, iz, 1, 0, 0, 0, 0);
                        }
                    }
                }
            }
        };

        taskHandle[0] = plugin.getScheduler().runGlobalRepeating(particleRunnable, 0L, 10L);
    }

    private void fillBorders(Inventory inv, Material glass) {
        ItemStack filler = new ItemBuilder(glass).name(" ").build();
        int size = inv.getSize();
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, filler);
            }
        }
    }

    private String formatMaterialName(Material material) {
        String[] words = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String formatLogDetails(FarmerLogEntry entry) {
        if (entry == null) return "";
        String raw = entry.getDetails();
        if (raw == null || raw.isBlank()) return "";
        String act = entry.getAction() != null ? entry.getAction().toUpperCase() : "";

        if (act.contains("PICKUP_GUI")) {
            return plugin.getLanguageManager().format("logs.details.pickup_gui");
        }
        if (act.contains("PICKUP_CMD")) {
            return plugin.getLanguageManager().format("logs.details.pickup_cmd");
        }
        if (act.contains("PLACE_RESTORE")) {
            Matcher m = Pattern.compile("(?i)(?:Depo|Storage):?\\s*([0-9.,]+)").matcher(raw);
            String stored = m.find() ? m.group(1) : "0";
            return plugin.getLanguageManager().format("logs.details.place_restore", "{stored}", stored);
        }
        if (act.equals("PLACE")) {
            return plugin.getLanguageManager().format("logs.details.place");
        }
        if (act.contains("PICKUP_BREAK")) {
            Matcher mStored = Pattern.compile("(?i)(?:Depo|Storage):?\\s*([0-9.,]+)").matcher(raw);
            Matcher mFuel = Pattern.compile("(?i)(?:Yakıt|Fuel):?\\s*([0-9.,]+)").matcher(raw);
            String stored = mStored.find() ? mStored.group(1) : "0";
            String fuel = mFuel.find() ? mFuel.group(1) : "0";
            return plugin.getLanguageManager().format("logs.details.pickup_break", "{stored}", stored, "{fuel}", fuel);
        }
        if (act.contains("UPGRADE")) {
            Matcher mLevel = Pattern.compile("(?i)(?:Seviye|Level)\\s*([0-9]+)").matcher(raw);
            Matcher mCost = Pattern.compile("\\$([0-9.,]+)").matcher(raw);
            String level = mLevel.find() ? mLevel.group(1) : "1";
            String cost = mCost.find() ? mCost.group(1) : "0";
            String type = plugin.getLanguageManager().getRaw("gui.main.upgrades_name");
            String lower = raw.toLowerCase();
            if (lower.contains("depo") || lower.contains("capacity")) {
                type = plugin.getLanguageManager().getRaw("gui.upgrades.capacity_name");
            } else if (lower.contains("menzil") || lower.contains("range")) {
                type = plugin.getLanguageManager().getRaw("gui.upgrades.range_name");
            } else if (lower.contains("hız") || lower.contains("speed")) {
                type = plugin.getLanguageManager().getRaw("gui.upgrades.speed_name");
            } else if (lower.contains("şans") || lower.contains("fortune")) {
                type = plugin.getLanguageManager().getRaw("gui.upgrades.fortune_name");
            } else if (lower.contains("vergi") || lower.contains("tax")) {
                type = plugin.getLanguageManager().getRaw("gui.upgrades.tax_name");
            }
            return plugin.getLanguageManager().format("logs.details.upgrade", "{type}", type, "{level}", level, "{cost}", cost);
        }
        if (act.contains("FUEL_ADD")) {
            Matcher m = Pattern.compile("\\+([0-9.,]+)").matcher(raw);
            String amount = m.find() ? m.group(1) : "0";
            return plugin.getLanguageManager().format("logs.details.fuel_add", "{amount}", amount);
        }
        if (act.contains("SALARY_PAID")) {
            Matcher m = Pattern.compile("\\$?([0-9.,]+).*?\\+([0-9.,]+)").matcher(raw);
            if (m.find()) {
                return plugin.getLanguageManager().format("logs.details.salary_paid", "{cost}", m.group(1), "{amount}", m.group(2));
            }
        }
        if (act.contains("MEMBER_ADD")) {
            Matcher m = Pattern.compile("^(.+?)(?: üye olarak eklendi\\.| added as farmer member\\.)", Pattern.CASE_INSENSITIVE).matcher(raw);
            String p = m.find() ? m.group(1).trim() : raw;
            return plugin.getLanguageManager().format("logs.details.member_add", "{player}", p);
        }
        if (act.contains("MEMBER_REMOVE")) {
            Matcher m = Pattern.compile("^(.+?)(?: yetkileri kaldırıldı\\.| permissions revoked\\.)", Pattern.CASE_INSENSITIVE).matcher(raw);
            String p = m.find() ? m.group(1).trim() : raw;
            return plugin.getLanguageManager().format("logs.details.member_remove", "{player}", p);
        }
        if (act.contains("WITHDRAW")) {
            Matcher m = Pattern.compile("^([0-9]+)\\s*(?:adet|x)\\s*(.+?)\\s*(?:depodan çekildi\\.|from farmer storage\\.)", Pattern.CASE_INSENSITIVE).matcher(raw);
            if (m.find()) {
                return plugin.getLanguageManager().format("logs.details.withdraw", "{amount}", m.group(1), "{item}", m.group(2).trim());
            }
        }
        if (act.contains("SELL_ITEM")) {
            Matcher m = Pattern.compile("^([0-9]+)\\s*(?:adet|x)\\s*(.+?)\\s*(?:satıldı\\.|sold\\.)\\s*Net:\\s*\\$([0-9.,]+)", Pattern.CASE_INSENSITIVE).matcher(raw);
            if (m.find()) {
                return plugin.getLanguageManager().format("logs.details.sell_item", "{amount}", m.group(1), "{item}", m.group(2).trim(), "{net}", m.group(3));
            }
        }
        if (act.contains("SELL_ALL")) {
            Matcher m = Pattern.compile("([0-9]+).*?\\$([0-9.,]+)").matcher(raw);
            if (m.find()) {
                return plugin.getLanguageManager().format("logs.details.sell_all", "{amount}", m.group(1), "{net}", m.group(2));
            }
        }
        if (act.contains("AUTO_SELL")) {
            Matcher m = Pattern.compile("([0-9]+).*?Net:\\s*\\$([0-9.,]+).*?\\$([0-9.,]+)").matcher(raw);
            if (m.find()) {
                return plugin.getLanguageManager().format("logs.details.auto_sell", "{amount}", m.group(1), "{net}", m.group(2), "{tax}", m.group(3));
            }
        }
        if (act.contains("ADMIN_DELETE")) {
            return plugin.getLanguageManager().format("logs.details.admin_delete_farmer");
        }
        if (act.contains("ADMIN_CLEAR")) {
            return plugin.getLanguageManager().format("logs.details.admin_clear_storage");
        }

        return raw;
    }
}
