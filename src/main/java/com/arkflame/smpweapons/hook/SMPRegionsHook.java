package com.arkflame.smpweapons.hook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class SMPRegionsHook {
    private static final String REGIONS_PLUGIN_NAME = "SMPRegions";
    private static final String PVP_FLAG = "pvp";

    private final JavaPlugin plugin;
    private final boolean enabled;
    private final boolean denyWhenPvpDenied;
    private final boolean debug;
    private final Set<String> blacklistedRegions;

    private Object api;
    private Method resolveStateMethod;
    private Method applicableMethod;
    private boolean unavailableLogged;
    private boolean invalidApiLogged;

    public SMPRegionsHook(final JavaPlugin plugin, final ConfigurationSection section) {
        this.plugin = plugin;
        this.enabled = section == null || section.getBoolean("enabled", true);
        this.denyWhenPvpDenied = section == null || section.getBoolean("deny-when-pvp-denied", true);
        this.debug = section != null && section.getBoolean("debug", false);
        this.blacklistedRegions = normalizeBlacklist(section == null ? Collections.<String>emptyList() : section.getStringList("blacklist-regions"));
    }

    public boolean isAllowed(final Player player, final Location actionLocation) {
        if (!this.enabled || player == null) {
            return true;
        }
        final Object resolvedApi = resolveApi();
        if (resolvedApi == null) {
            return true;
        }
        if (isDeniedAt(resolvedApi, player.getLocation())) {
            return false;
        }
        return actionLocation == null || isSameBlock(player.getLocation(), actionLocation) || !isDeniedAt(resolvedApi, actionLocation);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    private boolean isDeniedAt(final Object resolvedApi, final Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (this.denyWhenPvpDenied && isPvpDenied(resolvedApi, location)) {
            return true;
        }
        return !this.blacklistedRegions.isEmpty() && isBlacklisted(resolvedApi, location);
    }

    private boolean isPvpDenied(final Object resolvedApi, final Location location) {
        try {
            final Object state = this.resolveStateMethod.invoke(resolvedApi, location, PVP_FLAG);
            return state != null && "DENY".equalsIgnoreCase(String.valueOf(state));
        } catch (final Exception exception) {
            logInvalidApi("Could not query SMP Regions pvp flag: " + exception.getClass().getSimpleName());
            return false;
        }
    }

    private boolean isBlacklisted(final Object resolvedApi, final Location location) {
        try {
            final Object rawRegions = this.applicableMethod.invoke(resolvedApi, location);
            if (!(rawRegions instanceof Collection)) {
                return false;
            }
            for (final Object region : (Collection<?>) rawRegions) {
                if (matchesBlacklistedRegion(region)) {
                    return true;
                }
            }
            return false;
        } catch (final Exception exception) {
            logInvalidApi("Could not query SMP Regions applicable regions: " + exception.getClass().getSimpleName());
            return false;
        }
    }

    private boolean matchesBlacklistedRegion(final Object region) {
        if (region == null) {
            return false;
        }
        final String name = invokeString(region, "name");
        if (!name.isEmpty() && this.blacklistedRegions.contains(normalize(name))) {
            return true;
        }
        final String key = invokeString(region, "key");
        return !key.isEmpty() && this.blacklistedRegions.contains(normalize(key));
    }

    private String invokeString(final Object target, final String methodName) {
        try {
            final Method method = target.getClass().getMethod(methodName);
            final Object value = method.invoke(target);
            return value == null ? "" : String.valueOf(value);
        } catch (final Exception ignored) {
            return "";
        }
    }

    private Object resolveApi() {
        final Plugin regions = Bukkit.getPluginManager().getPlugin(REGIONS_PLUGIN_NAME);
        if (regions == null || !regions.isEnabled()) {
            if (this.debug && !this.unavailableLogged) {
                this.plugin.getLogger().info("SMP Regions hook enabled but SMPRegions is not loaded; allowing weapon usage.");
                this.unavailableLogged = true;
            }
            clearApi();
            return null;
        }
        try {
            final Method apiMethod = regions.getClass().getMethod("api");
            final Object resolvedApi = apiMethod.invoke(regions);
            if (resolvedApi == null) {
                logInvalidApi("SMPRegions.api() returned null.");
                clearApi();
                return null;
            }
            if (resolvedApi != this.api) {
                this.api = resolvedApi;
                this.resolveStateMethod = resolvedApi.getClass().getMethod("resolveState", Location.class, String.class);
                this.applicableMethod = resolvedApi.getClass().getMethod("applicable", Location.class);
                this.invalidApiLogged = false;
            }
            return this.api;
        } catch (final Exception exception) {
            logInvalidApi("Could not initialize SMP Regions hook: " + exception.getClass().getSimpleName());
            clearApi();
            return null;
        }
    }

    private void clearApi() {
        this.api = null;
        this.resolveStateMethod = null;
        this.applicableMethod = null;
    }

    private void logInvalidApi(final String message) {
        if (!this.invalidApiLogged) {
            this.plugin.getLogger().warning(message);
            this.invalidApiLogged = true;
        }
    }

    private static Set<String> normalizeBlacklist(final List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }
        final Set<String> output = new HashSet<String>();
        for (final String value : values) {
            final String normalized = normalize(value);
            if (!normalized.isEmpty()) {
                output.add(normalized);
            }
        }
        return Collections.unmodifiableSet(output);
    }

    private static String normalize(final String input) {
        return input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isSameBlock(final Location first, final Location second) {
        if (first == null || second == null || first.getWorld() == null || second.getWorld() == null) {
            return false;
        }
        return first.getWorld().equals(second.getWorld())
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }
}