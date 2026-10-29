package com.squezsaz.lfarmer.locale;

import com.squezsaz.lfarmer.util.ColorUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageManager {

    private final Plugin plugin;
    private FileConfiguration langConfig;
    private String currentLang;
    private String prefix;
    private final Map<String, String> cache;

    public LanguageManager(Plugin plugin) {
        this.plugin = plugin;
        this.cache = new HashMap<>();
    }

    public void load(String lang) {
        this.currentLang = (lang != null && !lang.isEmpty()) ? lang.toLowerCase() : "tr";
        this.cache.clear();

        File langFolder = new File(plugin.getDataFolder(), "locale");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        saveDefaultLangFile("messages_tr.yml");
        saveDefaultLangFile("messages_en.yml");

        File activeFile = new File(langFolder, "messages_" + this.currentLang + ".yml");
        if (!activeFile.exists()) {
            activeFile = new File(langFolder, "messages_tr.yml");
        }

        this.langConfig = YamlConfiguration.loadConfiguration(activeFile);

        InputStream defStream = plugin.getResource("locale/messages_" + this.currentLang + ".yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            langConfig.setDefaults(defConfig);
        }

        this.prefix = langConfig.getString("prefix", "<gradient:#ffb347:#ffcc33>[L-Farmer]</gradient> ");
    }

    private void saveDefaultLangFile(String fileName) {
        File langFolder = new File(plugin.getDataFolder(), "locale");
        File file = new File(langFolder, fileName);
        if (!file.exists()) {
            plugin.saveResource("locale/" + fileName, false);
        }
    }

    public String getRaw(String path) {
        return langConfig.getString(path, path);
    }

    public String format(String path, String... placeholders) {
        String msg = getRaw(path);
        msg = msg.replace("{prefix}", prefix);
        if (placeholders != null && placeholders.length > 1) {
            for (int i = 0; i < placeholders.length - 1; i += 2) {
                String target = placeholders[i];
                String replacement = placeholders[i + 1];
                if (target != null && replacement != null) {
                    msg = msg.replace(target, replacement);
                }
            }
        }
        return msg;
    }

    public Component getComponent(String path, String... placeholders) {
        return ColorUtil.parse(format(path, placeholders));
    }

    public List<Component> getComponentList(String path, String... placeholders) {
        List<String> list = langConfig.getStringList(path);
        List<Component> components = new ArrayList<>();
        if (list.isEmpty()) {
            components.add(getComponent(path, placeholders));
            return components;
        }
        for (String line : list) {
            line = line.replace("{prefix}", prefix);
            if (placeholders != null && placeholders.length > 1) {
                for (int i = 0; i < placeholders.length - 1; i += 2) {
                    String target = placeholders[i];
                    String replacement = placeholders[i + 1];
                    if (target != null && replacement != null) {
                        line = line.replace(target, replacement);
                    }
                }
            }
            components.add(ColorUtil.parse(line));
        }
        return components;
    }

    public List<String> getStringList(String path, String... placeholders) {
        List<String> list = langConfig.getStringList(path);
        List<String> parsed = new ArrayList<>();
        for (String line : list) {
            line = line.replace("{prefix}", prefix);
            if (placeholders != null && placeholders.length > 1) {
                for (int i = 0; i < placeholders.length - 1; i += 2) {
                    String target = placeholders[i];
                    String replacement = placeholders[i + 1];
                    if (target != null && replacement != null) {
                        line = line.replace(target, replacement);
                    }
                }
            }
            parsed.add(ColorUtil.parseToLegacy(line));
        }
        return parsed;
    }

    public void sendMessage(CommandSender sender, String path, String... placeholders) {
        if (sender == null) return;
        sender.sendMessage(getComponent(path, placeholders));
    }

    public void sendActionBar(Player player, String path, String... placeholders) {
        if (player == null) return;
        player.sendActionBar(getComponent(path, placeholders));
    }

    public void sendTitle(Player player, String titlePath, String subtitlePath, int fadeIn, int stay, int fadeOut, String... placeholders) {
        if (player == null) return;
        Component title = getComponent(titlePath, placeholders);
        Component subtitle = getComponent(subtitlePath, placeholders);
        Title.Times times = Title.Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L));
        player.showTitle(Title.title(title, subtitle, times));
    }

    public void playSound(Player player, String soundName) {
        if (player == null || soundName == null || soundName.equalsIgnoreCase("none")) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public String getCurrentLang() {
        return currentLang;
    }

    public String getPrefix() {
        return prefix;
    }
}
