package com.arkflame.smpweapons.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;

public final class Entities {
    private Entities() {
    }

    public static void pushForward(final Player player, final double force) {
        if (player == null) {
            return;
        }
        final Vector direction = player.getLocation().getDirection().normalize().multiply(force);
        player.setVelocity(player.getVelocity().add(direction));
    }

    public static void push(final Entity entity, final double x, final double y, final double z) {
        if (entity == null) {
            return;
        }
        entity.setVelocity(entity.getVelocity().add(new Vector(x, y, z)));
    }

    public static void pushAway(final Entity victim, final Location source, final double force) {
        if (victim == null || source == null) {
            return;
        }
        final Vector vector = victim.getLocation().toVector().subtract(source.toVector());
        if (vector.lengthSquared() <= 0.000001D) {
            vector.setY(0.2D);
        }
        victim.setVelocity(victim.getVelocity().add(vector.normalize().multiply(force)));
    }

    public static void rawDamage(final LivingEntity entity, final double damage) {
        if (entity == null || damage <= 0.0D) {
            return;
        }
        if (entity.getHealth() <= damage) {
            entity.damage(70.0D);
            return;
        }
        try {
            entity.damage(0.0D);
        } catch (final Exception ignored) {
            // hurt animation is cosmetic
        }
        entity.setHealth(Math.max(0.0D, entity.getHealth() - damage));
    }

    public static void setGliding(final Player player, final boolean gliding) {
        invoke(player, "setGliding", boolean.class, Boolean.valueOf(gliding));
    }

    public static void setGlowing(final Entity entity, final boolean glowing) {
        invoke(entity, "setGlowing", boolean.class, Boolean.valueOf(glowing));
    }

    private static void invoke(final Object target, final String methodName, final Class<?> parameterType, final Object value) {
        if (target == null) {
            return;
        }
        try {
            final Method method = target.getClass().getMethod(methodName, parameterType);
            method.invoke(target, value);
        } catch (final Exception ignored) {
            // older server, feature absent
        }
    }
}
