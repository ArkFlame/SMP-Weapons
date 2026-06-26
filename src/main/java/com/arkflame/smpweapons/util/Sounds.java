package com.arkflame.smpweapons.util;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class Sounds {
    private Sounds() {
    }

    public static void play(final Location location, final String name, final float volume, final float pitch) {
        if (location == null || location.getWorld() == null || name == null || name.trim().isEmpty()) {
            return;
        }
        final Sound sound = resolve(name);
        if (sound != null) {
            location.getWorld().playSound(location, sound, volume, pitch);
        }
    }

    public static void play(final Player player, final String name, final float volume, final float pitch) {
        if (player == null || name == null || name.trim().isEmpty()) {
            return;
        }
        final Sound sound = resolve(name);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    private static Sound resolve(final String input) {
        final String normalized = input.trim().toUpperCase(Locale.ROOT).replace('.', '_');
        final String[] aliases = aliases(normalized);
        for (final String alias : aliases) {
            try {
                return Sound.valueOf(alias);
            } catch (final IllegalArgumentException ignored) {
                // next alias
            }
        }
        return null;
    }

    private static String[] aliases(final String normalized) {
        if ("ENTITY_EXPERIENCE_ORB_PICKUP".equals(normalized)) {
            return new String[]{normalized, "ORB_PICKUP"};
        }
        if ("ENTITY_PLAYER_LEVELUP".equals(normalized)) {
            return new String[]{normalized, "LEVEL_UP"};
        }
        if ("BLOCK_NOTE_BLOCK_BASS".equals(normalized)) {
            return new String[]{normalized, "NOTE_BASS"};
        }
        if ("UI_BUTTON_CLICK".equals(normalized)) {
            return new String[]{normalized, "CLICK"};
        }
        if ("BLOCK_CHEST_OPEN".equals(normalized)) {
            return new String[]{normalized, "CHEST_OPEN"};
        }
        if ("ENTITY_GENERIC_EXPLODE".equals(normalized)) {
            return new String[]{normalized, "EXPLODE"};
        }
        if ("ENTITY_GENERIC_HURT".equals(normalized)) {
            return new String[]{normalized, "HURT_FLESH"};
        }
        if ("ENTITY_ENDER_DRAGON_GROWL".equals(normalized)) {
            return new String[]{normalized, "ENDERDRAGON_GROWL"};
        }
        if ("ENTITY_FIREWORK_ROCKET_LAUNCH".equals(normalized)) {
            return new String[]{normalized, "FIREWORK_LAUNCH"};
        }
        if ("ENTITY_WARDEN_SONIC_BOOM".equals(normalized)) {
            return new String[]{normalized, "ENTITY_ENDERDRAGON_GROWL", "ENDERDRAGON_GROWL", "EXPLODE"};
        }
        if ("ENTITY_WARDEN_ROAR".equals(normalized)) {
            return new String[]{normalized, "ENTITY_ENDERDRAGON_GROWL", "ENDERDRAGON_GROWL", "ENDERDRAGON_HIT"};
        }
        if ("ITEM_TRIDENT_THUNDER".equals(normalized)) {
            return new String[]{normalized, "ENTITY_LIGHTNING_THUNDER", "AMBIENCE_THUNDER", "EXPLODE"};
        }
        if ("ITEM_CROSSBOW_SHOOT".equals(normalized)) {
            return new String[]{normalized, "SHOOT_ARROW", "ENTITY_ARROW_SHOOT"};
        }
        if ("ITEM_TOTEM_USE".equals(normalized)) {
            return new String[]{normalized, "ENTITY_PLAYER_LEVELUP", "LEVEL_UP"};
        }
        if ("BLOCK_RESPAWN_ANCHOR_DEPLETE".equals(normalized)) {
            return new String[]{normalized, "EXPLODE"};
        }
        if ("BLOCK_BEACON_ACTIVATE".equals(normalized)) {
            return new String[]{normalized, "LEVEL_UP"};
        }
        if ("ENTITY_PLAYER_ATTACK_SWEEP".equals(normalized)) {
            return new String[]{normalized, "SUCCESSFUL_HIT"};
        }
        if ("ENTITY_WIND_CHARGE_WIND_BURST".equals(normalized) || "ENTITY_BREEZE_WIND_BURST".equals(normalized)) {
            return new String[]{normalized, "FIZZ"};
        }
        if ("ITEM_MACE_SMASH_AIR".equals(normalized) || "ITEM_SPEAR_LUNGE_3".equals(normalized)) {
            return new String[]{normalized, "SUCCESSFUL_HIT"};
        }
        if ("ENTITY_RAVAGER_ROAR".equals(normalized) || "ENTITY_WARDEN_ROAR".equals(normalized)) {
            return new String[]{normalized, "ENDERDRAGON_GROWL"};
        }
        return new String[]{normalized};
    }
}
