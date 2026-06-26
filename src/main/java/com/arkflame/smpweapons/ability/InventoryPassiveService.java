package com.arkflame.smpweapons.ability;

import com.arkflame.smpweapons.SMPWeaponsPlugin;
import com.arkflame.smpweapons.compat.scheduler.TaskHandle;
import com.arkflame.smpweapons.model.WeaponDefinition;
import com.arkflame.smpweapons.util.PlayerItems;
import com.arkflame.smpweapons.util.PotionEffects;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class InventoryPassiveService {
    private final SMPWeaponsPlugin plugin;
    private TaskHandle task;

    public InventoryPassiveService(final SMPWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        final long period = Math.max(20L, plugin.getConfig().getLong("engine.inventory-passive-period-ticks", 40L));
        this.task = plugin.getSchedulerBridge().runGlobalRepeating(new Runnable() {
            @Override
            public void run() {
                tickAll();
            }
        }, period, period);
    }

    public void stop() {
        if (this.task != null) {
            try {
                this.task.cancel();
            } catch (final Exception ignored) {
            }
            this.task = null;
        }
    }

    private void tickAll() {
        for (final Player player : plugin.getServer().getOnlinePlayers()) {
            if (player == null) {
                continue;
            }
            plugin.getSchedulerBridge().runEntityLater(player, new Runnable() {
                @Override
                public void run() {
                    tickPlayer(player);
                }
            }, null, 0L);
        }
    }

    private void tickPlayer(final Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        final List<ItemStack> items = PlayerItems.allInventoryItems(player);
        if (items == null || items.isEmpty()) {
            return;
        }
        final Set<String> seen = new HashSet<String>();
        for (final ItemStack item : items) {
            if (item == null) {
                continue;
            }
            final WeaponDefinition weapon = plugin.getWeaponManager().identify(item).orElse(null);
            if (weapon == null || !weapon.isEnabled()) {
                continue;
            }
            if (!seen.add(weapon.getId())) {
                continue;
            }
            runInventoryPassive(player, weapon);
        }
    }

    private void runInventoryPassive(final Player player, final WeaponDefinition weapon) {
        final ConfigurationSection passives = weapon.getPassivesSection();
        if (passives == null) {
            return;
        }
        for (final String key : passives.getKeys(false)) {
            final ConfigurationSection passive = passives.getConfigurationSection(key);
            if (passive == null) {
                continue;
            }
            if (!matchesInventoryEvent(passive.getStringList("events"))) {
                continue;
            }
            final List<String> effects = passive.getStringList("effects");
            if (effects != null) {
                for (final String effect : effects) {
                    if (effect != null && !effect.trim().isEmpty()) {
                        PotionEffects.apply(player, effect.trim());
                    }
                }
            }
            final String timeline = passive.getString("timeline", null);
            if (timeline != null && !timeline.trim().isEmpty()) {
                plugin.getAbilityEngine().executeNamedTimeline(player, weapon, timeline.trim(), null, player, null);
            }
        }
    }

    private boolean matchesInventoryEvent(final List<String> events) {
        if (events == null || events.isEmpty()) {
            return false;
        }
        for (final String raw : events) {
            final String normalized = normalize(raw);
            if ("INVENTORY_TICK".equals(normalized) || "INVENTORY".equals(normalized) || "HELD_OR_INVENTORY".equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(final String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}