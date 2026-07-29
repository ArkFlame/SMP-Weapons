package com.arkflame.smpweapons.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientWeaponsResourceTest {

    @Test
    void clientWeaponsResourceExists() {
        assertNotNull(getClass().getResourceAsStream("/weapons/client-weapons.yml"));
    }

    @Test
    void clientWeaponsResourceHasWeaponsSection() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/weapons/client-weapons.yml")) {
            assertNotNull(stream);
            final YamlConfiguration configuration = load(stream);
            assertTrue(configuration.isConfigurationSection("weapons"));
        }
    }

    @Test
    void clientWeaponsResourceContainsRequestedWeaponIds() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/weapons/client-weapons.yml")) {
            assertNotNull(stream);
            final YamlConfiguration configuration = load(stream);
            final Set<String> expected = new LinkedHashSet<String>(Arrays.asList(
                    "force_bow", "boom_crossbow", "repell_shield", "ultratotem", "flux_sword", "zero_point", "heavy_core", "flame_sword"));
            final Set<String> actual = new LinkedHashSet<String>(configuration.getConfigurationSection("weapons").getKeys(false));
            assertEquals(expected, actual);
        }
    }

    @Test
    void flameSwordResourceHasRequestedConfiguration() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/weapons/client-weapons.yml")) {
            assertNotNull(stream);
            final YamlConfiguration configuration = load(stream);
            final ConfigurationSection weapon = configuration.getConfigurationSection("weapons.flame_sword");
            assertNotNull(weapon, "flame_sword weapon section missing");
            assertTrue(weapon.getBoolean("enabled"));
            assertFalse(weapon.getBoolean("override"));
            assertEquals("Flame Sword", weapon.getString("display-id"));
            assertEquals(Arrays.asList("NETHERITE_SWORD", "DIAMOND_SWORD"), weapon.getStringList("item.material"));
            assertEquals("<gradient:#f97316:#dc2626><bold>Flame Sword</bold></gradient>", weapon.getString("item.name"));
            assertEquals(Arrays.asList(
                    "<gray>Passive: Fire Resistance while in inventory.</gray>",
                    "<gray>Sneak right click to launch a fireball.</gray>",
                    "<gray>Impact creates a wave of lava.</gray>"), weapon.getStringList("item.lore"));
            assertEquals(Arrays.asList(
                    "DAMAGE_ALL:5", "KNOCKBACK:2", "LOOT_BONUS_MOBS:3", "SWEEPING_EDGE:3",
                    "DURABILITY:3", "MENDING:1", "FIRE_ASPECT:3"), weapon.getStringList("item.enchants"));
            assertEquals(Arrays.asList("HIDE_ATTRIBUTES", "HIDE_UNBREAKABLE"), weapon.getStringList("item.item-flags"));
            assertTrue(weapon.getBoolean("item.unbreakable"));
            assertEquals(Arrays.asList("Flame Sword"), weapon.getStringList("legacy.names-contains"));
            assertEquals(Arrays.asList("getflamesword"), weapon.getStringList("commands.get"));
            assertEquals(30, weapon.getInt("cooldowns.primary.seconds"));
            assertEquals(Arrays.asList("RIGHT_CLICK"), weapon.getStringList("triggers.cast.events"));
            assertEquals(Arrays.asList("SNEAKING", "MAIN_HAND"), weapon.getStringList("triggers.cast.conditions"));
            assertEquals("primary", weapon.getString("triggers.cast.cooldown"));
            assertEquals("flame_cast", weapon.getString("triggers.cast.timeline"));
            assertEquals(Arrays.asList("INVENTORY_TICK"), weapon.getStringList("passives.inventory.events"));
            assertEquals(Arrays.asList("FIRE_RESISTANCE:1:5"), weapon.getStringList("passives.inventory.effects"));

            final ConfigurationSection projectile = weapon.getConfigurationSection("projectiles.flame_fireball");
            assertNotNull(projectile, "flame_fireball projectile section missing");
            assertEquals("FIREBALL", projectile.getString("type"));
            assertEquals("EYE", projectile.getString("origin"));
            assertEquals(1.35D, projectile.getDouble("speed"), 0.001D);
            assertEquals(0.0D, projectile.getDouble("upward"), 0.001D);
            assertEquals(100, projectile.getInt("lifetime-ticks"));
            assertEquals("FLAME", projectile.getString("trail-particle"));
            assertTrue(projectile.getBoolean("remove-on-hit"));
            assertTrue(projectile.getBoolean("cancel-hit-damage"));
            assertEquals(0.0D, projectile.getDouble("yield"), 0.001D);
            assertFalse(projectile.getBoolean("incendiary"));
            assertEquals("flame_impact", projectile.getString("on-hit.timeline"));

            final List<?> castActions = weapon.getList("timelines.flame_cast.0");
            assertNotNull(castActions);
            assertEquals(3, castActions.size());
            final java.util.Map<?, ?> potionAction = (java.util.Map<?, ?>) castActions.get(0);
            final java.util.Map<?, ?> potion = (java.util.Map<?, ?>) potionAction.get("potion");
            assertEquals("caster", potion.get("target"));
            assertEquals(Arrays.asList("STRENGTH:3:6"), potion.get("effects"));
            final java.util.Map<?, ?> soundAction = (java.util.Map<?, ?>) castActions.get(1);
            final java.util.Map<?, ?> sound = (java.util.Map<?, ?>) soundAction.get("sound");
            assertEquals("caster", sound.get("target"));
            assertEquals("ENTITY_BLAZE_SHOOT", sound.get("name"));
            final java.util.Map<?, ?> spawnAction = (java.util.Map<?, ?>) castActions.get(2);
            final java.util.Map<?, ?> spawnProjectile = (java.util.Map<?, ?>) spawnAction.get("spawn_projectile");
            assertEquals("flame_fireball", spawnProjectile.get("id"));

            final List<?> impactActions = weapon.getList("timelines.flame_impact.0");
            assertNotNull(impactActions);
            assertEquals(4, impactActions.size());
            final java.util.Map<?, ?> impactSoundAction = (java.util.Map<?, ?>) impactActions.get(0);
            final java.util.Map<?, ?> impactSound = (java.util.Map<?, ?>) impactSoundAction.get("sound");
            assertEquals("impact_location", impactSound.get("target"));
            assertEquals("ENTITY_GENERIC_BURN", impactSound.get("name"));
            final java.util.Map<?, ?> particleAction = (java.util.Map<?, ?>) impactActions.get(1);
            final java.util.Map<?, ?> particle = (java.util.Map<?, ?>) particleAction.get("particle_shape");
            assertEquals("impact_location", particle.get("origin"));
            assertEquals("FLAME", particle.get("effect"));
            final java.util.Map<?, ?> particleShape = (java.util.Map<?, ?>) particle.get("shape");
            assertEquals("SPHERE", particleShape.get("type"));
            assertEquals(2.0D, ((Number) particleShape.get("radius")).doubleValue(), 0.001D);
            assertEquals(48, ((Number) particleShape.get("points")).intValue());
            final java.util.Map<?, ?> waveAction = (java.util.Map<?, ?>) impactActions.get(3);
            final java.util.Map<?, ?> wave = (java.util.Map<?, ?>) waveAction.get("shape_block_wave");
            assertEquals("impact_location", wave.get("origin"));
            assertEquals("fake", wave.get("mode"));
            assertEquals("LAVA", wave.get("block"));
            assertEquals(35, ((Number) wave.get("ttl-ticks")).intValue());
            final java.util.Map<?, ?> waveShape = (java.util.Map<?, ?>) wave.get("shape");
            assertEquals("SPHERE", waveShape.get("type"));
            assertEquals(5.0D, ((Number) waveShape.get("radius-to")).doubleValue(), 0.001D);
            assertEquals("HOLLOW", waveShape.get("fill"));
            final java.util.Map<?, ?> expand = (java.util.Map<?, ?>) wave.get("expand");
            assertEquals(2, ((Number) expand.get("every-ticks")).intValue());
            final java.util.Map<?, ?> collapse = (java.util.Map<?, ?>) wave.get("collapse");
            assertEquals(6, ((Number) collapse.get("delay-after-expand-ticks")).intValue());
            final java.util.Map<?, ?> collision = (java.util.Map<?, ?>) wave.get("collision");
            assertEquals(0.0D, ((Number) collision.get("real-radius")).doubleValue(), 0.001D);
        }
    }

    @Test
    void flameSwordImpactExplosionMatchesDamageAndVelocityContract() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/weapons/client-weapons.yml")) {
            assertNotNull(stream);
            final YamlConfiguration configuration = load(stream);
            final ConfigurationSection weapon = configuration.getConfigurationSection("weapons.flame_sword");
            assertNotNull(weapon, "flame_sword weapon section missing");
            final List<?> impactActions = weapon.getList("timelines.flame_impact.0");
            assertNotNull(impactActions, "flame_impact actions missing");
            assertTrue(impactActions.size() > 2, "flame_impact explosion action missing");
            final Object actionObject = impactActions.get(2);
            assertTrue(actionObject instanceof Map, "flame_impact action must be a map");
            final Map<?, ?> action = (Map<?, ?>) actionObject;
            assertTrue(action.containsKey("explosion"), "flame_impact action must declare explosion key");
            final Object explosionObject = action.get("explosion");
            assertNotNull(explosionObject, "flame_impact explosion missing");
            assertTrue(explosionObject instanceof Map, "flame_impact explosion must be a map");
            final Map<?, ?> explosion = (Map<?, ?>) explosionObject;

            final Map<String, Object> expected = new LinkedHashMap<String, Object>();
            expected.put("origin", "impact_location");
            expected.put("power", 0.0D);
            expected.put("radius", 5.0D);
            expected.put("damage", 6.0D);
            expected.put("raw", false);
            expected.put("falloff", "NONE");
            expected.put("players-only", true);
            expected.put("include-caster", false);
            expected.put("set-fire", false);
            expected.put("break-blocks", false);
            expected.put("sound", "");
            expected.put("particle", "");
            expected.put("knockback", true);
            expected.put("knockback-strength", 0.35D);
            expected.put("knockback-lift", 0.18D);
            expected.put("knockback-players-only", true);
            expected.put("knockback-include-caster", false);
            assertEquals(expected, explosion);
        }
    }

    @Test
    void flameSwordImpactExplosionRadiusMatchesLavaWaveRadius() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/weapons/client-weapons.yml")) {
            assertNotNull(stream);
            final YamlConfiguration configuration = load(stream);
            final ConfigurationSection weapon = configuration.getConfigurationSection("weapons.flame_sword");
            assertNotNull(weapon, "flame_sword weapon section missing");
            final List<?> impactActions = weapon.getList("timelines.flame_impact.0");
            assertNotNull(impactActions, "flame_impact actions missing");
            assertTrue(impactActions.size() > 3, "flame_impact lava wave action missing");

            final Object explosionActionObject = impactActions.get(2);
            assertTrue(explosionActionObject instanceof Map, "flame_impact explosion action must be a map");
            final Map<?, ?> explosionAction = (Map<?, ?>) explosionActionObject;
            assertTrue(explosionAction.containsKey("explosion"), "flame_impact action must declare explosion key");
            final Object explosionObject = explosionAction.get("explosion");
            assertNotNull(explosionObject, "flame_impact explosion missing");
            assertTrue(explosionObject instanceof Map, "flame_impact explosion must be a map");
            final Map<?, ?> explosion = (Map<?, ?>) explosionObject;
            assertTrue(explosion.get("radius") instanceof Number, "flame_impact explosion radius missing");

            final Object waveActionObject = impactActions.get(3);
            assertTrue(waveActionObject instanceof Map, "flame_impact wave action must be a map");
            final Map<?, ?> waveAction = (Map<?, ?>) waveActionObject;
            assertTrue(waveAction.containsKey("shape_block_wave"), "flame_impact action must declare shape_block_wave key");
            final Object waveObject = waveAction.get("shape_block_wave");
            assertNotNull(waveObject, "flame_impact lava wave missing");
            assertTrue(waveObject instanceof Map, "flame_impact lava wave must be a map");
            final Map<?, ?> wave = (Map<?, ?>) waveObject;
            final Object shapeObject = wave.get("shape");
            assertNotNull(shapeObject, "flame_impact lava wave shape missing");
            assertTrue(shapeObject instanceof Map, "flame_impact lava wave shape must be a map");
            final Map<?, ?> shape = (Map<?, ?>) shapeObject;
            assertTrue(shape.get("radius-to") instanceof Number, "flame_impact lava wave radius missing");

            assertEquals(((Number) explosion.get("radius")).doubleValue(),
                    ((Number) shape.get("radius-to")).doubleValue(), 0.001D);
        }
    }

    @Test
    void heavyCoreSpeedAndZeroPointTimelineRegression() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/weapons/client-weapons.yml")) {
            assertNotNull(stream);
            final YamlConfiguration configuration = load(stream);
            final ConfigurationSection heavyCore = configuration.getConfigurationSection("weapons.heavy_core");
            assertNotNull(heavyCore, "heavy_core weapon section missing");
            assertTrue(heavyCore.getBoolean("enabled"));
            assertFalse(heavyCore.getBoolean("override"));
            assertEquals("Heavy Core", heavyCore.getString("display-id"));
            assertEquals(Arrays.asList("HEAVY_CORE", "NETHER_STAR"), heavyCore.getStringList("item.material"));
            assertEquals(Arrays.asList("UNBREAKING:1"), heavyCore.getStringList("item.enchants"));
            assertEquals(Arrays.asList("HIDE_ATTRIBUTES"), heavyCore.getStringList("item.item-flags"));
            assertEquals(Arrays.asList("Heavy Core"), heavyCore.getStringList("legacy.names-contains"));
            assertEquals(Arrays.asList("getheavycore"), heavyCore.getStringList("commands.get"));
            assertEquals(45, heavyCore.getInt("cooldowns.primary.seconds"));
            assertEquals(Arrays.asList("RIGHT_CLICK"), heavyCore.getStringList("triggers.pull.events"));
            assertEquals(Arrays.asList("SNEAKING", "MAIN_HAND"), heavyCore.getStringList("triggers.pull.conditions"));
            assertEquals("primary", heavyCore.getString("triggers.pull.cooldown"));
            assertEquals("pull_players", heavyCore.getString("triggers.pull.timeline"));
            final List<?> pullActions = heavyCore.getList("timelines.pull_players.0");
            assertNotNull(pullActions);
            assertEquals(3, pullActions.size());
            final java.util.Map<?, ?> pullAction = (java.util.Map<?, ?>) pullActions.get(2);
            final java.util.Map<?, ?> pull = (java.util.Map<?, ?>) pullAction.get("pull");
            assertEquals(7.0D, ((Number) pull.get("radius")).doubleValue(), 0.001D);
            assertEquals(80, ((Number) pull.get("duration-ticks")).intValue());
            assertEquals(1, ((Number) pull.get("period-ticks")).intValue());
            assertEquals(0.105D, ((Number) pull.get("speed")).doubleValue(), 0.000001D);
            assertEquals(0.08D, ((Number) pull.get("vertical")).doubleValue(), 0.001D);
            assertEquals("REVERSE_PORTAL", pull.get("particle"));
            assertEquals(6, ((Number) pull.get("particle-count")).intValue());
            assertEquals(Boolean.TRUE, pull.get("players-only"));

            final ConfigurationSection zeroPoint = configuration.getConfigurationSection("weapons.zero_point");
            assertNotNull(zeroPoint, "zero_point weapon section missing");
            assertTrue(zeroPoint.getBoolean("enabled"));
            assertFalse(zeroPoint.getBoolean("override"));
            assertEquals("Zero Point", zeroPoint.getString("display-id"));
            assertEquals(Arrays.asList("HEART_OF_THE_SEA", "PRISMARINE_CRYSTALS", "NETHER_STAR"), zeroPoint.getStringList("item.material"));
            assertEquals(Arrays.asList("UNBREAKING:1"), zeroPoint.getStringList("item.enchants"));
            assertEquals(Arrays.asList("HIDE_ATTRIBUTES"), zeroPoint.getStringList("item.item-flags"));
            assertEquals(Arrays.asList("Zero Point"), zeroPoint.getStringList("legacy.names-contains"));
            assertEquals(Arrays.asList("getzeropoint"), zeroPoint.getStringList("commands.get"));
            assertEquals(Arrays.asList("INVENTORY_TICK"), zeroPoint.getStringList("passives.conduit_inventory.events"));
            assertEquals(Arrays.asList("CONDUIT_POWER:1:5"), zeroPoint.getStringList("passives.conduit_inventory.effects"));
            assertFalse(zeroPoint.isConfigurationSection("timelines"));
            assertFalse(zeroPoint.isConfigurationSection("pull"));
        }
    }

    @Test
    void boomCrossbowTimelineAndExplosionAreWired() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/weapons/client-weapons.yml")) {
            assertNotNull(stream);
            final YamlConfiguration configuration = load(stream);
            final ConfigurationSection weapon = configuration.getConfigurationSection("weapons.boom_crossbow");
            assertNotNull(weapon, "boom_crossbow weapon section missing");
            final ConfigurationSection projectile = weapon.getConfigurationSection("projectiles.boom_arrow");
            assertNotNull(projectile, "boom_arrow projectile section missing");
            final ConfigurationSection onHit = projectile.getConfigurationSection("on-hit");
            assertNotNull(onHit, "projectile on-hit section missing");
            assertEquals("boom_impact", onHit.getString("timeline"));
            final ConfigurationSection timelines = weapon.getConfigurationSection("timelines");
            assertNotNull(timelines, "timelines section missing");
            final ConfigurationSection boomImpact = timelines.getConfigurationSection("boom_impact");
            assertNotNull(boomImpact, "boom_impact timeline missing");
            final List<?> actions = boomImpact.getList("0");
            assertNotNull(actions);
            assertFalse(actions.isEmpty());
            final Object first = actions.get(0);
            assertTrue(first instanceof java.util.Map, "boom_impact action must be a map");
            final java.util.Map<?, ?> map = (java.util.Map<?, ?>) first;
            assertTrue(map.containsKey("explosion"), "boom_impact action must declare explosion key");
            final java.util.Map<?, ?> explosion = (java.util.Map<?, ?>) map.get("explosion");
            assertEquals(6.0D, ((Number) explosion.get("damage")).doubleValue(), 0.001D);
            assertEquals(4.0D, ((Number) explosion.get("radius")).doubleValue(), 0.001D);
            assertEquals(Boolean.TRUE, explosion.get("raw"));
            assertEquals(Boolean.TRUE, explosion.get("players-only"));
        }
    }

    @Test
    void boomCrossbowExplosionUsesFlatFalloff() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/weapons/client-weapons.yml")) {
            assertNotNull(stream);
            final YamlConfiguration configuration = load(stream);
            final List<?> actions = configuration.getList("weapons.boom_crossbow.timelines.boom_impact.0");
            assertNotNull(actions);
            assertFalse(actions.isEmpty());
            final java.util.Map<?, ?> action = (java.util.Map<?, ?>) actions.get(0);
            final java.util.Map<?, ?> explosion = (java.util.Map<?, ?>) action.get("explosion");
            assertEquals("NONE", explosion.get("falloff"));
        }
    }

    private static YamlConfiguration load(final InputStream stream) {
        final YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (final Exception ignored) {
            return configuration;
        }
        return configuration;
    }
}
