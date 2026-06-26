package com.arkflame.smpweapons.ability;

import com.arkflame.smpweapons.config.WeaponManager;
import com.arkflame.smpweapons.model.WeaponDefinition;
import com.arkflame.smpweapons.util.Entities;
import com.arkflame.smpweapons.util.Particles;
import com.arkflame.smpweapons.util.PlayerItems;
import com.arkflame.smpweapons.util.Sounds;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class ShieldPassiveService {
    private final WeaponManager weaponManager;

    public ShieldPassiveService(final WeaponManager weaponManager) {
        this.weaponManager = weaponManager;
    }

    public void handleDamage(final EntityDamageByEntityEvent event) {
        if (event == null || event.isCancelled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        final Player defender = (Player) event.getEntity();
        if (!PlayerItems.isBlocking(defender)) {
            return;
        }
        final ItemStack mainHand = PlayerItems.mainHand(defender);
        final ItemStack offHand = PlayerItems.offHand(defender);
        final Optional<WeaponDefinition> mainWeapon = mainHand == null
                ? Optional.empty()
                : this.weaponManager.identify(mainHand);
        final Optional<WeaponDefinition> offWeapon = offHand == null
                ? Optional.empty()
                : this.weaponManager.identify(offHand);
        final LivingEntity attacker = resolveAttacker(event);
        if (attacker == null || attacker.getUniqueId().equals(defender.getUniqueId())) {
            return;
        }
        if (mainWeapon.isPresent() && mainWeapon.get().isEnabled()) {
            applyReflectPassives(mainWeapon.get(), event, defender, attacker);
        }
        if (offWeapon.isPresent() && offWeapon.get().isEnabled()) {
            applyReflectPassives(offWeapon.get(), event, defender, attacker);
        }
    }

    private void applyReflectPassives(final WeaponDefinition weapon, final EntityDamageByEntityEvent event,
                                      final Player defender, final LivingEntity attacker) {
        final ConfigurationSection passives = weapon.getPassivesSection();
        if (passives == null) {
            return;
        }
        for (final String key : passives.getKeys(false)) {
            final ConfigurationSection passive = passives.getConfigurationSection(key);
            if (passive == null) {
                continue;
            }
            if (!matchesShieldEvent(passive.getStringList("events"))) {
                continue;
            }
            final double chance = passive.getDouble("chance", 0.0D);
            if (chance <= 0.0D) {
                continue;
            }
            if (ThreadLocalRandom.current().nextDouble(100.0D) >= chance) {
                continue;
            }
            final double percent = passive.getDouble("reflect-percent", 40.0D);
            final double baseDamage = event.getDamage();
            if (baseDamage <= 0.0D) {
                continue;
            }
            final double reflected = Math.max(0.0D, baseDamage * (percent / 100.0D));
            if (reflected <= 0.0D) {
                continue;
            }
            Entities.rawDamage(attacker, reflected);
            final Location location = defender.getLocation();
            final String sound = passive.getString("sound", null);
            if (sound != null && !sound.trim().isEmpty()) {
                Sounds.play(location, sound.trim(), 1.0F, 1.0F);
            }
            final String particle = passive.getString("particle", null);
            if (particle != null && !particle.trim().isEmpty()) {
                Particles.spawn(location, particle.trim(), 12);
            }
        }
    }

    private boolean matchesShieldEvent(final List<String> events) {
        if (events == null || events.isEmpty()) {
            return false;
        }
        for (final String raw : events) {
            final String normalized = normalize(raw);
            if ("SHIELD_BLOCK".equals(normalized) || "BLOCK".equals(normalized) || "DEFEND".equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private LivingEntity resolveAttacker(final EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity) {
            return (LivingEntity) event.getDamager();
        }
        if (event.getDamager() instanceof Projectile) {
            final Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof LivingEntity) {
                return (LivingEntity) projectile.getShooter();
            }
        }
        return null;
    }

    private static String normalize(final String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}