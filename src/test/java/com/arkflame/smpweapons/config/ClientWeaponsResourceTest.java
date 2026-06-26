package com.arkflame.smpweapons.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

final class ClientWeaponsResourceTest {

    @Test
    void clientWeaponsResourceExists() {
        final InputStream stream = getClass().getResourceAsStream("/weapons/client-weapons.yml");
        assertNotNull(stream, "Bundled resource /weapons/client-weapons.yml must exist");
    }

    @Test
    void clientWeaponsResourceHasWeaponsSection() {
        final InputStream stream = getClass().getResourceAsStream("/weapons/client-weapons.yml");
        assertNotNull(stream, "Bundled resource /weapons/client-weapons.yml must exist");
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        assertTrue(configuration.isConfigurationSection("weapons"),
                "Bundled /weapons/client-weapons.yml must expose a top-level weapons section");
    }

    @Test
    void clientWeaponsResourceContainsRequestedWeaponIds() {
        final InputStream stream = getClass().getResourceAsStream("/weapons/client-weapons.yml");
        assertNotNull(stream, "Bundled resource /weapons/client-weapons.yml must exist");
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        assertTrue(configuration.isConfigurationSection("weapons"),
                "Bundled /weapons/client-weapons.yml must expose a top-level weapons section");

        final Set<String> expected = new LinkedHashSet<String>(Arrays.asList(
                "force_bow",
                "boom_crossbow",
                "repell_shield",
                "ultratotem",
                "flux_sword",
                "zero_point",
                "heavy_core"));
        final Set<String> actual = new LinkedHashSet<String>(
                configuration.getConfigurationSection("weapons").getKeys(false));
        assertEquals(expected, actual,
                "Bundled /weapons/client-weapons.yml must declare the expected weapon ids in the requested order");
    }
}