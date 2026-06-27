package com.arkflame.smpweapons.hook;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SMPRegionsHookResourceTest {

    @Test
    void defaultConfigEnablesSMPRegionsHook() {
        assertTrue(loadResource("/config.yml").getBoolean("hooks.smp-regions.enabled"));
    }

    @Test
    void defaultConfigDeniesWhenPvpDenied() {
        assertTrue(loadResource("/config.yml").getBoolean("hooks.smp-regions.deny-when-pvp-denied"));
    }

    @Test
    void defaultConfigHasEmptyRegionBlacklist() {
        assertEquals(0, loadResource("/config.yml").getStringList("hooks.smp-regions.blacklist-regions").size());
    }

    @Test
    void messagesContainRegionDenied() {
        assertTrue(loadResource("/messages.yml").isString("region-denied"));
    }

    private static YamlConfiguration loadResource(final String path) {
        final InputStream stream = SMPRegionsHookResourceTest.class.getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Missing test resource: " + path);
        }
        final YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (final Exception exception) {
            throw new IllegalStateException("Could not load test resource: " + path, exception);
        }
        return configuration;
    }
}