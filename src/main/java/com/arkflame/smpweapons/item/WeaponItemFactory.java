package com.arkflame.smpweapons.item;

import com.arkflame.smpweapons.model.WeaponDefinition;
import com.arkflame.smpweapons.util.Materials;
import com.arkflame.smpweapons.util.TextBridge;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
            applyBanner(meta, definition.getBannerSection());
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
        } else if ("POWER".equals(normalized)) {
            aliases = new String[]{"ARROW_DAMAGE", normalized};
        } else if ("PUNCH".equals(normalized)) {
            aliases = new String[]{"ARROW_KNOCKBACK", normalized};
        } else if ("FLAME".equals(normalized)) {
            aliases = new String[]{"ARROW_FIRE", normalized};
        } else if ("INFINITY".equals(normalized)) {
            aliases = new String[]{"ARROW_INFINITE", normalized};
        } else if ("UNBREAKING".equals(normalized)) {
            aliases = new String[]{"DURABILITY", normalized};
        } else if ("QUICKCHARGE".equals(normalized)) {
            aliases = new String[]{"QUICK_CHARGE", normalized};
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

    private void applyBanner(final ItemMeta meta, final ConfigurationSection section) {
        if (meta == null || section == null) {
            return;
        }
        try {
            final Class<?> bannerMetaClass = Class.forName("org.bukkit.inventory.meta.BannerMeta");
            if (!bannerMetaClass.isInstance(meta)) {
                return;
            }
            final Class<?> dyeColorClass = Class.forName("org.bukkit.DyeColor");
            final Class<?> patternTypeClass = Class.forName("org.bukkit.block.banner.PatternType");
            final Class<?> patternClass = Class.forName("org.bukkit.block.banner.Pattern");
            final Object baseColor = resolveDyeColor(section.getString("base-color", null));
            if (baseColor != null) {
                final Method setBaseColor = bannerMetaClass.getMethod("setBaseColor", dyeColorClass);
                setBaseColor.invoke(meta, baseColor);
            }
            final Constructor<?> patternConstructor = patternClass.getConstructor(dyeColorClass, patternTypeClass);
            final Method addPattern = bannerMetaClass.getMethod("addPattern", patternClass);
            final List<Map<?, ?>> patterns = section.getMapList("patterns");
            for (final Map<?, ?> pattern : patterns) {
                final Object typeRaw = pattern.get("type");
                final Object colorRaw = pattern.get("color");
                final Object patternType = resolvePatternType(typeRaw == null ? null : String.valueOf(typeRaw));
                final Object color = resolveDyeColor(colorRaw == null ? null : String.valueOf(colorRaw));
                if (patternType == null || color == null) {
                    continue;
                }
                addPattern.invoke(meta, patternConstructor.newInstance(color, patternType));
            }
        } catch (final Exception ignored) {
            // optional banner capability
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object resolveDyeColor(final String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        final String normalized = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        final String[] aliases;
        if ("GRAY".equals(normalized)) {
            aliases = new String[]{"GRAY", "SILVER"};
        } else if ("GREY".equals(normalized)) {
            aliases = new String[]{"GRAY", "SILVER", "GREY"};
        } else if ("LIGHT_GRAY".equals(normalized) || "LIGHT_GREY".equals(normalized)) {
            aliases = new String[]{"LIGHT_GRAY", "SILVER"};
        } else {
            aliases = new String[]{normalized};
        }
        try {
            final Class<?> dyeColorClass = Class.forName("org.bukkit.DyeColor");
            for (final String alias : aliases) {
                try {
                    return Enum.valueOf((Class<Enum>) dyeColorClass, alias);
                } catch (final Exception ignored) {
                    // next alias
                }
            }
        } catch (final Exception ignored) {
            return null;
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object resolvePatternType(final String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        final String normalized = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        final String[] aliases;
        if ("CIRCLE".equals(normalized)) {
            aliases = new String[]{"CIRCLE", "CIRCLE_MIDDLE"};
        } else if ("RHOMBUS".equals(normalized)) {
            aliases = new String[]{"RHOMBUS", "RHOMBUS_MIDDLE"};
        } else {
            aliases = new String[]{normalized};
        }
        try {
            final Class<?> patternTypeClass = Class.forName("org.bukkit.block.banner.PatternType");
            for (final String alias : aliases) {
                try {
                    return Enum.valueOf((Class<Enum>) patternTypeClass, alias);
                } catch (final Exception ignored) {
                    // next alias
                }
            }
        } catch (final Exception ignored) {
            return null;
        }
        return null;
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
