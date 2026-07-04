package com.arkflame.smpweapons.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class CommandAbilityExampleResourceTest {

    @Test
    void commandAbilityExampleExists() throws Exception {
        final YamlConfiguration configuration = loadExamples();
        assertNotNull(configuration.getConfigurationSection("weapons.example_sell_wand"));
    }

    @Test
    void commandAbilityExampleUsesRightClickTrigger() throws Exception {
        final YamlConfiguration configuration = loadExamples();
        assertEquals("RIGHT_CLICK", configuration.getString("weapons.example_sell_wand.trigger.type"));
    }

    @Test
    void commandAbilityExampleHasNoCooldown() throws Exception {
        final YamlConfiguration configuration = loadExamples();
        assertEquals(0, configuration.getInt("weapons.example_sell_wand.trigger.cooldown"));
    }

    @Test
    void commandAbilityExampleUsesCommandAbilityType() throws Exception {
        final YamlConfiguration configuration = loadExamples();
        assertEquals("COMMAND", configuration.getString("weapons.example_sell_wand.ability.type"));
    }

    @Test
    void commandAbilityExampleRunsSellAllAsPlayer() throws Exception {
        final YamlConfiguration configuration = loadExamples();
        assertEquals(Collections.singletonList("/sell all"), configuration.getStringList("weapons.example_sell_wand.ability.commands"));
    }

    @Test
    void commandAbilityExampleSenderIsPlayer() throws Exception {
        final YamlConfiguration configuration = loadExamples();
        assertEquals("player", configuration.getString("weapons.example_sell_wand.ability.sender"));
    }

    private static YamlConfiguration loadExamples() throws Exception {
        try (InputStream stream = CommandAbilityExampleResourceTest.class.getResourceAsStream("/weapons/examples.yml")) {
            assertNotNull(stream);
            final YamlConfiguration configuration = new YamlConfiguration();
            configuration.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return configuration;
        }
    }
}
