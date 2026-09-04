package com.arkflame.smpweapons.ability;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CooldownServiceVisualCooldownTest {

    interface CooldownCapablePlayer extends Player {
        void setCooldown(ItemStack item, int ticks);
        void setCooldown(Material material, int ticks);
    }

    private static final UUID FIXED_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void nonShieldWithoutKeyAppliesVisualCooldown() {
        final AtomicInteger cooldownCalls = new AtomicInteger(0);
        final ItemStack held = new ItemStack(Material.DIAMOND_SWORD);
        final Player player = createPlayer(held, cooldownCalls);
        final com.arkflame.smpweapons.model.WeaponDefinition weapon = weapon(Collections.singletonList("DIAMOND_SWORD"), null);

        final CooldownService service = new CooldownService(null, null);
        service.start(player, weapon, "primary", 5, false, held);

        assertTrue(cooldownCalls.get() > 0);
    }

    @Test
    void shieldWithoutKeySkipsVisualCooldown() {
        final AtomicInteger cooldownCalls = new AtomicInteger(0);
        final ItemStack held = new ItemStack(Material.BANNER);
        final Player player = createPlayer(held, cooldownCalls);
        final com.arkflame.smpweapons.model.WeaponDefinition weapon = weapon(Collections.singletonList("SHIELD"), null);

        final CooldownService service = new CooldownService(null, null);
        service.start(player, weapon, "primary", 5, false, held);

        assertEquals(0, cooldownCalls.get());
    }

    @Test
    void explicitTrueOverridesShieldDefault() {
        final AtomicInteger cooldownCalls = new AtomicInteger(0);
        final ItemStack held = new ItemStack(Material.BANNER);
        final Player player = createPlayer(held, cooldownCalls);
        final com.arkflame.smpweapons.model.WeaponDefinition weapon = weapon(Collections.singletonList("SHIELD"), Boolean.TRUE);

        final CooldownService service = new CooldownService(null, null);
        service.start(player, weapon, "primary", 5, false, held);

        assertTrue(cooldownCalls.get() > 0);
    }

    @Test
    void explicitFalseOverridesNonShieldDefault() {
        final AtomicInteger cooldownCalls = new AtomicInteger(0);
        final ItemStack held = new ItemStack(Material.DIAMOND_SWORD);
        final Player player = createPlayer(held, cooldownCalls);
        final com.arkflame.smpweapons.model.WeaponDefinition weapon = weapon(Collections.singletonList("DIAMOND_SWORD"), Boolean.FALSE);

        final CooldownService service = new CooldownService(null, null);
        service.start(player, weapon, "primary", 5, false, held);

        assertEquals(0, cooldownCalls.get());
    }

    @Test
    void disabledVisualCooldownStillStoresLogicalCooldown() {
        final AtomicInteger cooldownCalls = new AtomicInteger(0);
        final ItemStack held = new ItemStack(Material.BANNER);
        final Player player = createPlayer(held, cooldownCalls);
        final com.arkflame.smpweapons.model.WeaponDefinition weapon = weapon(Collections.singletonList("SHIELD"), null);

        final CooldownService service = new CooldownService(null, null);
        service.start(player, weapon, "primary", 5, false, held);

        assertEquals(0, cooldownCalls.get());
        assertTrue(service.remainingSeconds(player, weapon, "primary") > 0L);
    }

    private static Player createPlayer(final ItemStack held, final AtomicInteger cooldownCalls) {
        final InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                final String name = method.getName();
                if ("getUniqueId".equals(name)) {
                    return FIXED_UUID;
                }
                if ("getItemInHand".equals(name)) {
                    return held;
                }
                if ("setCooldown".equals(name)) {
                    cooldownCalls.incrementAndGet();
                    return null;
                }
                if ("isOnline".equals(name)) {
                    return Boolean.TRUE;
                }
                if ("equals".equals(name)) {
                    return args != null && args.length > 0 && args[0] == proxy;
                }
                if ("hashCode".equals(name)) {
                    return System.identityHashCode(proxy);
                }
                if ("toString".equals(name)) {
                    return "MockPlayer";
                }
                if (method.getReturnType() == boolean.class) {
                    return false;
                }
                if (method.getReturnType() == int.class) {
                    return 0;
                }
                if (method.getReturnType() == long.class) {
                    return 0L;
                }
                if (method.getReturnType() == double.class) {
                    return 0.0;
                }
                if (method.getReturnType() == float.class) {
                    return 0.0f;
                }
                return null;
            }
        };
        final ClassLoader loader = Player.class.getClassLoader();
        final Class<?>[] interfaces = new Class<?>[] { Player.class, CooldownCapablePlayer.class };
        return (Player) Proxy.newProxyInstance(loader, interfaces, handler);
    }

    private static com.arkflame.smpweapons.model.WeaponDefinition weapon(final List<String> materialAliases, final Boolean visualCooldown) {
        final YamlConfiguration config = new YamlConfiguration();
        final ConfigurationSection section = config.createSection("weapon");
        section.set("item.material", materialAliases);
        section.set("item.name", "Test Weapon");
        if (visualCooldown != null) {
            section.set("visual-cooldown", visualCooldown.booleanValue());
        }
        return com.arkflame.smpweapons.model.WeaponDefinition.from("test_weapon", section, "test.yml");
    }
}
