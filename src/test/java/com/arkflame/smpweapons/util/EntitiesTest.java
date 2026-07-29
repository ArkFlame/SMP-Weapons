package com.arkflame.smpweapons.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class EntitiesTest {
    private Location location;
    private Vector velocity;

    @Test
    void pushAwayWithLiftAddsOutwardHorizontalAndVerticalImpulse() {
        location = new Location(null, 3.0D, 5.0D, 4.0D);
        velocity = new Vector(0.10D, 0.20D, -0.10D);

        Entities.pushAwayWithLift(entity(), new Location(null, 0.0D, 5.0D, 0.0D), 0.35D, 0.18D);

        assertEquals(0.0D, velocity.distance(new Vector(0.31D, 0.38D, 0.18D)), 1E-12);
    }

    @Test
    void pushAwayWithLiftAtExplosionCenterAddsOnlyLift() {
        location = new Location(null, 2.0D, 5.0D, -3.0D);
        velocity = new Vector(0.20D, -0.10D, 0.30D);

        Entities.pushAwayWithLift(entity(), new Location(null, 2.0D, 5.0D, -3.0D), 0.35D, 0.18D);

        assertEquals(0.0D, velocity.distance(new Vector(0.20D, 0.08D, 0.30D)), 1E-12);
    }

    private Entity entity() {
        return (Entity) Proxy.newProxyInstance(
                Entity.class.getClassLoader(),
                new Class<?>[]{Entity.class},
                (proxy, method, arguments) -> {
                    if ("getLocation".equals(method.getName())) {
                        return location.clone();
                    }
                    if ("getVelocity".equals(method.getName())) {
                        return velocity.clone();
                    }
                    if ("setVelocity".equals(method.getName())) {
                        velocity = ((Vector) arguments[0]).clone();
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
