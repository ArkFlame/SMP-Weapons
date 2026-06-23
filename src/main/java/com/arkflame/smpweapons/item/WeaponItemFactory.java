package com.arkflame.smpweapons.item;

import com.arkflame.smpweapons.model.WeaponDefinition;
import com.arkflame.smpweapons.util.Materials;
import com.arkflame.smpweapons.util.TextBridge;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.Locale;

public final class WeaponItemFactory {
    private final TextBridge text;
    private final ItemIdentityService identityService;

    public WeaponItemFactory(final TextBridge text, final ItemIdentityService identityService) {
        this.text = text;
        this.identityService = identityService;
    }

    public ItemStack create(final WeaponDefinition definition, final int amount) {
        final Material material = Materials.fallback(Material.DIAMOND_SWORD, definition.getMaterialAliases());
        final ItemStack item = new ItemStack(material, Math.max(1, amount));
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            this.text.applyNameAndLore(meta, definition.getName(), definition.getLore());
            this.identityService.setCustomModelData(meta, definition.getCustomModelData());
            this.identityService.write(meta, definition.getId());
            addItemFlags(meta, definition.getItemFlags());
            setUnbreakable(meta, definition.isUnbreakable());
            item.setItemMeta(meta);
        }
        applyEnchants(item, definition);
        return item;
    }

    private void applyEnchants(final ItemStack item, final WeaponDefinition definition) {
        for (final String encoded : definition.getEnchants()) {
            final String[] parts = encoded.split(":");
            if (parts.length == 0) {
                continue;
            }
            final Enchantment enchantment = resolveEnchant(parts[0]);
            if (enchantment == null) {
                continue;
            }
            final int level = parts.length > 1 ? parseInt(parts[1], 1) : 1;
            item.addUnsafeEnchantment(enchantment, Math.max(1, level));
        }
    }

    private static Enchantment resolveEnchant(final String raw) {
        if (raw == null) {
            return null;
        }
        final String normalized = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        final String[] aliases;
        if ("SHARPNESS".equals(normalized)) {
            aliases = new String[]{"DAMAGE_ALL", normalized};
        } else if ("UNBREAKING".equals(normalized)) {
            aliases = new String[]{"DURABILITY", normalized};
        } else {
            aliases = new String[]{normalized};
        }
        for (final String alias : aliases) {
            final Enchantment enchantment = Enchantment.getByName(alias);
            if (enchantment != null) {
                return enchantment;
            }
        }
        return null;
    }

    private static int parseInt(final String raw, final int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (final Exception ignored) {
            return fallback;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addItemFlags(final ItemMeta meta, final java.util.List<String> configuredFlags) {
        try {
            final Class<?> flagClass = Class.forName("org.bukkit.inventory.ItemFlag");
            final java.lang.reflect.Method method = meta.getClass().getMethod("addItemFlags", java.lang.reflect.Array.newInstance(flagClass, 0).getClass());
            final java.util.List<Object> flags = new java.util.ArrayList<Object>();
            final java.util.List<String> sourceFlags = configuredFlags == null ? java.util.Collections.<String>emptyList() : configuredFlags;
            for (final String name : sourceFlags) {
                try {
                    flags.add(Enum.valueOf((Class<Enum>) flagClass, name));
                } catch (final Exception ignored) {
                    // enum constant absent on this API
                }
            }
            final Object array = java.lang.reflect.Array.newInstance(flagClass, flags.size());
            for (int i = 0; i < flags.size(); i++) {
                java.lang.reflect.Array.set(array, i, flags.get(i));
            }
            method.invoke(meta, array);
        } catch (final Throwable ignored) {
            // item flags absent on very old forks
        }
    }

    private static void setUnbreakable(final ItemMeta meta, final boolean value) {
        try {
            final Method method = meta.getClass().getMethod("setUnbreakable", boolean.class);
            method.invoke(meta, Boolean.valueOf(value));
        } catch (final Exception ignored) {
            try {
                final Method method = meta.getClass().getMethod("spigot");
                final Object spigot = method.invoke(meta);
                final Method unbreakable = spigot.getClass().getMethod("setUnbreakable", boolean.class);
                unbreakable.invoke(spigot, Boolean.valueOf(value));
            } catch (final Exception ignoredAgain) {
                // optional
            }
        }
    }
}
