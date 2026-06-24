package com.arkflame.smpweapons.listener;

import com.arkflame.smpweapons.SMPWeaponsPlugin;
import com.arkflame.smpweapons.model.WeaponDefinition;
import com.arkflame.smpweapons.util.Entities;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class WeaponListener implements Listener {
    private final SMPWeaponsPlugin plugin;

    public WeaponListener(final SMPWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        if (event.getClickedBlock() != null && this.plugin.getTemporaryBlockService() != null) {
            this.plugin.getTemporaryBlockService().syncBlock(player, event.getClickedBlock().getLocation());
        }
        final ItemStack item = itemForEvent(player, event);
        final Optional<WeaponDefinition> weapon = this.plugin.getWeaponManager().identify(item);
        if (!weapon.isPresent() || !weapon.get().isEnabled()) {
            return;
        }
        final TriggerActivation activation = matchInteract(event, weapon.get());
        if (activation == null) {
            return;
        }
        if (!canUse(player, weapon.get())) {
            this.plugin.getText().send(player, "no-permission");
            return;
        }
        event.setCancelled(true);
        final boolean bypass = this.plugin.getConfig().getBoolean("settings.cooldown-bypass-enabled", false)
                && (player.hasPermission("smpweapons.bypasscooldown." + weapon.get().getId()) || player.hasPermission("smpweapons.bypasscooldown.*"));
        if (!bypass && !this.plugin.getCooldownService().isReady(player, weapon.get(), activation.cooldownKey)) {
            final Map<String, String> placeholders = new HashMap<String, String>();
            placeholders.put("seconds", String.valueOf(this.plugin.getCooldownService().remainingSeconds(player, weapon.get(), activation.cooldownKey)));
            this.plugin.getText().sendActionBar(player, "ability-cooldown", placeholders);
            return;
        }
        if (!bypass) {
            this.plugin.getCooldownService().start(player, weapon.get(), activation.cooldownKey, activation.cooldownSeconds, this.plugin.getConfig().getBoolean("settings.ready-notification", true), item);
        }
        if (activation.timeline != null && !activation.timeline.trim().isEmpty()) {
            this.plugin.getAbilityEngine().executeNamedTimeline(player, weapon.get(), activation.timeline.trim(), null, null, null);
        } else {
            this.plugin.getAbilityEngine().execute(player, weapon.get());
        }
    }

    @EventHandler
    public void onProjectileHit(final ProjectileHitEvent event) {
        this.plugin.getProjectileService().handleHit(event);
    }

    @EventHandler
    public void onDamage(final EntityDamageByEntityEvent event) {
        this.plugin.getProjectileService().handleDamage(event);
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        final Player attacker = (Player) event.getDamager();
        final Optional<WeaponDefinition> weapon = this.plugin.getWeaponManager().identify(attacker.getItemInHand());
        if (!weapon.isPresent() || !weapon.get().isEnabled()) {
            return;
        }
        this.plugin.getAbilityEngine().executePassive(attacker, (LivingEntity) event.getEntity(), weapon.get());
    }

    @EventHandler
    public void onFallDamage(final EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && this.plugin.getFallProtectionService().isProtected(((Player) event.getEntity()).getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        if (!this.plugin.getConfig().getBoolean("virtual-blocks.refresh-on-join", true)) {
            return;
        }
        final Player player = event.getPlayer();
        this.plugin.getSchedulerBridge().runEntityLater(player, new Runnable() {
            @Override
            public void run() {
                if (plugin.getTemporaryBlockService() != null) {
                    plugin.getTemporaryBlockService().syncAllTo(player);
                }
            }
        }, null, 10L);
    }

    @EventHandler
    public void onWorldChange(final PlayerChangedWorldEvent event) {
        if (this.plugin.getTemporaryBlockService() != null) {
            this.plugin.getTemporaryBlockService().syncAllTo(event.getPlayer());
        }
        if (this.plugin.getGlideService() != null) {
            this.plugin.getGlideService().stop(event.getPlayer());
        }
        if (this.plugin.getProjectileService() != null) {
            this.plugin.getProjectileService().clearOwned(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onMove(final PlayerMoveEvent event) {
        if (!this.plugin.getConfig().getBoolean("virtual-blocks.refresh-on-move-nearby", false) || this.plugin.getTemporaryBlockService() == null) {
            return;
        }
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY() && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        this.plugin.getTemporaryBlockService().syncAllTo(event.getPlayer());
    }

    @EventHandler
    public void onDeath(final PlayerDeathEvent event) {
        cleanupPlayer(event.getEntity());
        if (this.plugin.getProjectileService() != null) {
            this.plugin.getProjectileService().clearOwned(event.getEntity().getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        if (this.plugin.getConfig().getBoolean("settings.reset-cooldowns-on-quit", true)) {
            this.plugin.getCooldownService().reset(event.getPlayer());
        }
        if (this.plugin.getProjectileService() != null) {
            this.plugin.getProjectileService().clearOwned(event.getPlayer().getUniqueId());
        }
        cleanupPlayer(event.getPlayer());
    }

    @EventHandler
    public void onChunkUnload(final ChunkUnloadEvent event) {
        if (this.plugin.getConfig().getBoolean("real-blocks.restore-on-chunk-unload", true) && this.plugin.getTemporaryBlockService() != null) {
            this.plugin.getTemporaryBlockService().restoreChunk(event.getChunk());
        }
    }


    private TriggerActivation matchInteract(final PlayerInteractEvent event, final WeaponDefinition weapon) {
        final org.bukkit.configuration.ConfigurationSection triggers = weapon.getTriggersSection();
        if (triggers != null && !triggers.getKeys(false).isEmpty()) {
            for (final String key : triggers.getKeys(false)) {
                final org.bukkit.configuration.ConfigurationSection trigger = triggers.getConfigurationSection(key);
                if (trigger == null) {
                    continue;
                }
                final Action action = event.getAction();
                final boolean right = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
                final boolean left = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
                if (!matchesEvent(trigger.getStringList("events"), right, left)) {
                    continue;
                }
                boolean conditionsOk = true;
                for (final String condition : trigger.getStringList("conditions")) {
                    if (!matchesCondition(event, condition)) {
                        conditionsOk = false;
                        break;
                    }
                }
                if (!conditionsOk) {
                    continue;
                }
                final String cooldownKey = trigger.getString("cooldown", key == null ? "primary" : key);
                final int seconds = trigger.getInt("cooldown-seconds", cooldownSeconds(weapon, cooldownKey, weapon.getCooldownSeconds()));
                return new TriggerActivation(key, trigger.getString("timeline", null), cooldownKey, seconds);
            }
            return null;
        }
        return matchesLegacyInteract(event, weapon) ? new TriggerActivation("legacy", weapon.getTriggerTimeline(), weapon.getTriggerCooldownKey(), weapon.getCooldownSeconds()) : null;
    }

    private boolean matchesLegacyInteract(final PlayerInteractEvent event, final WeaponDefinition weapon) {
        final Action action = event.getAction();
        final boolean right = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        final boolean left = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        final java.util.List<String> events = weapon.getTriggerEvents();
        if (events.isEmpty()) {
            final String trigger = weapon.getTriggerType() == null ? "SNEAK_RIGHT_CLICK" : normalize(weapon.getTriggerType());
            if (trigger.contains("RIGHT") && !right) {
                return false;
            }
            if (trigger.contains("LEFT") && !left) {
                return false;
            }
            if (!trigger.contains("RIGHT") && !trigger.contains("LEFT") && !right) {
                return false;
            }
        } else if (!matchesEvent(events, right, left)) {
            return false;
        }
        final java.util.List<String> conditions = weapon.getTriggerConditions();
        if (conditions.isEmpty()) {
            final String trigger = weapon.getTriggerType() == null ? "SNEAK_RIGHT_CLICK" : normalize(weapon.getTriggerType());
            if (trigger.contains("NOT_SNEAK")) {
                return !event.getPlayer().isSneaking();
            }
            if (trigger.contains("SNEAK") && !event.getPlayer().isSneaking()) {
                return false;
            }
            return !trigger.contains("OFF_HAND") || isOffHand(event);
        }
        for (final String condition : conditions) {
            if (!matchesCondition(event, condition)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesEvent(final java.util.List<String> events, final boolean right, final boolean left) {
        if (events == null || events.isEmpty()) {
            return right;
        }
        for (final String event : events) {
            final String normalized = normalize(event);
            if (("RIGHT_CLICK".equals(normalized) || "RIGHT".equals(normalized)) && right) {
                return true;
            }
            if (("LEFT_CLICK".equals(normalized) || "LEFT".equals(normalized)) && left) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesCondition(final PlayerInteractEvent event, final String raw) {
        final String condition = normalize(raw);
        final Player player = event.getPlayer();
        if ("SNEAKING".equals(condition) || "SNEAK".equals(condition)) {
            return player.isSneaking();
        }
        if ("NOT_SNEAKING".equals(condition) || "NOT_SNEAK".equals(condition)) {
            return !player.isSneaking();
        }
        if ("MAIN_HAND".equals(condition)) {
            return !isOffHand(event);
        }
        if ("OFF_HAND".equals(condition)) {
            return isOffHand(event);
        }
        if ("ON_GROUND".equals(condition)) {
            return player.isOnGround();
        }
        if ("IN_AIR".equals(condition)) {
            return !player.isOnGround();
        }
        if (condition.startsWith("HAS_PERMISSION:")) {
            return player.hasPermission(raw.substring(raw.indexOf(':') + 1));
        }
        if (condition.startsWith("WORLD_ALLOWLIST:")) {
            return containsCsv(raw.substring(raw.indexOf(':') + 1), player.getWorld().getName());
        }
        if (condition.startsWith("WORLD_DENYLIST:")) {
            return !containsCsv(raw.substring(raw.indexOf(':') + 1), player.getWorld().getName());
        }
        if ("TARGET_BLOCK_EXISTS".equals(condition)) {
            return event.getClickedBlock() != null;
        }
        if ("COOLDOWN_READY".equals(condition) || condition.startsWith("COOLDOWN_READY:")) {
            return true;
        }
        return true;
    }

    private int cooldownSeconds(final WeaponDefinition weapon, final String key, final int fallback) {
        if (weapon == null || weapon.getCooldownsSection() == null || key == null) {
            return fallback;
        }
        final org.bukkit.configuration.ConfigurationSection section = weapon.getCooldownsSection().getConfigurationSection(key);
        return section == null ? fallback : section.getInt("seconds", fallback);
    }

    private ItemStack itemForEvent(final Player player, final PlayerInteractEvent event) {
        if (player == null || event == null || !isOffHand(event)) {
            return player == null ? null : player.getItemInHand();
        }
        try {
            final Object inventory = player.getInventory();
            final Object item = inventory.getClass().getMethod("getItemInOffHand").invoke(inventory);
            return item instanceof ItemStack ? (ItemStack) item : player.getItemInHand();
        } catch (final Exception ignored) {
            return player.getItemInHand();
        }
    }

    private boolean isOffHand(final PlayerInteractEvent event) {
        try {
            final Object hand = event.getClass().getMethod("getHand").invoke(event);
            return hand != null && "OFF_HAND".equals(String.valueOf(hand));
        } catch (final Exception ignored) {
            return false;
        }
    }

    private boolean canUse(final Player player, final WeaponDefinition weapon) {
        if (!this.plugin.getConfig().getBoolean("settings.require-use-permission", false)) {
            return true;
        }
        final String base = "smpweapons.weapon." + weapon.getId() + ".use";
        return player.hasPermission("smpweapons.use") || player.hasPermission(base) || player.hasPermission("smpweapons.weapon.*.use") || player.hasPermission("smpweapons.admin");
    }

    private static boolean containsCsv(final String csv, final String value) {
        if (csv == null || value == null) {
            return false;
        }
        for (final String part : csv.split(",")) {
            if (value.equalsIgnoreCase(part.trim())) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(final String input) {
        return input == null ? "" : input.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private void cleanupPlayer(final Player player) {
        if (player == null) {
            return;
        }
        this.plugin.getFallProtectionService().unprotect(player.getUniqueId());
        if (this.plugin.getGlideService() != null) {
            this.plugin.getGlideService().stop(player);
        } else {
            Entities.setGliding(player, false);
        }
        Entities.setGlowing(player, false);
    }

    private static final class TriggerActivation {
        private final String id;
        private final String timeline;
        private final String cooldownKey;
        private final int cooldownSeconds;

        private TriggerActivation(final String id, final String timeline, final String cooldownKey, final int cooldownSeconds) {
            this.id = id;
            this.timeline = timeline;
            this.cooldownKey = cooldownKey == null || cooldownKey.trim().isEmpty() ? "primary" : cooldownKey;
            this.cooldownSeconds = Math.max(0, cooldownSeconds);
        }
    }

}
