package com.squezsaz.lfarmer.listener;

import com.squezsaz.lfarmer.LFarmer;
import com.squezsaz.lfarmer.model.Farmer;
import com.squezsaz.lfarmer.npc.FarmerNPC;
import com.squezsaz.lfarmer.util.NumberUtil;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class FarmerPlacementListener implements Listener {

    private final LFarmer plugin;

    public FarmerPlacementListener(LFarmer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!plugin.getFarmerManager().isFarmerItem(item)) {
            return;
        }

        boolean placed = handleFarmerPlacement(event.getPlayer(), event.getBlockPlaced().getLocation(), item, event.getHand(), true);
        if (!placed) {
            event.setCancelled(true);
        }
    }

    private boolean handleFarmerPlacement(Player player, Location loc, ItemStack item, EquipmentSlot hand, boolean isBlockPlace) {
        Chunk chunk = loc.getChunk();

        if (plugin.getFarmerManager().getFarmer(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()) != null) {
            plugin.getLanguageManager().sendMessage(player, "messages.already_has_farmer");
            plugin.getLanguageManager().playSound(player, "ENTITY_VILLAGER_NO");
            return false;
        }

        UUID savedFarmerId = plugin.getFarmerManager().getSavedFarmerUuid(item);
        if (savedFarmerId == null && !plugin.getFarmerManager().canPlaceMore(player)) {
            plugin.getLanguageManager().sendMessage(player, "messages.reached_limit", "{limit}", String.valueOf(plugin.getFarmerManager().getFarmerLimit(player)));
            plugin.getLanguageManager().playSound(player, "ENTITY_VILLAGER_NO");
            return false;
        }

        String denyReason = plugin.getHookManager().getPlaceDenyReason(player, loc);
        if (denyReason != null) {
            plugin.getLanguageManager().sendMessage(player, denyReason);
            plugin.getLanguageManager().playSound(player, "ENTITY_VILLAGER_NO");
            return false;
        }

        Location npcLoc = loc.clone().add(0.5, 0.0, 0.5);
        npcLoc.setYaw(player.getLocation().getYaw() + 180f);
        npcLoc.setPitch(0f);

        if (savedFarmerId != null) {
            Farmer farmer = plugin.getFarmerManager().getFarmer(savedFarmerId);
            if (farmer == null) {
                farmer = plugin.getDatabaseManager().loadFarmer(savedFarmerId);
            }
            if (farmer != null) {
                plugin.getFarmerManager().placeExistingFarmer(farmer, player, npcLoc);
                plugin.getLogService().log(farmer, player.getName(), "PLACE_RESTORE", plugin.getLanguageManager().format("logs.details.place_restore", "{stored}", String.valueOf(farmer.getTotalStored())));
                plugin.getLanguageManager().sendMessage(player, "messages.farmer_placed_restored");
                plugin.getLanguageManager().playSound(player, "ENTITY_PLAYER_LEVELUP");
                if (!isBlockPlace && player.getGameMode() != GameMode.CREATIVE) {
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        if (hand == EquipmentSlot.HAND) {
                            player.getInventory().setItemInMainHand(null);
                        } else {
                            player.getInventory().setItemInOffHand(null);
                        }
                    }
                }
                return true;
            }
        }

        Farmer farmer = plugin.getFarmerManager().createFarmer(player, npcLoc);
        plugin.getLogService().log(farmer, player.getName(), "PLACE", plugin.getLanguageManager().format("logs.details.place"));

        plugin.getLanguageManager().sendMessage(player, "messages.farmer_placed");
        plugin.getLanguageManager().playSound(player, "ENTITY_PLAYER_LEVELUP");
        if (!isBlockPlace && player.getGameMode() != GameMode.CREATIVE) {
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                if (hand == EquipmentSlot.HAND) {
                    player.getInventory().setItemInMainHand(null);
                } else {
                    player.getInventory().setItemInOffHand(null);
                }
            }
        }
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        Farmer farmer = plugin.getFarmerManager().getFarmerAtLocation(loc);
        if (farmer == null) {
            return;
        }

        Location farmerLoc = farmer.getLocation();
        if (farmerLoc != null && farmerLoc.getBlockX() == loc.getBlockX() && farmerLoc.getBlockY() == loc.getBlockY() && farmerLoc.getBlockZ() == loc.getBlockZ()) {
            Player player = event.getPlayer();
            if (!farmer.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("farmer.admin")) {
                event.setCancelled(true);
                plugin.getLanguageManager().sendMessage(player, "messages.only_owner_break");
                return;
            }

            Location targetLoc = farmerLoc != null ? farmerLoc : loc;
            plugin.getFarmerManager().pickupFarmerAtLocation(farmer, loc);
            plugin.getLogService().log(farmer, player.getName(), "PICKUP_BREAK", plugin.getLanguageManager().format("logs.details.pickup_break", "{stored}", String.valueOf(farmer.getTotalStored()), "{fuel}", NumberUtil.format(farmer.getFuelAmount())), targetLoc);
            plugin.getLanguageManager().sendMessage(player, "messages.farmer_picked_up");
            plugin.getLanguageManager().playSound(player, "ENTITY_ITEM_PICKUP");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        boolean isFarmer = plugin.getFarmerManager().isFarmerItem(item);

        if (event.getAction() == Action.RIGHT_CLICK_AIR && isFarmer) {
            event.setCancelled(true);
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Location loc = block.getLocation();
        Farmer farmer = plugin.getFarmerManager().getFarmerAtLocation(loc);
        if (farmer != null) {
            Location farmerLoc = farmer.getLocation();
            if (farmerLoc != null && farmerLoc.getBlockX() == loc.getBlockX() && farmerLoc.getBlockZ() == loc.getBlockZ()
                    && (farmerLoc.getBlockY() == loc.getBlockY() || farmerLoc.getBlockY() == loc.getBlockY() + 1)) {
                event.setCancelled(true);
                if (event.getHand() == EquipmentSlot.HAND) {
                    if (!canOpenFarmer(player, farmer)) {
                        plugin.getLanguageManager().sendMessage(player, "messages.no_access");
                        return;
                    }
                    plugin.getGuiManager().openMainMenu(player, farmer);
                }
                return;
            }
        }

        if (isFarmer) {
            event.setCancelled(true);
            if (event.getHand() == EquipmentSlot.HAND || (event.getHand() == EquipmentSlot.OFF_HAND && player.getInventory().getItemInMainHand().getType().isAir())) {
                Block target = block.isReplaceable() ? block : block.getRelative(event.getBlockFace());
                handleFarmerPlacement(player, target.getLocation(), item, event.getHand(), false);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        if (plugin.getFarmerManager().isFarmerItem(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getHand());
        if (plugin.getFarmerManager().isFarmerItem(item)) {
            event.setCancelled(true);
        }

        if (event.getHand() != EquipmentSlot.HAND) return;

        Entity entity = event.getRightClicked();
        FarmerNPC npc = plugin.getNpcManager().getNPCByEntity(entity);
        if (npc != null) {
            event.setCancelled(true);
            Farmer farmer = npc.getFarmer();
            if (!canOpenFarmer(player, farmer)) {
                plugin.getLanguageManager().sendMessage(player, "messages.no_access");
                return;
            }
            plugin.getGuiManager().openMainMenu(player, farmer);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (plugin.getNpcManager().getNPCByEntity(entity) != null) {
            event.setCancelled(true);
        }
    }

    private boolean canOpenFarmer(Player player, Farmer farmer) {
        if (player.hasPermission("farmer.admin")) return true;
        if (farmer.getOwnerUuid().equals(player.getUniqueId())) return true;
        if (!plugin.getHookManager().canAccessFarmer(player, farmer.getLocation())) return false;
        return farmer.getPermission(player.getUniqueId()).canOpen();
    }
}
