package com.arkflame.smpweapons.util;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;
import java.util.Optional;

public final class PotionEffects {
    private PotionEffects() {
    }

    public static void apply(final LivingEntity entity, final String encoded) {
        if (entity == null || encoded == null || encoded.trim().isEmpty()) {
            return;
        }
        final String[] parts = encoded.split(":");
        final String name = parts.length > 0 ? parts[0] : "";
        final int amplifier = parts.length > 1 ? parseInt(parts[1], 1) : 1;
        final int seconds = parts.length > 2 ? parseInt(parts[2], 1) : 1;
        apply(entity, name, amplifier, seconds);
    }

    public static void apply(final LivingEntity entity, final String name, final int skriptAmplifier, final int seconds) {
        if (entity == null || name == null) {
            return;
        }
        final Optional<PotionEffectType> type = resolve(name);
        if (!type.isPresent()) {
            return;
        }
        final int bukkitAmplifier = Math.max(0, skriptAmplifier - 1);
        entity.addPotionEffect(new PotionEffect(type.get(), Math.max(1, seconds) * 20, bukkitAmplifier, true, true), true);
    }

    public static Optional<PotionEffectType> resolve(final String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        final String normalized = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        final String[] aliases = aliases(normalized);
        for (final String alias : aliases) {
            final PotionEffectType byName = PotionEffectType.getByName(alias);
            if (byName != null) {
                return Optional.of(byName);
            }
        }
        return Optional.empty();
    }

    private static String[] aliases(final String normalized) {
        if ("STRENGTH".equals(normalized)) {
            return new String[]{"INCREASE_DAMAGE", normalized};
        }
        if ("SLOWNESS".equals(normalized)) {
            return new String[]{"SLOW", normalized};
        }
        if ("HASTE".equals(normalized)) {
            return new String[]{"FAST_DIGGING", normalized};
        }
        return new String[]{normalized};
    }

    private static int parseInt(final String raw, final int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (final Exception ignored) {
            return fallback;
        }
    }
}
