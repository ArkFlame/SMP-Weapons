package com.arkflame.smpweapons.projectile;

import com.arkflame.smpweapons.ability.AbilityEngine;
import com.arkflame.smpweapons.block.TemporaryBlockService;
import com.arkflame.smpweapons.model.WeaponDefinition;
import com.arkflame.smpweapons.util.FoliaAPI;
import com.arkflame.smpweapons.util.Materials;
import com.arkflame.smpweapons.util.Particles;
import com.arkflame.smpweapons.util.PotionEffects;
import com.arkflame.smpweapons.util.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ProjectileService {
    private final FoliaAPI scheduler;
    private final TemporaryBlockService temporaryBlocks;
    private final int maxPerCaster;
    private final int maxGlobal;
    private AbilityEngine abilityEngine;
    private final Map<UUID, ProjectileContext> projectiles = new HashMap<UUID, ProjectileContext>();

    public ProjectileService(final FoliaAPI scheduler, final TemporaryBlockService temporaryBlocks) {
        this(scheduler, temporaryBlocks, 10, 200);
    }

    public ProjectileService(final FoliaAPI scheduler, final TemporaryBlockService temporaryBlocks, final int maxPerCaster, final int maxGlobal) {
        this.scheduler = scheduler;
        this.temporaryBlocks = temporaryBlocks;
        this.maxPerCaster = Math.max(1, maxPerCaster);
        this.maxGlobal = Math.max(1, maxGlobal);
    }

    public void setAbilityEngine(final AbilityEngine abilityEngine) {
        this.abilityEngine = abilityEngine;
    }

    public void launchCobwebBomb(final Player caster, final WeaponDefinition weapon, final ConfigurationSection section) {
        launchFromSection(caster, weapon, section, section == null ? null : section.getConfigurationSection("projectile"));
    }

    public void launchConfigured(final Player caster, final WeaponDefinition weapon, final ConfigurationSection section, final String projectileId) {
        ConfigurationSection projectile = null;
        ConfigurationSection projectiles = section == null ? null : section.getConfigurationSection("projectiles");
        if ((projectiles == null || (projectileId != null && !projectiles.getKeys(false).contains(projectileId))) && weapon != null && weapon.getProjectilesSection() != null) {
            projectiles = weapon.getProjectilesSection();
        }
        if (projectiles != null && projectileId != null) {
            projectile = projectiles.getConfigurationSection(projectileId);
        }
        if (projectile == null && section != null) {
            projectile = section.getConfigurationSection("projectile");
        }
        launchFromSection(caster, weapon, section, projectile);
    }

    private void launchFromSection(final Player caster, final WeaponDefinition weapon, final ConfigurationSection impactSection, final ConfigurationSection projectileSection) {
        if (caster == null || weapon == null || this.projectiles.size() >= this.maxGlobal || countOwned(caster.getUniqueId()) >= this.maxPerCaster) {
            return;
        }
        final double speed = getDouble(projectileSection, "speed", 1.35D);
        final double upward = getDouble(projectileSection, "upward", 0.10D);
        final int lifetime = Math.max(1, Math.min(600, getInt(projectileSection, "lifetime-ticks", 100)));
        final Location origin = resolveOrigin(caster, getString(projectileSection, "origin", "EYE"));
        final Vector direction = origin.getDirection().normalize();
        final Vector velocity = direction.clone().multiply(speed).setY(direction.getY() * speed + upward);
        final Projectile projectile = spawnProjectile(caster, origin, getString(projectileSection, "type", "SNOWBALL"));
        if (projectile == null) {
            return;
        }
        projectile.setVelocity(velocity);
        final ProjectileContext context = new ProjectileContext(projectile.getUniqueId(), projectile, caster.getUniqueId(), weapon, impactSection, projectileSection);
        this.projectiles.put(projectile.getUniqueId(), context);
        projectileTrail(projectile, context, 0, lifetime);
        this.scheduler.runEntityLater(projectile, new Runnable() {
            @Override
            public void run() {
                expire(projectile.getUniqueId());
            }
        }, null, Math.max(1L, lifetime));
    }

    private Location resolveOrigin(final Player caster, final String raw) {
        if (caster == null) {
            return null;
        }
        final String normalized = raw == null ? "EYE" : raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if ("FEET".equals(normalized) || "CASTER".equals(normalized)) {
            return caster.getLocation().clone();
        }
        return caster.getEyeLocation().clone();
    }

    private int countOwned(final UUID casterId) {
        int count = 0;
        for (final ProjectileContext context : this.projectiles.values()) {
            if (context != null && casterId.equals(context.getCasterId())) {
                count++;
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private Projectile spawnProjectile(final Player caster, final Location origin, final String type) {
        final Class<? extends Projectile> projectileClass = projectileClass(type);
        try {
            final Projectile projectile = caster.getWorld().spawn(origin, projectileClass);
            try {
                projectile.setShooter((ProjectileSource) caster);
            } catch (final Throwable ignored) {
                // old API variants still keep projectile ownership by registry
            }
            return projectile;
        } catch (final Exception ex) {
            try {
                final Snowball fallback = caster.getWorld().spawn(origin, Snowball.class);
                fallback.setShooter((ProjectileSource) caster);
                return fallback;
            } catch (final Exception ignored) {
                return null;
            }
        }
    }

    private Class<? extends Projectile> projectileClass(final String raw) {
        final String type = raw == null ? "SNOWBALL" : raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if ("ARROW".equals(type)) {
            return Arrow.class;
        }
        if ("FIREBALL".equals(type) || "SMALL_FIREBALL".equals(type)) {
            return Fireball.class;
        }
        if ("ENDER_PEARL".equals(type) || "ENDERPEARL".equals(type)) {
            return EnderPearl.class;
        }
        if ("EGG".equals(type)) {
            return Egg.class;
        }
        return Snowball.class;
    }

    public void handleDamage(final EntityDamageByEntityEvent event) {
        if (event == null || !(event.getDamager() instanceof Projectile)) {
            return;
        }
        final Projectile projectile = (Projectile) event.getDamager();
        final ProjectileContext context = this.projectiles.remove(projectile.getUniqueId());
        if (context == null) {
            return;
        }
        if (context.removeOnHit()) {
            event.setCancelled(true);
        }
        final Location impact = event.getEntity().getLocation();
        final LivingEntity hit = event.getEntity() instanceof LivingEntity ? (LivingEntity) event.getEntity() : null;
        runHitEffects(hit, context);
        runHitTimeline(impact, hit, projectile, context);
        if (!context.hasHitTimeline()) {
            runImpact(impact, context);
        }
        if (context.removeOnHit()) {
            projectile.remove();
        }
    }

    public void handleHit(final ProjectileHitEvent event) {
        if (event == null || !(event.getEntity() instanceof Projectile)) {
            return;
        }
        final Projectile projectile = (Projectile) event.getEntity();
        final ProjectileContext context = this.projectiles.remove(projectile.getUniqueId());
        if (context == null) {
            return;
        }
        final Location impact = projectile.getLocation();
        runHitTimeline(impact, null, projectile, context);
        if (!context.hasHitTimeline()) {
            runImpact(impact, context);
        }
        if (context.removeOnHit()) {
            projectile.remove();
        }
    }

    public void clearOwned(final UUID casterId) {
        if (casterId == null) {
            return;
        }
        final java.util.List<UUID> remove = new java.util.ArrayList<UUID>();
        for (final Map.Entry<UUID, ProjectileContext> entry : this.projectiles.entrySet()) {
            final ProjectileContext context = entry.getValue();
            if (context != null && casterId.equals(context.getCasterId())) {
                remove.add(entry.getKey());
            }
        }
        for (final UUID id : remove) {
            expire(id);
        }
    }

    public void clear() {
        for (final ProjectileContext context : new java.util.ArrayList<ProjectileContext>(this.projectiles.values())) {
            if (context.getEntity() != null && !context.getEntity().isDead()) {
                context.getEntity().remove();
            }
        }
        this.projectiles.clear();
    }

    private void expire(final UUID projectileId) {
        if (projectileId == null) {
            return;
        }
        final ProjectileContext context = this.projectiles.remove(projectileId);
        if (context != null) {
            final Entity entity = context.getEntity();
            runExpireTimeline(entity == null ? null : entity.getLocation(), entity, context);
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
    }

    private void projectileTrail(final Entity projectile, final ProjectileContext context, final int tick, final int lifetime) {
        if (projectile == null || projectile.isDead() || !this.projectiles.containsKey(projectile.getUniqueId()) || tick >= lifetime) {
            return;
        }
        final ConfigurationSection section = context.getProjectileSection();
        final String particle = getString(section, "trail-particle", getString(section, "particle", "WHITE_DUST"));
        Particles.spawn(projectile.getLocation(), particle, 2);
        final ConfigurationSection onTick = section == null ? null : section.getConfigurationSection("on_tick");
        if (onTick != null) {
            final int every = Math.max(1, onTick.getInt("every", 1));
            final String timeline = onTick.getString("timeline", null);
            if (timeline != null && tick % every == 0) {
                runTimeline(timeline, projectile.getLocation(), null, projectile, context);
            }
        }
        this.scheduler.runEntityLater(projectile, new Runnable() {
            @Override
            public void run() {
                projectileTrail(projectile, context, tick + 1, lifetime);
            }
        }, null, 1L);
    }

    private void runHitEffects(final LivingEntity hitEntity, final ProjectileContext context) {
        if (hitEntity == null) {
            return;
        }
        final ConfigurationSection section = context.getSection();
        final java.util.List<String> effects = section == null ? java.util.Collections.<String>emptyList() : section.getStringList("hit-effects");
        for (final String effect : effects) {
            PotionEffects.apply(hitEntity, effect);
        }
    }

    private void runHitTimeline(final Location impact, final LivingEntity hitEntity, final Entity projectile, final ProjectileContext context) {
        final ConfigurationSection onHit = context.getOnHitSection();
        final String timeline = onHit == null ? null : onHit.getString("timeline", null);
        if (timeline != null) {
            runTimeline(timeline, impact, hitEntity, projectile, context);
        }
    }

    private void runExpireTimeline(final Location location, final Entity projectile, final ProjectileContext context) {
        final ConfigurationSection section = context.getProjectileSection();
        final ConfigurationSection onExpire = section == null ? null : section.getConfigurationSection("on_expire");
        final String timeline = onExpire == null ? null : onExpire.getString("timeline", null);
        if (timeline != null) {
            runTimeline(timeline, location, null, projectile, context);
        }
    }

    private void runTimeline(final String timeline, final Location impact, final LivingEntity hitEntity, final Entity projectile, final ProjectileContext context) {
        if (this.abilityEngine == null || timeline == null || context == null || context.getWeapon() == null) {
            return;
        }
        final Player caster = Bukkit.getPlayer(context.getCasterId());
        if (caster == null || !caster.isOnline()) {
            return;
        }
        this.abilityEngine.executeNamedTimeline(caster, context.getWeapon(), timeline, impact, hitEntity, projectile);
    }

    private void runImpact(final Location impact, final ProjectileContext context) {
        if (impact == null) {
            return;
        }
        final ConfigurationSection section = context.getSection();
        final Material cobweb = Materials.find(getString(section, "block", "COBWEB")).orElse(Material.getMaterial("WEB"));
        if (cobweb == null) {
            return;
        }
        Sounds.play(impact, getString(section, "impact-sound", "ENTITY_SPIDER_DEATH"), 1.0F, 1.0F);
        Particles.cloud(impact.clone().add(0.0D, 0.5D, 0.0D), getInt(section, "impact-particles", 20), 0.6D);
        final int centerTtl = getInt(section, "center-ttl-ticks", 35);
        final int radius = getInt(section, "wave-radius", 5);
        final int ttl = getInt(section, "wave-ttl-ticks", 35);
        final int realRadius = getInt(section, "real-radius", 1);
        final long step = getInt(section, "wave-step-ticks", 2);
        final long collapseDelay = getInt(section, "collapse-delay-ticks", 6);
        final String mode = getString(section, "block-mode", "fake");
        this.temporaryBlocks.placeTemporary(impact, cobweb, centerTtl, mode);
        this.temporaryBlocks.waveSphere(impact, cobweb, radius, ttl, mode, realRadius, step, collapseDelay);
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

    private static final class ProjectileContext {
        private final UUID projectileId;
        private final Entity entity;
        private final UUID casterId;
        private final WeaponDefinition weapon;
        private final String weaponId;
        private final ConfigurationSection section;
        private final ConfigurationSection projectileSection;

        private ProjectileContext(final UUID projectileId, final Entity entity, final UUID casterId, final WeaponDefinition weapon, final ConfigurationSection section, final ConfigurationSection projectileSection) {
            this.projectileId = projectileId;
            this.entity = entity;
            this.casterId = casterId;
            this.weapon = weapon;
            this.weaponId = weapon == null ? "" : weapon.getId().toLowerCase(Locale.ROOT);
            this.section = section;
            this.projectileSection = projectileSection;
        }

        private Entity getEntity() {
            return entity;
        }

        private WeaponDefinition getWeapon() {
            return weapon;
        }

        private ConfigurationSection getSection() {
            return section;
        }

        private ConfigurationSection getProjectileSection() {
            return projectileSection;
        }

        private UUID getCasterId() {
            return casterId;
        }

        private boolean removeOnHit() {
            return projectileSection == null || projectileSection.getBoolean("remove-on-hit", true);
        }

        private ConfigurationSection getOnHitSection() {
            if (projectileSection == null) {
                return null;
            }
            return projectileSection.getConfigurationSection("on_hit");
        }

        private boolean hasHitTimeline() {
            final ConfigurationSection onHit = getOnHitSection();
            return onHit != null && onHit.isString("timeline");
        }

        @Override
        public String toString() {
            return "ProjectileContext{projectileId=" + projectileId + ", casterId=" + casterId + ", weaponId='" + weaponId + "'}";
        }
    }

}
