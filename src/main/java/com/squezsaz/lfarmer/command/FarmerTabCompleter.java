package com.squezsaz.lfarmer.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FarmerTabCompleter implements TabCompleter {

    private static final List<String> SUB_COMMANDS = Arrays.asList("give", "remove", "reload", "stats", "list", "admin", "log", "help");

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> available = new ArrayList<>();
            for (String sub : SUB_COMMANDS) {
                if (sub.equals("give") && !sender.hasPermission("farmer.admin.give")) continue;
                if (sub.equals("reload") && !sender.hasPermission("farmer.admin.reload")) continue;
                if (sub.equals("admin") && !sender.hasPermission("farmer.admin")) continue;
                available.add(sub);
            }
            return StringUtil.copyPartialMatches(args[0], available, completions);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") && sender.hasPermission("farmer.admin.give")) {
                List<String> playerNames = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    playerNames.add(p.getName());
                }
                return StringUtil.copyPartialMatches(args[1], playerNames, completions);
            }
            if (args[0].equalsIgnoreCase("admin") && sender.hasPermission("farmer.admin")) {
                List<String> playerNames = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    playerNames.add(p.getName());
                }
                return StringUtil.copyPartialMatches(args[1], playerNames, completions);
            }
            if (args[0].equalsIgnoreCase("log") && (sender.hasPermission("farmer.admin.logs") || sender.hasPermission("farmer.admin"))) {
                List<String> playerNames = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    playerNames.add(p.getName());
                }
                return StringUtil.copyPartialMatches(args[1], playerNames, completions);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("farmer.admin.give")) {
            return Arrays.asList("1", "2", "5", "10", "64");
        }

        return Collections.emptyList();
    }
}
