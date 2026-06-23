package com.arkflame.smpweapons.util;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class Titles {
    private Titles() {
    }

    public static void sendActionBar(final Player player, final String message) {
        if (player == null || message == null) {
            return;
        }
        try {
            final Method method = player.getClass().getMethod("sendActionBar", String.class);
            method.invoke(player, message);
            return;
        } catch (final Exception ignored) {
            // legacy fallback below
        }
        player.sendMessage(message);
    }
}
