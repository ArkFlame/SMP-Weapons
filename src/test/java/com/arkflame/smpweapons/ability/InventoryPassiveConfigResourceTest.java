package com.arkflame.smpweapons.ability;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class InventoryPassiveConfigResourceTest {

    @Test
    void defaultInventoryPassivePeriodIsOneSecond() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/config.yml")) {
            assertNotNull(stream);
            final YamlConfiguration configuration = load(stream);
            assertEquals(20L, configuration.getLong("engine.inventory-passive-period-ticks"));
        }
    }

    private static YamlConfiguration load(final InputStream stream) {
        final YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (final Exception exception) {
            throw new IllegalStateException("Could not load config.yml", exception);
        }
        return configuration;
    }
}
