package com.arkflame.smpweapons.util;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Random;

public final class Particles {
    private static final Random RANDOM = new Random();
    private static final Class<?> PARTICLE_CLASS = classOrNull("org.bukkit.Particle");
    private static final Class<?> COLOR_CLASS = classOrNull("org.bukkit.Color");
    private static final Class<?> DUST_OPTIONS_CLASS = classOrNull("org.bukkit.Particle$DustOptions");
    private static Method colorFromRgb;
    private static Constructor<?> dustOptionsConstructor;

    private Particles() {
    }

    public static void spawn(final Location location, final String name, final int count) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        final World world = location.getWorld();
        if (tryModern(world, location, name, Math.max(1, count))) {
            return;
        }
        try {
            world.playEffect(location, Effect.SMOKE, 0);
        } catch (final Exception ignored) {
            // cosmetic only
        }
    }

    public static void cloud(final Location location, final int count, final double spread) {
        if (location == null) {
            return;
        }
        for (int i = 0; i < Math.max(1, count); i++) {
            final Location at = location.clone().add(random(spread), random(spread), random(spread));
            spawn(at, "CLOUD", 1);
        }
    }

    private static boolean tryModern(final World world, final Location location, final String rawName, final int count) {
        if (PARTICLE_CLASS == null || rawName == null) {
            return false;
        }
        final Object particle = resolveParticle(rawName);
        if (particle == null) {
            return false;
        }
        final Object dustData = dustData(rawName);
        if (dustData != null) {
            try {
                final Method method = world.getClass().getMethod("spawnParticle", PARTICLE_CLASS, Location.class, int.class, double.class, double.class, double.class, double.class, Object.class);
                method.invoke(world, particle, location, Integer.valueOf(count), Double.valueOf(0.0D), Double.valueOf(0.0D), Double.valueOf(0.0D), Double.valueOf(0.0D), dustData);
                return true;
            } catch (final Exception ignored) {
                // fallback below
            }
        }
        try {
            final Method method = world.getClass().getMethod("spawnParticle", PARTICLE_CLASS, Location.class, int.class, double.class, double.class, double.class, double.class);
            method.invoke(world, particle, location, Integer.valueOf(count), Double.valueOf(0.15D), Double.valueOf(0.15D), Double.valueOf(0.15D), Double.valueOf(0.0D));
            return true;
        } catch (final Exception ignored) {
            return false;
        }
    }

    private static Object resolveParticle(final String rawName) {
        final String normalized = normalize(rawName);
        final String[] aliases;
        if (isDustAlias(normalized)) {
            aliases = new String[]{"DUST", "REDSTONE", "CLOUD"};
        } else if ("ANGRY_VILLAGER".equals(normalized)) {
            aliases = new String[]{"VILLAGER_ANGRY", "ANGRY_VILLAGER", "CLOUD"};
        } else if ("FALLING_WATER".equals(normalized)) {
            aliases = new String[]{"FALLING_WATER", "WATER_SPLASH", "WATER_DROP", "CLOUD"};
        } else if ("WHITE_SMOKE".equals(normalized) || "SMOKE".equals(normalized)) {
            aliases = new String[]{"WHITE_SMOKE", "CAMPFIRE_COSY_SMOKE", "SMOKE_NORMAL", "SMOKE", "CLOUD"};
        } else if ("EXPLOSION".equals(normalized)) {
            aliases = new String[]{"EXPLOSION", "EXPLOSION_NORMAL", "EXPLOSION_LARGE", "CLOUD"};
        } else if ("SONIC_BOOM".equals(normalized)) {
            aliases = new String[]{"SONIC_BOOM", "EXPLOSION_LARGE", "EXPLOSION_NORMAL", "CLOUD"};
        } else if ("SCULK_SOUL".equals(normalized)) {
            aliases = new String[]{"SCULK_SOUL", "SPELL_WITCH", "PORTAL", "CLOUD"};
        } else if ("SOUL".equals(normalized)) {
            aliases = new String[]{"SOUL", "SPELL_WITCH", "SMOKE_NORMAL", "CLOUD"};
        } else if ("REVERSE_PORTAL".equals(normalized)) {
            aliases = new String[]{"REVERSE_PORTAL", "PORTAL", "CLOUD"};
        } else if ("DRAGON_BREATH".equals(normalized)) {
            aliases = new String[]{"DRAGON_BREATH", "SPELL_WITCH", "CLOUD"};
        } else if ("TOTEM".equals(normalized)) {
            aliases = new String[]{"TOTEM", "VILLAGER_HAPPY", "HAPPY_VILLAGER", "CLOUD"};
        } else {
            aliases = new String[]{normalized, "CLOUD"};
        }
        for (final String alias : aliases) {
            try {
                return Enum.valueOf((Class<Enum>) PARTICLE_CLASS, alias);
            } catch (final Exception ignored) {
                // next
            }
        }
        return null;
    }

    private static Object dustData(final String rawName) {
        final String normalized = normalize(rawName);
        if (!isDustAlias(normalized) || COLOR_CLASS == null || DUST_OPTIONS_CLASS == null) {
            return null;
        }
        try {
            if (colorFromRgb == null) {
                colorFromRgb = COLOR_CLASS.getMethod("fromRGB", int.class, int.class, int.class);
            }
            if (dustOptionsConstructor == null) {
                dustOptionsConstructor = DUST_OPTIONS_CLASS.getConstructor(COLOR_CLASS, float.class);
            }
            final int[] rgb = rgb(normalized);
            final Object color = colorFromRgb.invoke(null, Integer.valueOf(rgb[0]), Integer.valueOf(rgb[1]), Integer.valueOf(rgb[2]));
            return dustOptionsConstructor.newInstance(color, Float.valueOf(size(normalized)));
        } catch (final Exception ignored) {
            return null;
        }
    }

    private static boolean isDustAlias(final String normalized) {
        return normalized.endsWith("_DUST") || "DUST".equals(normalized) || "REDSTONE".equals(normalized);
    }

    private static int[] rgb(final String normalized) {
        if ("BLACK_DUST".equals(normalized)) {
            return new int[]{0, 0, 0};
        }
        if ("GREEN_DUST".equals(normalized) || "VENOM_DUST".equals(normalized)) {
            return new int[]{0, 170, 0};
        }
        if ("RED_DUST".equals(normalized)) {
            return new int[]{222, 66, 66};
        }
        if ("PURPLE_DUST".equals(normalized)) {
            return new int[]{128, 0, 192};
        }
        if ("YELLOW_DUST".equals(normalized)) {
            return new int[]{255, 210, 0};
        }
        if ("ORANGE_DUST".equals(normalized)) {
            return new int[]{255, 170, 0};
        }
        return new int[]{255, 255, 255};
    }

    private static float size(final String normalized) {
        if ("BLACK_DUST".equals(normalized) || "GREEN_DUST".equals(normalized) || "PURPLE_DUST".equals(normalized) || "YELLOW_DUST".equals(normalized) || "WHITE_DUST".equals(normalized)) {
            return 2.0F;
        }
        return 1.0F;
    }

    private static String normalize(final String rawName) {
        return rawName == null ? "CLOUD" : rawName.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static double random(final double spread) {
        return (RANDOM.nextDouble() - 0.5D) * spread * 2.0D;
    }

    private static Class<?> classOrNull(final String name) {
        try {
            return Class.forName(name);
        } catch (final ClassNotFoundException ignored) {
            return null;
        }
    }
}
