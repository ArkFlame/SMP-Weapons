package com.arkflame.smpweapons.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PlayerItems {
    private PlayerItems() {
    }

    public static ItemStack mainHand(final Player player) {
        return player == null ? null : player.getItemInHand();
    }

    public static ItemStack offHand(final Player player) {
        if (player == null || player.getInventory() == null) {
            return null;
        }
        try {
            final Method method = player.getInventory().getClass().getMethod("getItemInOffHand");
            final Object item = method.invoke(player.getInventory());
            return item instanceof ItemStack ? (ItemStack) item : null;
        } catch (final Exception ignored) {
            return null;
        }
    }

    public static List<ItemStack> allInventoryItems(final Player player) {
        if (player == null || player.getInventory() == null) {
            return Collections.emptyList();
        }
        final PlayerInventory inventory = player.getInventory();
        final List<ItemStack> items = new ArrayList<ItemStack>();
        addAll(items, inventory.getContents());
        addAll(items, inventory.getArmorContents());
        final ItemStack offHand = offHand(player);
        if (offHand != null) {
            items.add(offHand);
        }
        return items;
    }

    public static boolean isBlocking(final Player player) {
        if (player == null) {
            return false;
        }
        try {
            final Method method = player.getClass().getMethod("isBlocking");
            final Object blocking = method.invoke(player);
            return blocking instanceof Boolean && ((Boolean) blocking).booleanValue();
        } catch (final Exception ignored) {
            return false;
        }
    }

    private static void addAll(final List<ItemStack> items, final ItemStack[] source) {
        if (source == null) {
            return;
        }
        for (final ItemStack item : source) {
            if (item != null) {
                items.add(item);
            }
        }
    }
}
