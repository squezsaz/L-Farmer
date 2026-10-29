package com.squezsaz.lfarmer.command;

import com.squezsaz.lfarmer.LFarmer;
import com.squezsaz.lfarmer.model.Farmer;
import com.squezsaz.lfarmer.model.StatCategory;
import com.squezsaz.lfarmer.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class FarmerCommand implements CommandExecutor {

    private final LFarmer plugin;

    public FarmerCommand(LFarmer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                plugin.getLanguageManager().sendMessage(sender, "messages.only_players");
                return true;
            }

            Chunk chunk = player.getLocation().getChunk();
            Farmer farmer = plugin.getFarmerManager().getFarmer(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            if (farmer != null) {
                plugin.getGuiManager().openMainMenu(player, farmer);
                return true;
            }

            List<Farmer> list = plugin.getFarmerManager().getFarmersByOwner(player.getUniqueId());
            if (list.size() == 1) {
                plugin.getGuiManager().openMainMenu(player, list.get(0));
                return true;
            }

            plugin.getLanguageManager().sendMessage(player, "messages.no_farmer_in_chunk");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("give")) {
            if (!sender.hasPermission("farmer.admin.give")) {
                plugin.getLanguageManager().sendMessage(sender, "messages.no_permission");
                return true;
            }

            if (args.length < 2) {
                plugin.getLanguageManager().sendMessage(sender, "messages.usage_give");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                plugin.getLanguageManager().sendMessage(sender, "messages.player_not_found");
                return true;
            }

            int amount = 1;
            if (args.length >= 3) {
                try {
                    amount = Math.max(1, Integer.parseInt(args[2]));
                } catch (NumberFormatException ignored) {
                }
            }

            ItemStack item = plugin.getFarmerManager().getFarmerSpawnItem(amount);
            target.getInventory().addItem(item);
            plugin.getLanguageManager().sendMessage(sender, "messages.gave_farmer", "{player}", target.getName(), "{amount}", String.valueOf(amount));
            plugin.getLanguageManager().sendMessage(target, "messages.received_farmer", "{amount}", String.valueOf(amount));
            return true;
        }

        if (sub.equals("remove")) {
            if (!(sender instanceof Player player)) {
                plugin.getLanguageManager().sendMessage(sender, "messages.only_players");
                return true;
            }

            Chunk chunk = player.getLocation().getChunk();
            Farmer farmer = plugin.getFarmerManager().getFarmer(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            if (farmer == null) {
                plugin.getLanguageManager().sendMessage(player, "messages.no_farmer_in_chunk");
                return true;
            }

            if (!farmer.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("farmer.admin")) {
                plugin.getLanguageManager().sendMessage(player, "messages.only_owner");
                return true;
            }

            Location farmerLoc = farmer.getLocation();
            if (farmerLoc == null) {
                farmerLoc = farmer.getLastKnownLocation();
                if (farmerLoc == null) {
                    farmerLoc = player.getLocation();
                }
            }
            plugin.getFarmerManager().pickupFarmer(farmer, player);
            plugin.getLogService().log(farmer, player.getName(), "PICKUP_CMD", plugin.getLanguageManager().format("logs.details.pickup_cmd"), farmerLoc);
            plugin.getLanguageManager().sendMessage(player, "messages.farmer_picked_up");
            plugin.getLanguageManager().playSound(player, "ENTITY_ITEM_PICKUP");
            return true;
        }

        if (sub.equals("reload")) {
            if (!sender.hasPermission("farmer.admin.reload")) {
                plugin.getLanguageManager().sendMessage(sender, "messages.no_permission");
                return true;
            }

            plugin.reloadPlugin();
            plugin.getLanguageManager().sendMessage(sender, "messages.plugin_reloaded");
            return true;
        }

        if (sub.equals("stats")) {
            if (!(sender instanceof Player player)) {
                plugin.getLanguageManager().sendMessage(sender, "messages.only_players");
                return true;
            }

            Farmer currentFarmer = null;
            Chunk chunk = player.getLocation().getChunk();
            Farmer f = plugin.getFarmerManager().getFarmer(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            if (f != null && (f.getOwnerUuid().equals(player.getUniqueId()) || player.hasPermission("farmer.admin"))) {
                currentFarmer = f;
            }

            plugin.getGuiManager().openStatsMenu(player, currentFarmer, StatCategory.DAILY);
            return true;
        }

        if (sub.equals("list")) {
            if (!(sender instanceof Player player)) {
                plugin.getLanguageManager().sendMessage(sender, "messages.only_players");
                return true;
            }

            List<Farmer> farmers = plugin.getFarmerManager().getFarmersByOwner(player.getUniqueId());
            if (farmers.isEmpty()) {
                plugin.getLanguageManager().sendMessage(player, "messages.no_farmers_owned");
                return true;
            }

            plugin.getLanguageManager().sendMessage(player, "messages.farmer_list_header", "{count}", String.valueOf(farmers.size()));
            for (Farmer f : farmers) {
                plugin.getLanguageManager().sendMessage(
                        player,
                        "messages.farmer_list_item",
                        "{world}", f.getWorld(),
                        "{chunk_x}", String.valueOf(f.getChunkX()),
                        "{chunk_z}", String.valueOf(f.getChunkZ()),
                        "{level}", String.valueOf(f.getCapacityLevel()),
                        "{stored}", NumberUtil.format(f.getTotalStored())
                );
            }
            return true;
        }

        if (sub.equals("admin")) {
            if (!sender.hasPermission("farmer.admin")) {
                plugin.getLanguageManager().sendMessage(sender, "messages.no_permission");
                return true;
            }

            if (!(sender instanceof Player player)) {
                plugin.getLanguageManager().sendMessage(sender, "messages.only_players");
                return true;
            }

            int page = 1;
            String targetFilter = null;
            if (args.length >= 2) {
                try {
                    page = Math.max(1, Integer.parseInt(args[1]));
                } catch (NumberFormatException ignored) {
                    targetFilter = args[1];
                    if (args.length >= 3) {
                        try {
                            page = Math.max(1, Integer.parseInt(args[2]));
                        } catch (NumberFormatException ignored2) {
                        }
                    }
                }
            }

            plugin.getGuiManager().openAdminFarmersMenu(player, targetFilter, page);
            return true;
        }

        if (sub.equals("log")) {
            if (!(sender instanceof Player player)) {
                plugin.getLanguageManager().sendMessage(sender, "messages.only_players");
                return true;
            }

            if (args.length == 1) {
                plugin.getGuiManager().openPlayerLogsMenu(player, player.getName(), player.getUniqueId(), 1);
                return true;
            }

            if (!sender.hasPermission("farmer.admin.logs") && !sender.hasPermission("farmer.admin")) {
                plugin.getLanguageManager().sendMessage(sender, "messages.no_permission");
                return true;
            }

            String targetName = args[1];
            int page = 1;
            if (args.length >= 3) {
                try {
                    page = Math.max(1, Integer.parseInt(args[2]));
                } catch (NumberFormatException ignored) {
                }
            }

            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            UUID targetUuid = target.getUniqueId();
            String cleanName = target.getName() != null ? target.getName() : targetName;
            plugin.getGuiManager().openPlayerLogsMenu(player, cleanName, targetUuid, page);
            return true;
        }

        if (sub.equals("help")) {
            sendHelp(sender);
            return true;
        }

        plugin.getLanguageManager().sendMessage(sender, "messages.unknown_command");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        plugin.getLanguageManager().sendMessage(sender, "messages.help_header");
        plugin.getLanguageManager().sendMessage(sender, "messages.help_farmer");
        plugin.getLanguageManager().sendMessage(sender, "messages.help_list");
        plugin.getLanguageManager().sendMessage(sender, "messages.help_stats");
        plugin.getLanguageManager().sendMessage(sender, "messages.help_remove");
        plugin.getLanguageManager().sendMessage(sender, "messages.help_log_player");
        plugin.getLanguageManager().sendMessage(sender, "messages.help_help");

        if (sender.hasPermission("farmer.admin")) {
            plugin.getLanguageManager().sendMessage(sender, "messages.help_admin_header");
            plugin.getLanguageManager().sendMessage(sender, "messages.help_admin_panel");
            plugin.getLanguageManager().sendMessage(sender, "messages.help_admin_logs");
            plugin.getLanguageManager().sendMessage(sender, "messages.help_admin_give");
            plugin.getLanguageManager().sendMessage(sender, "messages.help_admin_reload");
        }
        plugin.getLanguageManager().sendMessage(sender, "messages.help_footer");
    }
}
