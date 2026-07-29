package com.arkflame.smpweapons.hook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class WorldGuardHook implements RegionProtectionProvider {
    private static final String WORLD_GUARD_PLUGIN_NAME = "WorldGuard";

    private final JavaPlugin plugin;
    private final boolean enabled;
    private final boolean debug;
    private Plugin cachedPlugin;
    private Object query;
    private boolean unavailableLogged;
    private boolean invalidApiLogged;

    public WorldGuardHook(final JavaPlugin plugin, final ConfigurationSection section) {
        this.plugin = plugin;
        this.enabled = section == null || section.getBoolean("enabled", true);
        this.debug = section != null && section.getBoolean("debug", false);
    }

    @Override
    public boolean isDenied(final Player player, final Location location) {
        if (!this.enabled || player == null || location == null || location.getWorld() == null) {
            return false;
        }
        final Object resolvedQuery = resolveQuery();
        if (resolvedQuery == null) {
            return false;
        }
        try {
            final Class<?> adapterClass = Class.forName("com.sk89q.worldguard.bukkit.BukkitAdapter");
            final Object adaptedLocation = adapterClass.getMethod("adapt", Location.class).invoke(null, location);
            final Object adaptedPlayer = adapterClass.getMethod("adapt", Player.class).invoke(null, player);
            final Class<?> flagsClass = Class.forName("com.sk89q.worldguard.protection.flags.Flags");
            final Field pvpField = flagsClass.getField("PVP");
            final Object state = queryState(resolvedQuery, adaptedLocation, adaptedPlayer, pvpField.get(null));
            return state != null && "DENY".equals(String.valueOf(state));
        } catch (final Exception exception) {
            logInvalidApi("Could not query WorldGuard pvp flag: " + exception.getClass().getSimpleName());
            return false;
        }
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    private Object resolveQuery() {
        final Plugin worldGuard = Bukkit.getPluginManager().getPlugin(WORLD_GUARD_PLUGIN_NAME);
        if (worldGuard == null || !worldGuard.isEnabled()) {
            if (this.debug && !this.unavailableLogged) {
                this.plugin.getLogger().info("WorldGuard hook enabled but WorldGuard is not loaded; allowing weapon usage.");
                this.unavailableLogged = true;
            }
            clearApi();
            return null;
        }
        if (worldGuard == this.cachedPlugin && this.query != null) {
            return this.query;
        }
        this.cachedPlugin = worldGuard;
        try {
            final Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            final Object worldGuardInstance = worldGuardClass.getMethod("getInstance").invoke(null);
            final Object platform = worldGuardInstance.getClass().getMethod("getPlatform").invoke(worldGuardInstance);
            final Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);
            this.query = container.getClass().getMethod("createQuery").invoke(container);
            return this.query;
        } catch (final Exception exception) {
            logInvalidApi("Could not initialize WorldGuard hook: " + exception.getClass().getSimpleName());
            this.query = null;
            return null;
        }
    }

    private static Object queryState(final Object query, final Object location,
                                     final Object player, final Object pvp) throws Exception {
        for (final Method method : query.getClass().getMethods()) {
            if ("queryState".equals(method.getName()) && method.getParameterTypes().length == 3) {
                final Class<?> flagsType = method.getParameterTypes()[2];
                if (!flagsType.isArray()) {
                    continue;
                }
                final Object flags = Array.newInstance(flagsType.getComponentType(), 1);
                Array.set(flags, 0, pvp);
                return method.invoke(query, new Object[]{location, player, flags});
            }
        }
        throw new NoSuchMethodException("queryState");
    }

    private void clearApi() {
        this.cachedPlugin = null;
        this.query = null;
    }

    private void logInvalidApi(final String message) {
        if (!this.invalidApiLogged) {
            this.plugin.getLogger().warning(message);
            this.invalidApiLogged = true;
        }
    }
}
