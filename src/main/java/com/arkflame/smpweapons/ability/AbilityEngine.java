package com.arkflame.smpweapons.ability;

import com.arkflame.smpweapons.block.BlockKey;
import com.arkflame.smpweapons.block.TemporaryBlockService;
import com.arkflame.smpweapons.model.WeaponDefinition;
import com.arkflame.smpweapons.projectile.ProjectileService;
import com.arkflame.smpweapons.util.Entities;
import com.arkflame.smpweapons.util.FoliaAPI;
import com.arkflame.smpweapons.util.Materials;
import com.arkflame.smpweapons.util.Particles;
import com.arkflame.smpweapons.util.PotionEffects;
import com.arkflame.smpweapons.util.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AbilityEngine {
    private final FoliaAPI scheduler;
    private final FallProtectionService fallProtection;
    private final CooldownService cooldowns;
    private final TemporaryBlockService temporaryBlocks;
    private final ProjectileService projectileService;
    private final GlideService glideService;
    private final int maxAirLoopTicks;
    private final int maxTargetDistance;
    private final Map<String, Set<UUID>> damageOnceMemory = new HashMap<String, Set<UUID>>();

    public AbilityEngine(final FoliaAPI scheduler, final FallProtectionService fallProtection, final CooldownService cooldowns, final TemporaryBlockService temporaryBlocks, final ProjectileService projectileService, final GlideService glideService, final int maxAirLoopTicks, final int maxTargetDistance) {
        this.scheduler = scheduler;
        this.fallProtection = fallProtection;
        this.cooldowns = cooldowns;
        this.temporaryBlocks = temporaryBlocks;
        this.projectileService = projectileService;
        this.glideService = glideService;
        this.maxAirLoopTicks = Math.max(20, maxAirLoopTicks);
        this.maxTargetDistance = Math.max(4, maxTargetDistance);
    }

    public void execute(final Player player, final WeaponDefinition weapon) {
        if (player == null || weapon == null) {
            return;
        }
        if (weapon.getTriggersSection() == null && weapon.getTriggerTimeline() != null && !weapon.getTriggerTimeline().trim().isEmpty()) {
            timeline(player, weapon, weapon.getTriggerTimeline().trim());
            return;
        }
        final String type = weapon.getAbilityType() == null ? "NONE" : weapon.getAbilityType().toUpperCase(Locale.ROOT);
        final ConfigurationSection section = weapon.getAbilitySection();
        if ("DASH_AOE".equals(type)) {
            dashAoe(player, section);
        } else if ("VENOM_DASH".equals(type)) {
            venomDash(player, section);
        } else if ("COBWEB_FIELD".equals(type)) {
            cobwebField(player, section);
        } else if ("SELF_BUFF".equals(type)) {
            selfBuff(player, section);
        } else if ("GRAVITY_LIFT".equals(type)) {
            gravityLift(player, section);
        } else if ("ROCKET_LIFT".equals(type)) {
            rocketLift(player, section);
        } else if ("SLAM_MACE".equals(type)) {
            slamMace(player, section);
        } else if ("ZOOM_MACE".equals(type)) {
            zoomMace(player, section);
        } else if ("TIDE_TRIDENT".equals(type)) {
            tideTrident(player, section);
        } else if ("EXPLOSIVE_MACE".equals(type)) {
            explosiveMace(player, section);
        } else if ("FLOW_SPEAR".equals(type)) {
            flowSpear(player, section);
        } else if ("COBWEB_PROJECTILE".equals(type)) {
            cobwebProjectile(player, weapon, section);
        } else if ("FORCE_BOW_DASH".equals(type)) {
            forceBowDash(player, section, 1.0D);
        } else if ("TIMELINE".equals(type)) {
            timeline(player, weapon, section);
        }
    }

    public void executeShoot(final Player player, final WeaponDefinition weapon, final double force, final Entity projectile) {
        if (player == null || weapon == null) {
            return;
        }
        final String type = weapon.getAbilityType() == null ? "NONE" : weapon.getAbilityType().toUpperCase(Locale.ROOT);
        final ConfigurationSection section = weapon.getAbilitySection();
        if ("FORCE_BOW_DASH".equals(type)) {
            forceBowDash(player, section, force);
            return;
        }
        if ("TIMELINE".equals(type)) {
            if (section != null && section.isString("timeline")) {
                timeline(player, weapon, section.getString("timeline", "").trim(), new AbilityContext(null, null, projectile));
                return;
            }
            if (section != null && section.isConfigurationSection("timeline")) {
                runTimelineSection(player, weapon, section, section.getConfigurationSection("timeline"), new AbilityContext(null, null, projectile));
                return;
            }
        }
        execute(player, weapon);
    }

    public void executePassive(final Player attacker, final LivingEntity victim, final WeaponDefinition weapon) {
        if (attacker == null || victim == null || weapon == null) {
            return;
        }
        runLegacyPassive(attacker, victim, weapon);
        runDslPassives(attacker, victim, weapon);
    }

    private void runLegacyPassive(final Player attacker, final LivingEntity victim, final WeaponDefinition weapon) {
        if (weapon.getPassiveSection() == null) {
            return;
        }
        final ConfigurationSection passive = weapon.getPassiveSection();
        if (Math.random() * 100.0D > passive.getDouble("chance", 0.0D)) {
            return;
        }
        final String type = weapon.getPassiveType() == null ? "NONE" : weapon.getPassiveType().toUpperCase(Locale.ROOT);
        if ("HIT_EFFECT".equals(type)) {
            for (final String effect : passive.getStringList("effects")) {
                PotionEffects.apply(victim, effect);
            }
        } else if ("COBWEB_ON_HIT".equals(type)) {
            final Material cobweb = Materials.find("COBWEB").orElse(Material.getMaterial("WEB"));
            if (cobweb != null) {
                this.temporaryBlocks.placeReal(victim.getLocation(), cobweb, passive.getInt("duration", 5) * 20L);
            }
        }
    }

    private void runDslPassives(final Player attacker, final LivingEntity victim, final WeaponDefinition weapon) {
        final ConfigurationSection passives = weapon.getPassivesSection();
        if (passives == null) {
            return;
        }
        for (final String key : passives.getKeys(false)) {
            final ConfigurationSection passive = passives.getConfigurationSection(key);
            if (passive == null || !passiveMatches(attacker, passive)) {
                continue;
            }
            if (Math.random() * 100.0D > passive.getDouble("chance", 100.0D)) {
                continue;
            }
            final String timeline = passive.getString("timeline", null);
            if (timeline != null && !timeline.trim().isEmpty()) {
                timeline(attacker, weapon, timeline.trim(), new AbilityContext(victim.getLocation(), victim, null));
                continue;
            }
            for (final String effect : passive.getStringList("effects")) {
                PotionEffects.apply(victim, effect);
            }
            if (passive.isConfigurationSection("temporary-block")) {
                final ConfigurationSection block = passive.getConfigurationSection("temporary-block");
                final Material material = Materials.find(block.getString("block", "COBWEB")).orElse(Material.getMaterial("WEB"));
                if (material != null) {
                    this.temporaryBlocks.placeTemporary(victim.getLocation(), material, block.getLong("ttl-ticks", block.getInt("duration", 5) * 20L), block.getString("mode", "real"));
                }
            }
        }
    }

    private boolean passiveMatches(final Player attacker, final ConfigurationSection passive) {
        final List<String> events = passive.getStringList("events");
        if (!events.isEmpty()) {
            boolean damageDealt = false;
            for (final String event : events) {
                final String normalized = event == null ? "" : event.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
                if ("DAMAGE_DEALT".equals(normalized) || "ATTACK".equals(normalized) || "HIT".equals(normalized)) {
                    damageDealt = true;
                    break;
                }
            }
            if (!damageDealt) {
                return false;
            }
        }
        final List<String> conditions = passive.getStringList("conditions");
        for (final String raw : conditions) {
            final String condition = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            if ("FULL_ATTACK_COOLDOWN".equals(condition) && !fullAttackCooldown(attacker)) {
                return false;
            }
            if (condition.startsWith("HAS_PERMISSION:") && !attacker.hasPermission(raw.substring(raw.indexOf(':') + 1))) {
                return false;
            }
            if ("SNEAKING".equals(condition) && !attacker.isSneaking()) {
                return false;
            }
            if ("NOT_SNEAKING".equals(condition) && attacker.isSneaking()) {
                return false;
            }
        }
        return true;
    }

    private boolean fullAttackCooldown(final Player player) {
        if (player == null) {
            return false;
        }
        try {
            final java.lang.reflect.Method method = player.getClass().getMethod("getAttackCooldown");
            final Object value = method.invoke(player);
            return value instanceof Number && ((Number) value).doubleValue() >= 0.999D;
        } catch (final Exception ignored) {
            return true;
        }
    }

    private void dashAoe(final Player player, final ConfigurationSection section) {
        final double upward = getDouble(section, "upward", 0.5D);
        final double forward = getDouble(section, "forward", 2.0D);
        final double burstRadius = getDouble(section, "burst-radius", 3.0D);
        final double trailRadius = getDouble(section, "trail-radius", 2.0D);
        final double pushSpeed = getDouble(section, "push-speed", 1.0D);
        final double damage = getDouble(section, "damage", 2.0D);
        final String particle = getString(section, "trail-particle", "WHITE_DUST");
        final List<String> sounds = section == null ? new ArrayList<String>() : section.getStringList("burst-sounds");
        final Set<UUID> damaged = new HashSet<UUID>();
        this.fallProtection.protect(player.getUniqueId());
        Entities.push(player, 0.0D, upward, 0.0D);
        scheduler.runEntityLater(player, new Runnable() {
            @Override
            public void run() {
                Entities.pushForward(player, forward);
                for (final String sound : sounds) {
                    Sounds.play(player.getLocation(), sound, 1.0F, 1.0F);
                }
                burstDamage(player, burstRadius, pushSpeed, damage, damaged);
                startAirTrail(player, trailRadius, pushSpeed, damage, particle, damaged, true);
            }
        }, null, 1L);
    }

    private void venomDash(final Player player, final ConfigurationSection section) {
        final double upward = getDouble(section, "upward", 0.5D);
        final double forward = getDouble(section, "forward", 2.2D);
        final double radius = getDouble(section, "radius", 5.0D);
        final int poison = getInt(section, "poison-amplifier", 1);
        final int weakness = getInt(section, "weakness-amplifier", 1);
        final int duration = getInt(section, "duration", 5);
        final Set<UUID> affected = new HashSet<UUID>();
        this.fallProtection.protect(player.getUniqueId());
        Entities.push(player, 0.0D, upward, 0.0D);
        scheduler.runEntityLater(player, new Runnable() {
            @Override
            public void run() {
                Entities.pushForward(player, forward);
                Sounds.play(player.getLocation(), "ENTITY_BREEZE_CHARGE", 1.0F, 1.0F);
                Sounds.play(player.getLocation(), "ENTITY_WIND_CHARGE_WIND_BURST", 1.0F, 1.0F);
                venomTrail(player, radius, poison, weakness, duration, affected, 0);
            }
        }, null, 1L);
    }

    private void cobwebField(final Player player, final ConfigurationSection section) {
        final int radius = getInt(section, "radius", 6);
        final int distance = getInt(section, "target-distance", 12);
        final long duration = getInt(section, "duration-ticks", 35);
        final String mode = getString(section, "block-mode", "real");
        final int realRadius = getInt(section, "real-radius", radius);
        final Location target = targetLocation(player, Math.min(distance, this.maxTargetDistance));
        final Material cobweb = Materials.find("COBWEB").orElse(Material.getMaterial("WEB"));
        if (cobweb == null) {
            return;
        }
        drawLine(player.getEyeLocation(), target, "WHITE_DUST");
        Sounds.play(target, "ENTITY_SPIDER_DEATH", 1.0F, 1.0F);
        this.temporaryBlocks.waveSphere(target, cobweb, radius, duration, mode, realRadius, 2L, getInt(section, "collapse-delay-ticks", 2));
    }

    private void selfBuff(final Player player, final ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (final String sound : section.getStringList("sounds")) {
            Sounds.play(player.getLocation(), sound, 1.0F, 1.0F);
        }
        for (final String effect : section.getStringList("effects")) {
            PotionEffects.apply(player, effect);
        }
        final boolean glowing = section.getBoolean("glowing", false);
        final int duration = section.getInt("duration", 6);
        if (glowing) {
            Entities.setGlowing(player, true);
            scheduler.runEntityLater(player, new Runnable() {
                @Override
                public void run() {
                    Entities.setGlowing(player, false);
                }
            }, null, Math.max(1L, duration * 20L));
        }
        particleAura(player, section.getString("particle", "RED_DUST"), Math.max(20, duration * 20), 2L);
    }

    private void gravityLift(final Player player, final ConfigurationSection section) {
        final double radius = getDouble(section, "radius", 6.0D);
        final String particle = getString(section, "particle", "PURPLE_DUST");
        final List<LivingEntity> victims = livingNearby(player, radius);
        for (final LivingEntity victim : victims) {
            for (final String effect : section.getStringList("effects")) {
                PotionEffects.apply(victim, effect);
            }
        }
        ring(player.getLocation(), particle, 15, 1);
        for (final LivingEntity victim : victims) {
            particleAura(victim, particle, 60, 2L);
        }
    }

    private void rocketLift(final Player player, final ConfigurationSection section) {
        final double upward = getDouble(section, "upward", 2.0D);
        final double forward = getDouble(section, "forward", 1.0D);
        final int duration = getInt(section, "duration", 3);
        final String particle = getString(section, "particle", "YELLOW_DUST");
        final boolean glidingAtPeak = section == null || section.getBoolean("gliding-at-peak", section.getBoolean("glide-at-peak", true));
        final int glideSustainTicks = getInt(section, "glide-sustain-ticks", 100);
        final double peakVelocityY = getDouble(section, "glide-start-velocity-y", 0.05D);
        this.fallProtection.protect(player.getUniqueId());
        Entities.push(player, 0.0D, upward, 0.0D);
        Entities.pushForward(player, forward);
        Sounds.play(player.getLocation(), "ENTITY_FIREWORK_ROCKET_LAUNCH", 1.0F, 1.0F);
        rocketTrail(player, particle, duration * 20, 0, glidingAtPeak, glideSustainTicks, peakVelocityY, false);
    }

    private void slamMace(final Player player, final ConfigurationSection section) {
        final double upward = getDouble(section, "upward", 1.6D);
        final double downward = getDouble(section, "downward", 3.0D);
        final int clearRadius = getInt(section, "clear-web-radius", 7);
        Entities.push(player, 0.0D, upward, 0.0D);
        Sounds.play(player, "ENTITY_ENDER_DRAGON_FLAP", 1.0F, 1.0F);
        scheduler.runEntityLater(player, new Runnable() {
            @Override
            public void run() {
                Entities.push(player, 0.0D, -downward, 0.0D);
                waitForGround(player, new Runnable() {
                    @Override
                    public void run() {
                        Sounds.play(player.getLocation(), "ENTITY_GENERIC_EXPLODE", 1.0F, 1.0F);
                        Particles.spawn(player.getLocation(), "EXPLOSION", 30);
                        clearCobwebs(player.getLocation(), clearRadius);
                    }
                }, 0, 100);
            }
        }, null, 12L);
    }

    private void zoomMace(final Player player, final ConfigurationSection section) {
        final double upward = getDouble(section, "upward", 5.0D);
        final double downward = getDouble(section, "downward", 4.0D);
        final double radius = getDouble(section, "radius", 6.0D);
        final double damage = getDouble(section, "damage", 8.0D);
        this.fallProtection.protect(player.getUniqueId());
        Entities.push(player, 0.0D, upward, 0.0D);
        Sounds.play(player, "ENTITY_WITHER_SHOOT", 1.0F, 1.0F);
        scheduler.runEntityLater(player, new Runnable() {
            @Override
            public void run() {
                Entities.push(player, 0.0D, -downward, 0.0D);
                waitForGround(player, new Runnable() {
                    @Override
                    public void run() {
                        Sounds.play(player.getLocation(), "ENTITY_GENERIC_EXPLODE", 1.0F, 1.0F);
                        Sounds.play(player.getLocation(), "ENTITY_IRON_GOLEM_DEATH", 1.0F, 1.0F);
                        for (final LivingEntity entity : livingNearby(player, radius)) {
                            entity.damage(damage, player);
                            Particles.spawn(entity.getLocation(), "CRIT", 10);
                        }
                        scheduler.runEntityLater(player, new Runnable() {
                            @Override
                            public void run() {
                                fallProtection.unprotect(player.getUniqueId());
                            }
                        }, null, 2L);
                    }
                }, 0, 200);
            }
        }, null, 10L);
    }

    private void tideTrident(final Player player, final ConfigurationSection section) {
        final double radius = getDouble(section, "radius", 7.0D);
        final double forward = getDouble(section, "forward", 3.5D);
        final String effect = getString(section, "effect", "WITHER:1:5");
        for (final LivingEntity entity : livingNearby(player, radius)) {
            if (entity instanceof Player) {
                PotionEffects.apply(entity, effect);
            }
        }
        Sounds.play(player, "ENTITY_DOLPHIN_SPLASH", 1.0F, 1.0F);
        Entities.pushForward(player, forward);
        waterTrail(player, 20, 0);
    }

    private void explosiveMace(final Player player, final ConfigurationSection section) {
        if (section == null) {
            return;
        }
        Sounds.play(player, "ENTITY_ENDER_DRAGON_GROWL", 1.0F, 1.0F);
        Particles.spawn(player.getLocation(), "LAVA", 40);
        for (final String effect : section.getStringList("effects")) {
            PotionEffects.apply(player, effect);
        }
        for (final LivingEntity entity : livingNearby(player, section.getDouble("radius", 5.0D))) {
            if (entity instanceof Player) {
                for (final String effect : section.getStringList("victim-effects")) {
                    PotionEffects.apply(entity, effect);
                }
                Sounds.play(entity.getLocation(), "ENTITY_SHULKER_SHOOT", 1.0F, 1.0F);
            }
        }
    }

    private void flowSpear(final Player player, final ConfigurationSection section) {
        Sounds.play(player, "ENTITY_PLAYER_ATTACK_SWEEP", 1.0F, 1.0F);
        Entities.push(player, 0.0D, getDouble(section, "upward", 0.4D), 0.0D);
        Entities.pushForward(player, getDouble(section, "forward", 4.0D));
    }

    private void forceBowDash(final Player player, final ConfigurationSection section, final double force) {
        final double minForward = getDouble(section, "min-forward", 1.2D);
        final double maxForward = getDouble(section, "max-forward", 4.2D);
        final double upward = getDouble(section, "upward", 0.35D);
        final int fallProtectionTicks = getInt(section, "fall-protection-ticks", 80);
        final String trailParticle = getString(section, "trail-particle", "CLOUD");
        final int trailTicks = getInt(section, "trail-ticks", 8);
        final double clamped = Math.max(0.0D, Math.min(1.0D, force));
        final double forward = minForward + ((maxForward - minForward) * clamped);
        final Vector direction = player.getEyeLocation().getDirection().clone().setY(0.0D);
        if (direction.lengthSquared() > 0.000001D) {
            direction.normalize().multiply(forward).setY(upward);
            player.setVelocity(player.getVelocity().add(direction));
        } else if (Math.abs(upward) > 0.000001D) {
            player.setVelocity(player.getVelocity().add(new Vector(0.0D, upward, 0.0D)));
        }
        if (fallProtectionTicks > 0) {
            this.fallProtection.protect(player.getUniqueId());
            this.scheduler.runEntityLater(player, new Runnable() {
                @Override
                public void run() {
                    fallProtection.unprotect(player.getUniqueId());
                }
            }, null, fallProtectionTicks);
        }
        forceBowTrail(player, trailParticle, Math.max(0, trailTicks), 0);
    }

    private void forceBowTrail(final Player player, final String particle, final int maxTicks, final int tick) {
        if (player == null || !player.isOnline() || tick >= maxTicks) {
            return;
        }
        Particles.spawn(player.getLocation().add(0.0D, 0.8D, 0.0D), particle, 2);
        this.scheduler.runEntityLater(player, new Runnable() {
            @Override
            public void run() {
                forceBowTrail(player, particle, maxTicks, tick + 1);
            }
        }, null, 1L);
    }

    private void cobwebProjectile(final Player player, final WeaponDefinition weapon, final ConfigurationSection section) {
        if (this.projectileService != null) {
            this.projectileService.launchCobwebBomb(player, weapon, section);
        }
    }

    public void executeNamedTimeline(final Player player, final WeaponDefinition weapon, final String timelineName, final Location impactLocation, final LivingEntity hitEntity, final Entity projectile) {
        if (player == null || weapon == null || timelineName == null || timelineName.trim().isEmpty()) {
            return;
        }
        timeline(player, weapon, timelineName.trim(), new AbilityContext(impactLocation, hitEntity, projectile));
    }

    private void timeline(final Player player, final WeaponDefinition weapon, final String timelineName) {
        timeline(player, weapon, timelineName, AbilityContext.empty());
    }

    private void timeline(final Player player, final WeaponDefinition weapon, final String timelineName, final AbilityContext context) {
        if (player == null || weapon == null || timelineName == null) {
            return;
        }
        final ConfigurationSection rootTimelines = weapon.getTimelinesSection();
        if (rootTimelines != null) {
            final ConfigurationSection named = rootTimelines.getConfigurationSection(timelineName);
            if (named != null) {
                runTimelineSection(player, weapon, weapon.getAbilitySection(), named, context);
                return;
            }
        }
        final ConfigurationSection ability = weapon.getAbilitySection();
        if (ability != null) {
            final ConfigurationSection embeddedTimelines = ability.getConfigurationSection("timelines");
            if (embeddedTimelines != null) {
                final ConfigurationSection named = embeddedTimelines.getConfigurationSection(timelineName);
                if (named != null) {
                    runTimelineSection(player, weapon, ability, named, context);
                    return;
                }
            }
        }
    }

    private void timeline(final Player player, final WeaponDefinition weapon, final ConfigurationSection section) {
        if (player == null || section == null) {
            return;
        }
        final ConfigurationSection namedTimelines = section.getConfigurationSection("timelines");
        if (namedTimelines != null && section.isString("timeline")) {
            final ConfigurationSection named = namedTimelines.getConfigurationSection(section.getString("timeline"));
            if (named != null) {
                runTimelineSection(player, weapon, section, named);
                return;
            }
        }
        final ConfigurationSection timeline = section.getConfigurationSection("timeline");
        if (timeline == null) {
            return;
        }
        runTimelineSection(player, weapon, section, timeline);
    }

    private void runTimelineSection(final Player player, final WeaponDefinition weapon, final ConfigurationSection section, final ConfigurationSection timeline) {
        runTimelineSection(player, weapon, section, timeline, AbilityContext.empty());
    }

    private void runTimelineSection(final Player player, final WeaponDefinition weapon, final ConfigurationSection section, final ConfigurationSection timeline, final AbilityContext context) {
        for (final String tickKey : timeline.getKeys(false)) {
            final List<Integer> delays = parseScheduleTicks(tickKey);
            final List<?> actions = timeline.getList(tickKey);
            if (actions == null || actions.isEmpty() || delays.isEmpty()) {
                continue;
            }
            for (final Integer boxedDelay : delays) {
                final int delay = boxedDelay == null ? 0 : boxedDelay.intValue();
                scheduler.runEntityLater(player, new Runnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) {
                            return;
                        }
                        for (final Object action : actions) {
                            runTimelineAction(player, weapon, section, action, context == null ? AbilityContext.empty() : context);
                        }
                    }
                }, null, Math.max(0L, delay));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void runTimelineAction(final Player player, final WeaponDefinition weapon, final ConfigurationSection section, final Object action, final AbilityContext context) {
        if (action == null || player == null) {
            return;
        }
        if (action instanceof String) {
            runTimelineStringAction(player, (String) action);
            return;
        }
        if (!(action instanceof java.util.Map)) {
            return;
        }
        final java.util.Map<Object, Object> map = (java.util.Map<Object, Object>) action;
        if (map.containsKey("stop_if")) {
            final java.util.Map<Object, Object> data = asMap(map.get("stop_if"));
            if (condition(player, string(data, "condition", ""))) {
                return;
            }
        }
        if (map.containsKey("sound")) {
            final java.util.Map<Object, Object> data = asMap(map.get("sound"));
            Sounds.play(resolveOrigin(player, data.get("target"), context), string(data, "name", "ENTITY_PLAYER_ATTACK_SWEEP"), (float) number(data, "volume", 1.0D), (float) number(data, "pitch", 1.0D));
            return;
        }
        if (map.containsKey("particle")) {
            final java.util.Map<Object, Object> data = asMap(map.get("particle"));
            Particles.spawn(resolveOrigin(player, data.get("origin"), context), string(data, "effect", string(data, "type", "CLOUD")), integer(data, "count", 1));
            return;
        }
        if (map.containsKey("particle_shape") || map.containsKey("particle-shape")) {
            final java.util.Map<Object, Object> data = asMap(map.containsKey("particle_shape") ? map.get("particle_shape") : map.get("particle-shape"));
            particleShape(resolveOrigin(player, data.get("origin"), context), string(data, "effect", string(data, "particle", "CLOUD")), asMap(data.get("shape")), integer(data, "count", 1));
            return;
        }
        if (map.containsKey("beam")) {
            beam(player, asMap(map.get("beam")));
            return;
        }
        if (map.containsKey("pull")) {
            pull(player, asMap(map.get("pull")), 0);
            return;
        }
        if (map.containsKey("velocity")) {
            final java.util.Map<Object, Object> data = asMap(map.get("velocity"));
            final List<LivingEntity> targets = resolveTargets(player, data, "caster", context);
            final String mode = string(data, "mode", "ADD").toUpperCase(Locale.ROOT).replace('-', '_');
            for (final LivingEntity target : targets) {
                if ("LOOK".equals(mode) && target instanceof Player) {
                    Entities.pushForward((Player) target, number(data, "forward", 1.0D));
                    final double upward = number(data, "upward", 0.0D);
                    if (Math.abs(upward) > 0.000001D) {
                        Entities.push(target, 0.0D, upward, 0.0D);
                    }
                } else if ("AWAY_FROM_CASTER".equals(mode) || "AWAY".equals(mode)) {
                    Entities.pushAway(target, player.getLocation(), number(data, "speed", number(data, "force", 1.0D)));
                } else {
                    Entities.push(target, number(data, "x", 0.0D), number(data, "y", number(data, "upward", 0.0D)), number(data, "z", 0.0D));
                    final double forward = number(data, "forward", 0.0D);
                    if (Math.abs(forward) > 0.000001D && target instanceof Player) {
                        Entities.pushForward((Player) target, forward);
                    }
                }
            }
            return;
        }
        if (map.containsKey("damage") || map.containsKey("raw_damage") || map.containsKey("raw-damage") || map.containsKey("damage_once") || map.containsKey("damage-once")) {
            final Object node = map.containsKey("damage") ? map.get("damage") : (map.containsKey("raw_damage") ? map.get("raw_damage") : (map.containsKey("raw-damage") ? map.get("raw-damage") : (map.containsKey("damage_once") ? map.get("damage_once") : map.get("damage-once"))));
            final java.util.Map<Object, Object> data = asMap(node);
            final boolean damageOnce = map.containsKey("damage_once") || map.containsKey("damage-once");
            final boolean raw = map.containsKey("raw_damage") || map.containsKey("raw-damage") || "RAW_HEALTH".equalsIgnoreCase(string(data, "mode", ""));
            final double amount = number(data, "amount", number(data, "damage", 1.0D));
            final String memory = player.getUniqueId().toString() + ':' + (weapon == null ? "" : weapon.getId()) + ':' + string(data, "memory", "default");
            Set<UUID> remembered = this.damageOnceMemory.get(memory);
            if (damageOnce && remembered == null) {
                remembered = new HashSet<UUID>();
                this.damageOnceMemory.put(memory, remembered);
            }
            for (final LivingEntity target : resolveTargets(player, data, "nearby_living", context)) {
                if (target == player && !Boolean.valueOf(string(data, "include-caster", "false")).booleanValue()) {
                    continue;
                }
                if (damageOnce && remembered != null && !remembered.add(target.getUniqueId())) {
                    continue;
                }
                if (raw) {
                    Entities.rawDamage(target, amount);
                } else {
                    target.damage(amount, player);
                }
            }
            return;
        }
        if (map.containsKey("potion")) {
            final java.util.Map<Object, Object> data = asMap(map.get("potion"));
            final Object effects = data.get("effects");
            final List<LivingEntity> targets = resolveTargets(player, data, "caster", context);
            for (final LivingEntity target : targets) {
                if (effects instanceof java.util.List) {
                    for (final Object effect : (java.util.List<?>) effects) {
                        PotionEffects.apply(target, String.valueOf(effect));
                    }
                } else {
                    PotionEffects.apply(target, string(data, "effect", "SPEED:1:1"));
                }
            }
            return;
        }
        if (map.containsKey("clear_effect") || map.containsKey("clear-effect")) {
            final java.util.Map<Object, Object> data = asMap(map.containsKey("clear_effect") ? map.get("clear_effect") : map.get("clear-effect"));
            final Object rawEffects = data.get("effects");
            final List<String> effects = stringList(rawEffects, string(data, "effect", ""));
            for (final LivingEntity target : resolveTargets(player, data, "caster", context)) {
                for (final String effect : effects) {
                    final java.util.Optional<PotionEffectType> type = PotionEffects.resolve(effect);
                    if (type.isPresent()) {
                        target.removePotionEffect(type.get());
                    }
                }
            }
            return;
        }
        if (map.containsKey("fall_protection")) {
            final java.util.Map<Object, Object> data = asMap(map.get("fall_protection"));
            if (Boolean.valueOf(String.valueOf(data.containsKey("enabled") ? data.get("enabled") : "true")).booleanValue()) {
                this.fallProtection.protect(player.getUniqueId());
            } else {
                this.fallProtection.unprotect(player.getUniqueId());
            }
            return;
        }
        if (map.containsKey("gliding")) {
            final java.util.Map<Object, Object> data = asMap(map.get("gliding"));
            final boolean enabled = Boolean.valueOf(String.valueOf(data.containsKey("enabled") ? data.get("enabled") : "true")).booleanValue();
            for (final LivingEntity target : resolveTargets(player, data, "caster", context)) {
                if (target instanceof Player) {
                    Entities.setGliding((Player) target, enabled);
                }
            }
            return;
        }
        if (map.containsKey("glowing")) {
            final java.util.Map<Object, Object> data = asMap(map.get("glowing"));
            final boolean enabled = Boolean.valueOf(String.valueOf(data.containsKey("enabled") ? data.get("enabled") : "true")).booleanValue();
            for (final LivingEntity target : resolveTargets(player, data, "caster", context)) {
                Entities.setGlowing(target, enabled);
            }
            return;
        }
        if (map.containsKey("temporary_block") || map.containsKey("shape_block")) {
            final java.util.Map<Object, Object> data = asMap(map.containsKey("temporary_block") ? map.get("temporary_block") : map.get("shape_block"));
            final Material material = Materials.find(string(data, "block", "COBWEB")).orElse(Material.getMaterial("WEB"));
            if (material != null) {
                final java.util.Map<Object, Object> shape = asMap(data.get("shape"));
                if (!shape.isEmpty()) {
                    final String fill = string(shape, "fill", "SOLID").toUpperCase(Locale.ROOT);
                    this.temporaryBlocks.placeShape(resolveOrigin(player, data.get("origin"), context), material, string(shape, "type", "SPHERE"), integer(shape, "radius", integer(shape, "radius-to", 3)), integer(shape, "height", 3), "HOLLOW".equals(fill) || "SURFACE".equals(fill) || "OUTLINE".equals(fill), integer(data, "ttl-ticks", 40), string(data, "mode", "real"));
                } else {
                    this.temporaryBlocks.placeTemporary(resolveOrigin(player, data.get("origin"), context), material, integer(data, "ttl-ticks", 40), string(data, "mode", "real"));
                }
            }
            return;
        }
        if (map.containsKey("virtual_block") || map.containsKey("virtual-block")) {
            final java.util.Map<Object, Object> data = asMap(map.containsKey("virtual_block") ? map.get("virtual_block") : map.get("virtual-block"));
            final Material material = Materials.find(string(data, "block", "COBWEB")).orElse(Material.getMaterial("WEB"));
            if (material != null) {
                final java.util.Map<Object, Object> shape = asMap(data.get("shape"));
                if (!shape.isEmpty()) {
                    final String fill = string(shape, "fill", "SOLID").toUpperCase(Locale.ROOT);
                    this.temporaryBlocks.placeShape(resolveOrigin(player, data.get("origin"), context), material, string(shape, "type", "SPHERE"), integer(shape, "radius", integer(shape, "radius-to", 3)), integer(shape, "height", 3), "HOLLOW".equals(fill) || "SURFACE".equals(fill) || "OUTLINE".equals(fill), integer(data, "ttl-ticks", 40), "fake");
                } else {
                    this.temporaryBlocks.placeTemporary(resolveOrigin(player, data.get("origin"), context), material, integer(data, "ttl-ticks", 40), "fake");
                }
            }
            return;
        }
        if (map.containsKey("shape_block_wave")) {
            final java.util.Map<Object, Object> data = asMap(map.get("shape_block_wave"));
            final Material material = Materials.find(string(data, "block", "COBWEB")).orElse(Material.getMaterial("WEB"));
            if (material != null) {
                this.temporaryBlocks.waveSphere(resolveOrigin(player, data.get("origin"), context), material, integer(data, "radius", integer(asMap(data.get("shape")), "radius-to", 3)), integer(data, "ttl-ticks", 40), string(data, "mode", "hybrid"), integer(asMap(data.get("collision")), "real-radius", 1), integer(asMap(data.get("expand")), "every-ticks", 2), integer(asMap(data.get("collapse")), "delay-after-expand-ticks", 4));
            }
            return;
        }
        if (map.containsKey("clear_blocks")) {
            final java.util.Map<Object, Object> data = asMap(map.get("clear_blocks"));
            clearCobwebs(resolveOrigin(player, data.get("origin"), context), integer(data, "radius", 5));
            return;
        }
        if (map.containsKey("message") || map.containsKey("actionbar") || map.containsKey("title")) {
            final Object node = map.containsKey("message") ? map.get("message") : (map.containsKey("actionbar") ? map.get("actionbar") : map.get("title"));
            final java.util.Map<Object, Object> data = asMap(node);
            final String text = string(data, "text", string(data, "message", ""));
            final String type = map.containsKey("actionbar") ? "actionbar" : (map.containsKey("title") ? "title" : "message");
            deliverText(player, type, text);
            return;
        }
        if (map.containsKey("command")) {
            final java.util.Map<Object, Object> data = asMap(map.get("command"));
            final String value = string(data, "value", string(data, "command", ""));
            if (!value.trim().isEmpty()) {
                final String rendered = replacePlaceholders(value, player, weapon);
                if ("console".equalsIgnoreCase(string(data, "sender", "console"))) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rendered.startsWith("/") ? rendered.substring(1) : rendered);
                } else {
                    player.performCommand(rendered.startsWith("/") ? rendered.substring(1) : rendered);
                }
            }
            return;
        }
        if (map.containsKey("restore_block") || map.containsKey("restore-block")) {
            final java.util.Map<Object, Object> data = asMap(map.containsKey("restore_block") ? map.get("restore_block") : map.get("restore-block"));
            this.temporaryBlocks.restore(BlockKey.from(resolveOrigin(player, data.get("origin"), context)));
            return;
        }
        if (map.containsKey("sync_block") || map.containsKey("sync-block")) {
            final java.util.Map<Object, Object> data = asMap(map.containsKey("sync_block") ? map.get("sync_block") : map.get("sync-block"));
            this.temporaryBlocks.syncBlock(player, resolveOrigin(player, data.get("origin"), context));
            return;
        }
        if (map.containsKey("cooldown_reset") || map.containsKey("cooldown-reset")) {
            if (this.cooldowns != null) {
                this.cooldowns.reset(player, weapon);
            }
            return;
        }
        if (map.containsKey("cooldown_set") || map.containsKey("cooldown-set")) {
            if (this.cooldowns != null) {
                this.cooldowns.start(player, weapon, false);
            }
            return;
        }
        if (map.containsKey("remove_projectile") || map.containsKey("remove-projectile")) {
            if (this.projectileService != null) {
                this.projectileService.clearOwned(player.getUniqueId());
            }
            return;
        }
        if (map.containsKey("spawn_projectile") && this.projectileService != null) {
            final java.util.Map<Object, Object> data = asMap(map.get("spawn_projectile"));
            this.projectileService.launchConfigured(player, weapon, section, string(data, "id", "cobweb_bomb"));
        }
    }

    private void beam(final Player player, final java.util.Map<Object, Object> data) {
        final Location origin = player.getEyeLocation();
        final Vector direction = origin.getDirection();
        if (direction.lengthSquared() <= 0.000001D) {
            return;
        }
        direction.normalize();
        final String particle = string(data, "particle", string(data, "trail-particle", "CLOUD"));
        final String sound = string(data, "sound", "");
        final double maxDistance = Math.max(0.1D, number(data, "max-distance", 24.0D));
        final double step = Math.max(0.1D, number(data, "step", 0.5D));
        final double radius = Math.max(0.05D, number(data, "radius", 0.8D));
        final double radiusSquared = radius * radius;
        final double damage = number(data, "damage", number(data, "raw-damage", 0.0D));
        final boolean rawDamage = data.containsKey("raw-damage") || Boolean.valueOf(string(data, "raw", "false")).booleanValue();
        final boolean playersOnly = Boolean.valueOf(string(data, "players-only", "false")).booleanValue();
        final boolean lineOfSight = Boolean.valueOf(string(data, "line-of-sight", "false")).booleanValue();
        if (!sound.trim().isEmpty()) {
            Sounds.play(origin, sound, 1.0F, 1.0F);
        }
        LivingEntity hit = null;
        for (double distance = 0.0D; distance <= maxDistance; distance += step) {
            final Location point = origin.clone().add(direction.clone().multiply(distance));
            Particles.spawn(point, particle, 1);
            for (final Entity entity : player.getNearbyEntities(maxDistance, maxDistance, maxDistance)) {
                if (!(entity instanceof LivingEntity) || entity == player || entity.isDead()) {
                    continue;
                }
                if (playersOnly && !(entity instanceof Player)) {
                    continue;
                }
                if (lineOfSight && !player.hasLineOfSight(entity)) {
                    continue;
                }
                if (entity.getLocation().add(0.0D, 0.9D, 0.0D).distanceSquared(point) <= radiusSquared) {
                    hit = (LivingEntity) entity;
                    break;
                }
            }
            if (hit != null) {
                if (damage > 0.0D) {
                    if (rawDamage) {
                        Entities.rawDamage(hit, damage);
                    } else {
                        hit.damage(damage, player);
                    }
                }
                for (final String effect : stringList(data.get("hit-effects"), "")) {
                    PotionEffects.apply(hit, effect);
                }
                return;
            }
        }
    }

    private void pull(final Player player, final java.util.Map<Object, Object> data, final int elapsedTicks) {
        if (player == null || !player.isOnline()) {
            return;
        }
        final int durationTicks = Math.max(0, integer(data, "duration-ticks", 80));
        if (elapsedTicks >= durationTicks) {
            return;
        }
        final int periodTicks = Math.max(1, integer(data, "period-ticks", 1));
        final double radius = Math.max(0.1D, number(data, "radius", 7.0D));
        final double speed = number(data, "speed", 0.42D);
        final double vertical = number(data, "vertical", 0.08D);
        final String particle = string(data, "particle", "REVERSE_PORTAL");
        final int particleCount = Math.max(0, integer(data, "particle-count", 6));
        final String sound = string(data, "sound", "BLOCK_BEACON_ACTIVATE");
        final boolean playersOnly = Boolean.valueOf(string(data, "players-only", "true")).booleanValue();
        if (elapsedTicks == 0 && !sound.trim().isEmpty()) {
            Sounds.play(player.getLocation(), sound, 1.0F, 1.0F);
        }
        final Location casterLocation = player.getLocation();
        Particles.spawn(casterLocation.clone().add(0.0D, 1.0D, 0.0D), particle, particleCount);
        for (final Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity) || entity == player || entity.isDead()) {
                continue;
            }
            if (playersOnly && !(entity instanceof Player)) {
                continue;
            }
            final Vector vector = casterLocation.toVector().subtract(entity.getLocation().toVector());
            if (vector.lengthSquared() <= 0.0625D) {
                continue;
            }
            vector.setY(0.0D);
            if (vector.lengthSquared() <= 0.000001D) {
                continue;
            }
            vector.normalize().multiply(speed).setY(vertical);
            entity.setVelocity(entity.getVelocity().add(vector));
            Particles.spawn(entity.getLocation().add(0.0D, 1.0D, 0.0D), particle, particleCount);
        }
        this.scheduler.runEntityLater(player, new Runnable() {
            @Override
            public void run() {
                pull(player, data, elapsedTicks + periodTicks);
            }
        }, null, periodTicks);
    }

    private void runTimelineStringAction(final Player player, final String action) {
        if (action == null) {
            return;
        }
        final String[] parts = action.split(":");
        if (parts.length == 0) {
            return;
        }
        final String type = parts[0].trim().toUpperCase(Locale.ROOT);
        if ("SOUND".equals(type) && parts.length >= 2) {
            Sounds.play(player, parts[1], 1.0F, 1.0F);
        } else if ("PARTICLE".equals(type) && parts.length >= 2) {
            Particles.spawn(player.getLocation(), parts[1], parts.length >= 3 ? parseInt(parts[2], 1) : 1);
        } else if ("VELOCITY_LOOK".equals(type) && parts.length >= 2) {
            Entities.pushForward(player, parseDouble(parts[1], 1.0D));
        } else if ("VELOCITY_Y".equals(type) && parts.length >= 2) {
            Entities.push(player, 0.0D, parseDouble(parts[1], 0.0D), 0.0D);
        } else if ("POTION".equals(type) && parts.length >= 4) {
            PotionEffects.apply(player, parts[1], parseInt(parts[2], 1), parseInt(parts[3], 1));
        } else if ("TEMP_BLOCK".equals(type) && parts.length >= 3) {
            final Material material = Materials.find(parts[1]).orElse(Material.getMaterial("WEB"));
            if (material != null) {
                this.temporaryBlocks.placeTemporary(player.getLocation(), material, parseInt(parts[2], 40), parts.length >= 4 ? parts[3] : "real");
            }
        } else if ("SHAPE_WAVE".equals(type) && parts.length >= 4) {
            final Material material = Materials.find(parts[1]).orElse(Material.getMaterial("WEB"));
            if (material != null) {
                this.temporaryBlocks.waveSphere(targetLocation(player, this.maxTargetDistance), material, parseInt(parts[2], 3), parseInt(parts[3], 40), parts.length >= 5 ? parts[4] : "hybrid", 1, 2L, 4L);
            }
        }
    }

    private Location resolveOrigin(final Player player, final Object raw) {
        return resolveOrigin(player, raw, AbilityContext.empty());
    }

    private Location resolveOrigin(final Player player, final Object raw, final AbilityContext context) {
        if (raw instanceof java.util.Map) {
            final java.util.Map<Object, Object> map = asMap(raw);
            final Location base = resolveOrigin(player, map.get("type") == null ? map.get("origin") : map.get("type"), context);
            return base.clone().add(number(map, "x", 0.0D), number(map, "y", 0.0D), number(map, "z", 0.0D));
        }
        final String value = raw == null ? "caster" : String.valueOf(raw).toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (("victim".equals(value) || "hit_entity".equals(value) || "target_entity".equals(value)) && context != null && context.hitEntity != null) {
            return context.hitEntity.getLocation();
        }
        if (("projectile".equals(value) || "projectile_location".equals(value)) && context != null && context.projectile != null) {
            return context.projectile.getLocation();
        }
        if (("impact_location".equals(value) || "impact".equals(value)) && context != null && context.impactLocation != null) {
            return context.impactLocation.clone();
        }
        if ("target_block".equals(value) || "target_location".equals(value)) {
            return targetLocation(player, this.maxTargetDistance);
        }
        if ("eye".equals(value)) {
            return player.getEyeLocation();
        }
        return player.getLocation();
    }

    @SuppressWarnings("unchecked")
    private static java.util.Map<Object, Object> asMap(final Object raw) {
        if (raw instanceof java.util.Map) {
            return (java.util.Map<Object, Object>) raw;
        }
        return new java.util.HashMap<Object, Object>();
    }

    private static String string(final java.util.Map<Object, Object> map, final String key, final String fallback) {
        final Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static int integer(final java.util.Map<Object, Object> map, final String key, final int fallback) {
        final Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return value == null ? fallback : parseInt(String.valueOf(value), fallback);
    }

    private static double number(final java.util.Map<Object, Object> map, final String key, final double fallback) {
        final Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return value == null ? fallback : parseDouble(String.valueOf(value), fallback);
    }

    private void startAirTrail(final Player player, final double radius, final double pushSpeed, final double damage, final String particle, final Set<UUID> damaged, final boolean clearNoFallOnGround) {
        airTrailTick(player, radius, pushSpeed, damage, particle, damaged, clearNoFallOnGround, 0);
    }

    private void airTrailTick(final Player player, final double radius, final double pushSpeed, final double damage, final String particle, final Set<UUID> damaged, final boolean clearNoFallOnGround, final int tick) {
        if (!player.isOnline() || tick >= this.maxAirLoopTicks || player.isOnGround()) {
            if (clearNoFallOnGround) {
                scheduler.runEntityLater(player, new Runnable() {
                    @Override
                    public void run() {
                        fallProtection.unprotect(player.getUniqueId());
                    }
                }, null, 2L);
            }
            return;
        }
        Particles.spawn(player.getLocation(), particle, 1);
        burstDamage(player, radius, pushSpeed, damage, damaged);
        scheduler.runEntityLater(player, new Runnable() {
            @Override
            public void run() {
                airTrailTick(player, radius, pushSpeed, damage, particle, damaged, clearNoFallOnGround, tick + 1);
            }
        }, null, 1L);
    }

    private void venomTrail(final Player player, final double radius, final int poison, final int weakness, final int duration, final Set<UUID> affected, final int tick) {
        if (!player.isOnline() || tick >= this.maxAirLoopTicks || player.isOnGround()) {
            scheduler.runEntityLater(player, new Runnable() {
                @Override
                public void run() {
                    fallProtection.unprotect(player.getUniqueId());
                }
            }, null, 2L);
            return;
        }
        Particles.spawn(player.getLocation().add(0.0D, 1.0D, 0.0D), "GREEN_DUST", 4);
        for (final LivingEntity entity : livingNearby(player, radius)) {
            if (affected.add(entity.getUniqueId())) {
                PotionEffects.apply(entity, "POISON", poison, duration);
                PotionEffects.apply(entity, "WEAKNESS", weakness, duration);
                drawLine(player.getEyeLocation(), entity.getLocation().add(0.0D, 1.0D, 0.0D), "BLACK_DUST");
            }
        }
        scheduler.runEntityLater(player, new Runnable() {
            @Override
            public void run() {
                venomTrail(player, radius, poison, weakness, duration, affected, tick + 1);
            }
        }, null, 1L);
    }

    private void burstDamage(final Player player, final double radius, final double pushSpeed, final double damage, final Set<UUID> damaged) {
        for (final LivingEntity entity : livingNearby(player, radius)) {
            if (damaged.add(entity.getUniqueId())) {
                Entities.rawDamage(entity, damage);
                Entities.pushAway(entity, player.getLocation(), pushSpeed);
            }
        }
    }

    private List<LivingEntity> livingNearby(final Player player, final double radius) {
        final List<LivingEntity> output = new ArrayList<LivingEntity>();
        for (final Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity && entity != player && !entity.isDead()) {
                output.add((LivingEntity) entity);
            }
        }
        return output;
    }

    private void ring(final Location origin, final String particle, final int steps, final int tick) {
        if (tick > steps) {
            return;
        }
        final double radius = tick * 0.5D;
        for (int angle = 0; angle < 360; angle += 10) {
            final double radians = Math.toRadians(angle);
            Particles.spawn(origin.clone().add(Math.cos(radians) * radius, tick * 0.05D, Math.sin(radians) * radius), particle, 1);
        }
        scheduler.runRegionLater(origin, new Runnable() {
            @Override
            public void run() {
                ring(origin, particle, steps, tick + 1);
            }
        }, 1L);
    }

    private void particleAura(final Entity entity, final String particle, final int ticks, final long period) {
        auraTick(entity, particle, ticks, period, 0);
    }

    private void auraTick(final Entity entity, final String particle, final int ticks, final long period, final int tick) {
        if (entity == null || entity.isDead() || tick >= ticks) {
            return;
        }
        Particles.spawn(entity.getLocation().add(0.0D, 1.0D, 0.0D), particle, 3);
        scheduler.runEntityLater(entity, new Runnable() {
            @Override
            public void run() {
                auraTick(entity, particle, ticks, period, tick + (int) period);
            }
        }, null, period);
    }

    private void rocketTrail(final Player player, final String particle, final int ticks, final int tick, final boolean glidingAtPeak, final int glideSustainTicks, final double peakVelocityY, final boolean glideStarted) {
        if (!player.isOnline() || (tick >= 5 && player.isOnGround()) || tick >= Math.max(1, ticks + Math.max(20, glideSustainTicks))) {
            fallProtection.unprotect(player.getUniqueId());
            if (this.glideService != null) {
                this.glideService.stop(player);
            } else {
                Entities.setGliding(player, false);
            }
            return;
        }
        Particles.spawn(player.getLocation(), particle, 1);
        final boolean reachedPeak = tick >= 5 && player.getVelocity().getY() <= peakVelocityY;
        final boolean shouldStartGlide = glidingAtPeak && !glideStarted && (reachedPeak || tick >= ticks);
        final boolean nowGliding = glideStarted || shouldStartGlide;
        if (shouldStartGlide) {
            if (this.glideService != null) {
                this.glideService.start(player, Math.max(20, glideSustainTicks));
            } else {
                Entities.setGliding(player, true);
            }
        }
        if (tick == ticks) {
            Entities.push(player, 0.0D, -0.3D, 0.0D);
            Entities.pushForward(player, 0.5D);
        }
        scheduler.runEntityLater(player, new Runnable() {
            @Override
            public void run() {
                rocketTrail(player, particle, ticks, tick + 1, glidingAtPeak, glideSustainTicks, peakVelocityY, nowGliding);
            }
        }, null, 1L);
    }

    private void waterTrail(final Player player, final int ticks, final int tick) {
        if (!player.isOnline() || tick >= ticks) {
            return;
        }
        Particles.spawn(player.getLocation(), "FALLING_WATER", 20);
        scheduler.runEntityLater(player, new Runnable() {
            @Override
            public void run() {
                waterTrail(player, ticks, tick + 1);
            }
        }, null, 1L);
    }

    private void waitForGround(final Player player, final Runnable onGround, final int tick, final int maxTicks) {
        if (!player.isOnline() || tick >= maxTicks || player.isOnGround()) {
            onGround.run();
            return;
        }
        scheduler.runEntityLater(player, new Runnable() {
            @Override
            public void run() {
                waitForGround(player, onGround, tick + 1, maxTicks);
            }
        }, null, 1L);
    }

    private void drawLine(final Location start, final Location end, final String particle) {
        final Vector delta = end.toVector().subtract(start.toVector());
        for (int i = 0; i <= 20; i++) {
            final double progress = i / 20.0D;
            final Location current = start.clone().add(delta.clone().multiply(progress));
            current.add(0.0D, Math.sin(progress * Math.PI) * 2.0D, 0.0D);
            Particles.spawn(current, particle, 2);
        }
    }

    private Location targetLocation(final Player player, final int distance) {
        final int safeDistance = Math.max(1, distance);
        try {
            java.lang.reflect.Method method;
            try {
                method = player.getClass().getMethod("getTargetBlock", java.util.Set.class, int.class);
                final Object target = method.invoke(player, null, Integer.valueOf(safeDistance));
                if (target instanceof Block) {
                    return ((Block) target).getLocation().add(0.5D, 0.5D, 0.5D);
                }
            } catch (final NoSuchMethodException ignored) {
                method = player.getClass().getMethod("getTargetBlock", java.util.HashSet.class, int.class);
                final Object target = method.invoke(player, null, Integer.valueOf(safeDistance));
                if (target instanceof Block) {
                    return ((Block) target).getLocation().add(0.5D, 0.5D, 0.5D);
                }
            }
        } catch (final Exception ignored) {
            // fallback below
        }
        return player.getEyeLocation().add(player.getLocation().getDirection().normalize().multiply(safeDistance));
    }

    private void placeCobwebSphere(final Location center, final int radius, final long durationTicks) {
        final World world = center.getWorld();
        if (world == null) {
            return;
        }
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radius * radius) {
                        continue;
                    }
                    final Location location = center.clone().add(x, y, z);
                    placeTemporaryCobweb(location, durationTicks);
                }
            }
        }
    }

    private void placeTemporaryCobweb(final Location location, final long durationTicks) {
        final Material cobweb = Materials.find("COBWEB").orElse(Material.getMaterial("WEB"));
        if (cobweb != null) {
            this.temporaryBlocks.placeTemporary(location, cobweb, durationTicks, "fake");
        }
    }

    private void clearCobwebs(final Location center, final int radius) {
        this.temporaryBlocks.clearCobwebs(center, radius);
    }

    private List<Integer> parseScheduleTicks(final String raw) {
        if (raw == null) {
            return Collections.singletonList(Integer.valueOf(0));
        }
        final String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (!normalized.contains("..") && !normalized.contains("-")) {
            return Collections.singletonList(Integer.valueOf(parseTick(normalized)));
        }
        String range = normalized;
        int step = 1;
        final int stepIndex = normalized.indexOf("step");
        if (stepIndex >= 0) {
            range = normalized.substring(0, stepIndex).trim();
            step = Math.max(1, parseInt(normalized.substring(stepIndex + 4).trim(), 1));
        }
        final String[] parts = range.contains("..") ? range.split("\\.\\.") : range.split("-");
        if (parts.length < 2) {
            return Collections.singletonList(Integer.valueOf(parseTick(raw)));
        }
        final int start = Math.max(0, parseTick(parts[0]));
        final int end = Math.max(start, parseTick(parts[1]));
        final List<Integer> ticks = new ArrayList<Integer>();
        for (int tick = start; tick <= end; tick += step) {
            ticks.add(Integer.valueOf(tick));
            if (ticks.size() > 512) {
                break;
            }
        }
        return ticks;
    }

    private boolean condition(final Player player, final String condition) {
        if (player == null || condition == null) {
            return false;
        }
        final String normalized = condition.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if ("CASTER_ON_GROUND".equals(normalized) || "ON_GROUND".equals(normalized)) {
            return player.isOnGround();
        }
        if ("CASTER_IN_AIR".equals(normalized) || "IN_AIR".equals(normalized)) {
            return !player.isOnGround();
        }
        if ("SNEAKING".equals(normalized)) {
            return player.isSneaking();
        }
        if ("NOT_SNEAKING".equals(normalized)) {
            return !player.isSneaking();
        }
        return false;
    }

    private List<LivingEntity> resolveTargets(final Player player, final java.util.Map<Object, Object> data, final String fallbackType) {
        return resolveTargets(player, data, fallbackType, AbilityContext.empty());
    }

    private List<LivingEntity> resolveTargets(final Player player, final java.util.Map<Object, Object> data, final String fallbackType, final AbilityContext context) {
        final java.util.Map<Object, Object> selector = asMap(data.get("selector"));
        final java.util.Map<Object, Object> source = selector.isEmpty() ? data : selector;
        final String type = string(source, "type", string(data, "target", fallbackType)).toLowerCase(Locale.ROOT).replace('-', '_');
        final double radius = number(source, "radius", 5.0D);
        final boolean includeCaster = Boolean.valueOf(string(source, "include-caster", type.contains("self") ? "true" : "false")).booleanValue();
        final boolean lineOfSight = Boolean.valueOf(string(source, "line-of-sight", "false")).booleanValue();
        final int maxTargets = Math.max(1, integer(source, "max-targets", 64));
        final List<LivingEntity> output = new ArrayList<LivingEntity>();
        if ("caster".equals(type) || "self".equals(type)) {
            output.add(player);
            return output;
        }
        if (("victim".equals(type) || "hit_entity".equals(type) || "target_entity".equals(type)) && context != null && context.hitEntity != null) {
            output.add(context.hitEntity);
            return output;
        }
        if ("self_and_nearby".equals(type) || "caster_and_nearby".equals(type)) {
            output.add(player);
        }
        for (final Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity) || entity.isDead()) {
                continue;
            }
            if (!includeCaster && entity == player) {
                continue;
            }
            if (("nearby_players".equals(type) || "players".equals(type)) && !(entity instanceof Player)) {
                continue;
            }
            if (("nearby_living".equals(type) || "living".equals(type) || "self_and_nearby".equals(type) || "caster_and_nearby".equals(type) || "nearby".equals(type)) == false) {
                continue;
            }
            if (lineOfSight && !player.hasLineOfSight(entity)) {
                continue;
            }
            output.add((LivingEntity) entity);
        }
        if ("nearest".equalsIgnoreCase(string(source, "sort", ""))) {
            Collections.sort(output, new java.util.Comparator<LivingEntity>() {
                @Override
                public int compare(final LivingEntity left, final LivingEntity right) {
                    return Double.compare(left.getLocation().distanceSquared(player.getLocation()), right.getLocation().distanceSquared(player.getLocation()));
                }
            });
        }
        if (output.size() > maxTargets) {
            return new ArrayList<LivingEntity>(output.subList(0, maxTargets));
        }
        return output;
    }

    private void particleShape(final Location origin, final String particle, final java.util.Map<Object, Object> shape, final int count) {
        if (origin == null) {
            return;
        }
        final String type = string(shape, "type", "RING").toUpperCase(Locale.ROOT).replace('-', '_');
        final int points = Math.max(2, Math.min(720, integer(shape, "points", 36)));
        final double radius = Math.max(0.1D, number(shape, "radius", number(shape, "radius-to", 1.0D)));
        if ("SPIRAL".equals(type) || "HELIX".equals(type)) {
            final double height = number(shape, "height", 1.8D);
            final int strands = Math.max(1, Math.min(8, integer(shape, "strands", 1)));
            for (int strand = 0; strand < strands; strand++) {
                final double base = (Math.PI * 2.0D / strands) * strand;
                for (int i = 0; i < points; i++) {
                    final double progress = i / (double) Math.max(1, points - 1);
                    final double angle = base + progress * Math.PI * 2.0D * number(shape, "rotations", 1.0D);
                    Particles.spawn(origin.clone().add(Math.cos(angle) * radius, progress * height, Math.sin(angle) * radius), particle, count);
                }
            }
            return;
        }
        if ("ARC".equals(type)) {
            final double height = number(shape, "height-curve", number(shape, "height", 2.0D));
            final double length = number(shape, "length", radius * 2.0D);
            for (int i = 0; i < points; i++) {
                final double progress = i / (double) Math.max(1, points - 1);
                Particles.spawn(origin.clone().add(length * progress, Math.sin(progress * Math.PI) * height, 0.0D), particle, count);
            }
            return;
        }
        if ("RANDOM_IN_SPHERE".equals(type) || "RANDOM_ON_SHELL".equals(type)) {
            final java.util.Random random = new java.util.Random();
            for (int i = 0; i < points; i++) {
                final double theta = random.nextDouble() * Math.PI * 2.0D;
                final double phi = Math.acos(2.0D * random.nextDouble() - 1.0D);
                final double distance = "RANDOM_ON_SHELL".equals(type) ? radius : radius * Math.cbrt(random.nextDouble());
                Particles.spawn(origin.clone().add(Math.sin(phi) * Math.cos(theta) * distance, Math.cos(phi) * distance, Math.sin(phi) * Math.sin(theta) * distance), particle, count);
            }
            return;
        }
        if ("SPHERE".equals(type)) {
            for (int i = 0; i < points; i++) {
                final double y = 1.0D - (i / (double) Math.max(1, points - 1)) * 2.0D;
                final double ringRadius = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
                final double angle = Math.PI * (3.0D - Math.sqrt(5.0D)) * i;
                Particles.spawn(origin.clone().add(Math.cos(angle) * ringRadius * radius, y * radius, Math.sin(angle) * ringRadius * radius), particle, count);
            }
            return;
        }
        for (int i = 0; i < points; i++) {
            final double angle = (Math.PI * 2.0D * i) / points;
            Particles.spawn(origin.clone().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius), particle, count);
        }
    }

    private void deliverText(final Player player, final String type, final String raw) {
        final String rendered = com.arkflame.smpweapons.util.TextBridge.renderLegacy(raw == null ? "" : raw);
        if ("actionbar".equals(type)) {
            com.arkflame.smpweapons.util.Titles.sendActionBar(player, rendered);
        } else {
            player.sendMessage(rendered);
        }
    }

    private String replacePlaceholders(final String raw, final Player player, final WeaponDefinition weapon) {
        return (raw == null ? "" : raw)
                .replace("{player}", player == null ? "" : player.getName())
                .replace("<player>", player == null ? "" : player.getName())
                .replace("{weapon}", weapon == null ? "" : weapon.getId())
                .replace("<weapon>", weapon == null ? "" : weapon.getId());
    }

    private List<String> stringList(final Object raw, final String fallback) {
        final List<String> values = new ArrayList<String>();
        if (raw instanceof java.util.List) {
            for (final Object value : (java.util.List<?>) raw) {
                values.add(String.valueOf(value));
            }
        } else if (fallback != null && !fallback.trim().isEmpty()) {
            values.add(fallback);
        }
        return values;
    }

    private static int parseTick(final String raw) {
        if (raw == null) {
            return 0;
        }
        final String first = raw.split("\\.")[0].split(" ")[0].trim();
        return parseInt(first, 0);
    }

    private static int parseInt(final String raw, final int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (final Exception ignored) {
            return fallback;
        }
    }

    private static double parseDouble(final String raw, final double fallback) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (final Exception ignored) {
            return fallback;
        }
    }

    private static double getDouble(final ConfigurationSection section, final String path, final double fallback) {
        return section == null ? fallback : section.getDouble(path, fallback);
    }

    private static int getInt(final ConfigurationSection section, final String path, final int fallback) {
        return section == null ? fallback : section.getInt(path, fallback);
    }

    private static String getString(final ConfigurationSection section, final String path, final String fallback) {
        return section == null ? fallback : section.getString(path, fallback);
    }

    private static final class AbilityContext {
        private final Location impactLocation;
        private final LivingEntity hitEntity;
        private final Entity projectile;

        private AbilityContext(final Location impactLocation, final LivingEntity hitEntity, final Entity projectile) {
            this.impactLocation = impactLocation == null ? null : impactLocation.clone();
            this.hitEntity = hitEntity;
            this.projectile = projectile;
        }

        private static AbilityContext empty() {
            return new AbilityContext(null, null, null);
        }
    }

}
