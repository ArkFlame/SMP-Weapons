package com.arkflame.smpweapons.listener;

import com.arkflame.smpweapons.SMPWeaponsPlugin;
import com.arkflame.smpweapons.ability.AbilityItemProtectionService;
import com.arkflame.smpweapons.model.WeaponDefinition;
import com.arkflame.smpweapons.util.PlayerItems;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class TotemPopListener implements Listener {
    private static final String RESURRECT_EVENT_CLASS = "org.bukkit.event.entity.EntityResurrectEvent";
    private final SMPWeaponsPlugin plugin;

    public TotemPopListener(final SMPWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    public void handle(final Event event) {
        if (event == null) {
            return;
        }
        if (!RESURRECT_EVENT_CLASS.equals(event.getClass().getName())) {
            return;
        }
        if (event instanceof Cancellable && ((Cancellable) event).isCancelled()) {
            return;
        }
        final Object entity = invokeNoArg(event, "getEntity");
        if (!(entity instanceof Player)) {
            return;
        }
        final Player player = (Player) entity;
        final String hand = stringOf(invokeNoArg(event, "getHand"));
        ItemStack preferred = null;
        if ("OFF_HAND".equals(hand)) {
            preferred = PlayerItems.offHand(player);
        } else if ("HAND".equals(hand) || "MAIN_HAND".equals(hand)) {
            preferred = PlayerItems.mainHand(player);
        }
        WeaponDefinition matched = null;
        ItemStack matchedItem = null;
        if (preferred != null) {
            final Optional<WeaponDefinition> identified = plugin.getWeaponManager().identify(preferred);
            if (identified.isPresent() && identified.get().isEnabled()) {
                matched = identified.get();
                matchedItem = preferred;
            }
        }
        if (matched == null) {
            final ItemStack main = PlayerItems.mainHand(player);
            if (main != null) {
                final Optional<WeaponDefinition> identified = plugin.getWeaponManager().identify(main);
                if (identified.isPresent() && identified.get().isEnabled()) {
                    matched = identified.get();
                    matchedItem = main;
                }
            }
        }
        if (matched == null) {
            final ItemStack off = PlayerItems.offHand(player);
            if (off != null) {
                final Optional<WeaponDefinition> identified = plugin.getWeaponManager().identify(off);
                if (identified.isPresent() && identified.get().isEnabled()) {
                    matched = identified.get();
                    matchedItem = off;
                }
            }
        }
        if (matched == null) {
            return;
        }
        final AbilityItemProtectionService abilityProtectionService = plugin.getAbilityItemProtectionService();
        final AbilityItemProtectionService.Protection protection = abilityProtectionService == null
                ? null
                : abilityProtectionService.start(player, matched, matchedItem);
        try {
            if (abilityProtectionService != null && !abilityProtectionService.sourceStillPresent(protection)) {
                return;
            }
            final String triggerTimeline = findTriggerTimeline(matched);
            if (triggerTimeline != null && !triggerTimeline.trim().isEmpty()) {
                plugin.getAbilityEngine().executeNamedTimeline(player, matched, triggerTimeline.trim(), null, player, null);
                return;
            }
            final String legacyTimeline = matched.getTriggerTimeline();
            if (legacyTimeline != null && !legacyTimeline.trim().isEmpty()) {
                plugin.getAbilityEngine().executeNamedTimeline(player, matched, legacyTimeline.trim(), null, player, null);
                return;
            }
            plugin.getAbilityEngine().execute(player, matched);
        } finally {
            if (protection != null) {
                protection.close();
            }
        }
    }

    private String findTriggerTimeline(final WeaponDefinition weapon) {
        final ConfigurationSection triggers = weapon.getTriggersSection();
        if (triggers == null) {
            return null;
        }
        for (final String key : triggers.getKeys(false)) {
            final ConfigurationSection trigger = triggers.getConfigurationSection(key);
            if (trigger == null) {
                continue;
            }
            if (!matchesResurrectEvent(trigger.getStringList("events"))) {
                continue;
            }
            return trigger.getString("timeline", null);
        }
        return null;
    }

    private boolean matchesResurrectEvent(final List<String> events) {
        if (events == null || events.isEmpty()) {
            return false;
        }
        for (final String raw : events) {
            final String normalized = normalize(raw);
            if ("TOTEM_POP".equals(normalized) || "RESURRECT".equals(normalized) || "ENTITY_RESURRECT".equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static Object invokeNoArg(final Object target, final String name) {
        if (target == null) {
            return null;
        }
        try {
            final Method method = target.getClass().getMethod(name);
            return method.invoke(target);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private static String stringOf(final Object raw) {
        return raw == null ? null : String.valueOf(raw);
    }

    private static String normalize(final String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}