package com.arkflame.smpweapons.ability;

import com.arkflame.smpweapons.compat.scheduler.TaskHandle;
import com.arkflame.smpweapons.model.WeaponDefinition;
import com.arkflame.smpweapons.util.FoliaAPI;
import com.arkflame.smpweapons.util.Sounds;
import com.arkflame.smpweapons.util.TextBridge;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CooldownService {
    private final TextBridge text;
    private final FoliaAPI scheduler;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<UUID, Map<String, Long>>();

    public CooldownService(final TextBridge text, final FoliaAPI scheduler) {
        this.text = text;
        this.scheduler = scheduler;
    }

    public boolean isReady(final Player player, final WeaponDefinition weapon) {
        return remainingSeconds(player, weapon) <= 0L;
    }

    public boolean isReady(final Player player, final WeaponDefinition weapon, final String cooldownKey) {
        return remainingSeconds(player, weapon, cooldownKey) <= 0L;
    }

    public long remainingSeconds(final Player player, final WeaponDefinition weapon) {
        return remainingSeconds(player, weapon, null);
    }

    public long remainingSeconds(final Player player, final WeaponDefinition weapon, final String cooldownKey) {
        if (player == null || weapon == null) {
            return 0L;
        }
        final Map<String, Long> map = cooldowns.get(player.getUniqueId());
        if (map == null) {
            return 0L;
        }
        final String key = storageKey(weapon, cooldownKey);
        final Long until = map.get(key);
        if (until == null) {
            return 0L;
        }
        final long remaining = until.longValue() - System.currentTimeMillis();
        if (remaining <= 0L) {
            map.remove(key);
            return 0L;
        }
        return Math.max(1L, (remaining + 999L) / 1000L);
    }

    public void start(final Player player, final WeaponDefinition weapon, final boolean readyNotification) {
        start(player, weapon, null, weapon == null ? 0 : weapon.getCooldownSeconds(), readyNotification);
    }

    public void start(final Player player, final WeaponDefinition weapon, final String cooldownKey, final int seconds, final boolean readyNotification) {
        start(player, weapon, cooldownKey, seconds, readyNotification, player == null ? null : player.getItemInHand());
    }

    public void start(final Player player, final WeaponDefinition weapon, final String cooldownKey, final int seconds, final boolean readyNotification, final org.bukkit.inventory.ItemStack itemForCooldown) {
        if (player == null || weapon == null || seconds <= 0) {
            return;
        }
        Map<String, Long> map = cooldowns.get(player.getUniqueId());
        if (map == null) {
            map = new HashMap<String, Long>();
            cooldowns.put(player.getUniqueId(), map);
        }
        final String key = storageKey(weapon, cooldownKey);
        map.put(key, Long.valueOf(System.currentTimeMillis() + seconds * 1000L));
        setItemCooldown(player, itemForCooldown == null ? player.getItemInHand() : itemForCooldown, seconds * 20);
        if (readyNotification) {
            scheduler.runEntityLater(player, new Runnable() {
                @Override
                public void run() {
                    if (player.isOnline() && remainingSeconds(player, weapon, cooldownKey) <= 0L) {
                        final Map<String, String> placeholders = new HashMap<String, String>();
                        placeholders.put("weapon", weapon.getDisplayId());
                        text.send(player, "ability-ready", placeholders);
                        Sounds.play(player, "ENTITY_EXPERIENCE_ORB_PICKUP", 1.0F, 1.0F);
                    }
                }
            }, null, Math.max(1L, seconds * 20L));
        }
    }

    public void reset(final Player player) {
        if (player != null) {
            cooldowns.remove(player.getUniqueId());
        }
    }

    public void reset(final Player player, final WeaponDefinition weapon) {
        reset(player, weapon, null);
    }

    public void reset(final Player player, final WeaponDefinition weapon, final String cooldownKey) {
        if (player == null || weapon == null) {
            return;
        }
        final Map<String, Long> map = cooldowns.get(player.getUniqueId());
        if (map != null) {
            map.remove(storageKey(weapon, cooldownKey));
            if (cooldownKey == null || cooldownKey.trim().isEmpty()) {
                final String prefix = weapon.getId() + ':';
                final java.util.List<String> remove = new java.util.ArrayList<String>();
                for (final String key : map.keySet()) {
                    if (key.startsWith(prefix)) {
                        remove.add(key);
                    }
                }
                for (final String key : remove) {
                    map.remove(key);
                }
            }
            if (map.isEmpty()) {
                cooldowns.remove(player.getUniqueId());
            }
        }
    }

    public Map<String, Long> snapshot(final Player player) {
        if (player == null) {
            return new HashMap<String, Long>();
        }
        final Map<String, Long> map = cooldowns.get(player.getUniqueId());
        return map == null ? new HashMap<String, Long>() : new HashMap<String, Long>(map);
    }

    public void resetAll() {
        cooldowns.clear();
    }

    private static String storageKey(final WeaponDefinition weapon, final String cooldownKey) {
        if (weapon == null) {
            return "";
        }
        if (cooldownKey == null || cooldownKey.trim().isEmpty() || "primary".equalsIgnoreCase(cooldownKey.trim())) {
            return weapon.getId();
        }
        return weapon.getId() + ':' + cooldownKey.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static void setItemCooldown(final Player player, final org.bukkit.inventory.ItemStack item, final int ticks) {
        if (player == null || item == null) {
            return;
        }
        final int safeTicks = Math.max(0, ticks);
        try {
            final Method itemMethod = player.getClass().getMethod("setCooldown", org.bukkit.inventory.ItemStack.class, int.class);
            itemMethod.invoke(player, item, Integer.valueOf(safeTicks));
        } catch (final Exception ignored) {
            // Bukkit usually exposes Material cooldown; Paper may expose ItemStack cooldown.
        }
        try {
            final Method materialMethod = player.getClass().getMethod("setCooldown", org.bukkit.Material.class, int.class);
            materialMethod.invoke(player, item.getType(), Integer.valueOf(safeTicks));
        } catch (final Exception ignored) {
            // old server, no item cooldown API
        }
    }
}
