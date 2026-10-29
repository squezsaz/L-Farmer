package com.squezsaz.lfarmer.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private ColorUtil() {
    }

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        String prepared = prepareHex(input);
        if (prepared.contains("<") && prepared.contains(">")) {
            try {
                return MINI_MESSAGE.deserialize(prepared);
            } catch (Exception ignored) {
            }
        }
        return LEGACY_SERIALIZER.deserialize(ChatColor.translateAlternateColorCodes('&', prepared));
    }

    public static String parseToLegacy(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        Component component = parse(input);
        return LEGACY_SERIALIZER.serialize(component);
    }

    private static String prepareHex(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            matcher.appendReplacement(builder, "<#" + hexCode + ">");
        }
        matcher.appendTail(builder);
        return builder.toString();
    }
}
