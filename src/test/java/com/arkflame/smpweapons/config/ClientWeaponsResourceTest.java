package com.arkflame.smpweapons.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
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
                    "force_bow", "boom_crossbow", "repell_shield", "ultratotem", "flux_sword", "zero_point", "heavy_core"));
            final Set<String> actual = new LinkedHashSet<String>(configuration.getConfigurationSection("weapons").getKeys(false));
            assertEquals(expected, actual);
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
