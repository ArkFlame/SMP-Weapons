package com.arkflame.smpweapons.hook;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RegionProtectionServiceTest {

    @Test
    void providerCompositionUsesOrAndKeepsPoliciesIndependent() {
        final World world = proxy(World.class, null);
        final Location sourceLocation = new Location(world, 1, 2, 3);
        final Location actionLocation = new Location(world, 4, 5, 6);
        final Player source = player(sourceLocation);
        final RegionProtectionProvider sourcePolicy = (player, location) -> location.equals(sourceLocation);
        final RegionProtectionProvider actionPolicy = (player, location) -> location.equals(actionLocation);

        assertTrue(new RegionProtectionService(Arrays.asList(sourcePolicy, actionPolicy), true, true)
                .isAbilityActivationDenied(source, actionLocation));
        assertTrue(new RegionProtectionService(Arrays.asList(sourcePolicy), true, true).isAbilityActivationDenied(source, actionLocation));
        assertFalse(new RegionProtectionService(Arrays.asList(actionPolicy), true, true).isAbilityActivationDenied(source, sourceLocation));
    }

    @Test
    void sameBlockActionIsQueriedOnce() {
        final World world = proxy(World.class, null);
        final Location sourceLocation = new Location(world, 1, 2, 3, 0.1F, 0.2F);
        final Location actionLocation = new Location(world, 1, 2, 3, 0.8F, 0.9F);
        final Player source = player(sourceLocation);
        final int[] calls = new int[1];
        final RegionProtectionProvider provider = (player, location) -> {
            calls[0]++;
            return false;
        };

        assertFalse(new RegionProtectionService(Arrays.asList(provider), true, true).isAbilityActivationDenied(source, actionLocation));
        assertTrue(calls[0] == 1);
    }

    @Test
    void targetGateUsesTargetPlayer() {
        final World world = proxy(World.class, null);
        final Player source = player(new Location(world, 1, 2, 3));
        final Player target = player(new Location(world, 4, 5, 6));
        final RegionProtectionProvider provider = (player, location) -> player == target;

        assertTrue(new RegionProtectionService(Arrays.asList(provider), true, true).isDamageDenied(source, target));
        assertTrue(new RegionProtectionService(Arrays.asList(provider), true, true).isEffectDenied(source, target));

        final LivingEntity nonPlayer = proxy(LivingEntity.class, null);
        assertFalse(new RegionProtectionService(Arrays.asList(provider), true, true).isDamageDenied(source, nonPlayer));
    }

    @Test
    void nullInputsFailOpen() {
        final RegionProtectionService service = new RegionProtectionService(
                Arrays.asList((RegionProtectionProvider) (player, location) -> true), true, true);

        assertFalse(service.isAbilityActivationDenied(null, null));
        assertFalse(service.isDamageDenied(null, (LivingEntity) null));
    }

    private static Player player(final Location location) {
        return proxy(Player.class, (proxy, method, arguments) -> {
            if ("getLocation".equals(method.getName())) {
                return location;
            }
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(final Class<T> type, final java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, arguments) -> {
            if ("equals".equals(method.getName())) {
                return arguments != null && arguments.length == 1 && proxy == arguments[0];
            }
            if ("hashCode".equals(method.getName())) {
                return System.identityHashCode(proxy);
            }
            if ("toString".equals(method.getName())) {
                return type.getSimpleName() + "Proxy";
            }
            return handler == null ? null : handler.invoke(proxy, method, arguments);
        });
    }
}
