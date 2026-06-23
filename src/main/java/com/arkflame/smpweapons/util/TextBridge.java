package com.arkflame.smpweapons.util;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TextBridge implements AutoCloseable {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();
    private static final LegacyComponentSerializer LEGACY_DISPLAY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final BukkitAudiences audiences;
    private final Map<String, Object> messages;
    private final Map<String, String> legacyCache = new HashMap<String, String>();

    public TextBridge(final JavaPlugin plugin, final Map<String, Object> messages) {
        this.audiences = BukkitAudiences.create(plugin);
        this.messages = messages == null ? Collections.<String, Object>emptyMap() : new HashMap<String, Object>(messages);
    }

    public void send(final CommandSender sender, final String key) {
        send(sender, key, Collections.<String, String>emptyMap());
    }

    public void send(final CommandSender sender, final String key, final Map<String, String> placeholders) {
        if (sender == null) {
            return;
        }
        final String raw = message(key, placeholders);
        final String rendered = legacy(raw);
        sender.sendMessage(rendered);
    }

    public void sendActionBar(final Player player, final String key, final Map<String, String> placeholders) {
        if (player == null) {
            return;
        }
        Titles.sendActionBar(player, legacy(message(key, placeholders)));
    }

    public void sendRaw(final CommandSender sender, final String input) {
        if (sender == null) {
            return;
        }
        sender.sendMessage(legacy(input));
    }

    public String message(final String key, final Map<String, String> placeholders) {
        final Object value = nested(this.messages, key);
        final Object prefix = this.messages.get("prefix");
        final String raw = value == null ? key : String.valueOf(value);
        final String withPrefix = key != null && key.startsWith("usage") ? raw : String.valueOf(prefix == null ? "" : prefix) + raw;
        return replace(withPrefix, placeholders);
    }

    @SuppressWarnings("unchecked")
    public List<String> messageList(final String key) {
        final Object value = nested(this.messages, key);
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        final List<Object> raw = (List<Object>) value;
        final List<String> output = new ArrayList<String>(raw.size());
        for (final Object line : raw) {
            output.add(legacy(String.valueOf(line)));
        }
        return output;
    }

    public String legacy(final String input) {
        final String key = input == null ? "" : input;
        final String cached = this.legacyCache.get(key);
        if (cached != null) {
            return cached;
        }
        final String rendered = renderLegacy(key);
        this.legacyCache.put(key, rendered);
        return rendered;
    }

    public List<String> lore(final List<String> input) {
        if (input == null) {
            return Collections.emptyList();
        }
        final List<String> output = new ArrayList<String>(input.size());
        for (final String line : input) {
            output.add(legacyNoItalic(line));
        }
        return output;
    }


    private String legacyNoItalic(final String input) {
        final String key = "NO_ITALIC\u0000" + (input == null ? "" : input);
        final String cached = this.legacyCache.get(key);
        if (cached != null) {
            return cached;
        }
        final String rendered = LEGACY_DISPLAY.serialize(component(input).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        this.legacyCache.put(key, rendered);
        return rendered;
    }

    public void applyNameAndLore(final ItemMeta meta, final String name, final List<String> lore) {
        if (meta == null) {
            return;
        }
        if (name != null) {
            meta.setDisplayName(legacy(name));
        }
        if (lore != null) {
            meta.setLore(lore(lore));
        }
    }

    public static String stripColor(final String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("(?i)§[0-9A-FK-ORX]", "");
    }

    public static String normalizePlain(final String input) {
        return stripColor(input).toLowerCase(Locale.ROOT).trim();
    }

    public static String renderLegacy(final String input) {
        return LEGACY_DISPLAY.serialize(component(input));
    }

    private static Component component(final String input) {
        final String normalized = normalize(input);
        if (normalized.isEmpty()) {
            return Component.empty();
        }
        try {
            if (looksLikeMiniMessage(normalized)) {
                return MINI_MESSAGE.deserialize(normalized);
            }
            if (looksLikeAmpersand(normalized)) {
                return LEGACY_AMPERSAND.deserialize(normalized);
            }
            if (looksLikeSection(normalized)) {
                return LEGACY_SECTION.deserialize(normalized);
            }
            return MINI_MESSAGE.deserialize(normalized);
        } catch (final Exception ignored) {
            try {
                return LEGACY_AMPERSAND.deserialize(normalized);
            } catch (final Exception ignoredAgain) {
                return LEGACY_SECTION.deserialize(normalized);
            }
        }
    }

    private static String normalize(final String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\n", "\n").replace("%nl%", "\n").replace("<newline>", "\n");
    }

    private static boolean looksLikeMiniMessage(final String input) {
        return input.indexOf('<') >= 0 && input.indexOf('>') > input.indexOf('<');
    }

    private static boolean looksLikeAmpersand(final String input) {
        return input.indexOf('&') >= 0;
    }

    private static boolean looksLikeSection(final String input) {
        return input.indexOf('§') >= 0;
    }

    private static String replace(final String message, final Map<String, String> placeholders) {
        String output = message == null ? "" : message;
        if (placeholders == null) {
            return output;
        }
        for (final Map.Entry<String, String> entry : placeholders.entrySet()) {
            final String value = entry.getValue() == null ? "" : entry.getValue();
            output = output.replace("<" + entry.getKey() + ">", value).replace("{" + entry.getKey() + "}", value);
        }
        return output;
    }

    @SuppressWarnings("unchecked")
    private static Object nested(final Map<String, Object> root, final String key) {
        if (root == null || key == null) {
            return null;
        }
        Object current = root;
        final String[] parts = key.split("\\.");
        for (final String part : parts) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<String, Object>) current).get(part);
        }
        return current;
    }

    @Override
    public void close() {
        this.audiences.close();
    }
}
