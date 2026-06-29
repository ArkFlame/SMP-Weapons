package com.arkflame.smpweapons.ability;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AbilityItemProtectionConfigResourceTest {

    @Test
    void defaultConfigEnablesAbilityItemProtection() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/config.yml")) {
            assertNotNull(stream);
            final YamlConfiguration configuration = load(stream);
            assertTrue(configuration.getBoolean("ability-item-protection.enabled"));
        }
    }

    @Test
    void defaultConfigUsesOneSecondFinishGrace() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/config.yml")) {
            assertNotNull(stream);
            final YamlConfiguration configuration = load(stream);
            assertEquals(20L, configuration.getLong("ability-item-protection.finish-grace-ticks"));
        }
    }

    @Test
    void defaultConfigHasStaleTimeoutLongerThanFinishGrace() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/config.yml")) {
            assertNotNull(stream);
            final YamlConfiguration configuration = load(stream);
            final long staleTimeout = configuration.getLong("ability-item-protection.stale-timeout-ticks");
            final long finishGrace = configuration.getLong("ability-item-protection.finish-grace-ticks");
            assertTrue(staleTimeout >= finishGrace, "stale-timeout-ticks must be >= finish-grace-ticks");
        }
    }

    @Test
    void defaultConfigSchedulesInventoryUpdateNextTick() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/config.yml")) {
            assertNotNull(stream);
            final YamlConfiguration configuration = load(stream);
            assertEquals(1L, configuration.getLong("ability-item-protection.update-inventory-delay-ticks"));
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