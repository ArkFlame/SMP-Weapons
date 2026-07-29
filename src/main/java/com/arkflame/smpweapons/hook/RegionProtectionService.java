package com.arkflame.smpweapons.hook;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class RegionProtectionService {
    private final boolean enabled;
    private final boolean failOpen;
    private final List<RegionProtectionProvider> providers;

    public RegionProtectionService(final Collection<RegionProtectionProvider> providers,
                                   final boolean enabled, final boolean failOpen) {
        this.enabled = enabled;
        this.failOpen = failOpen;
        final List<RegionProtectionProvider> copy = new ArrayList<RegionProtectionProvider>();
        if (providers != null) {
            for (final RegionProtectionProvider provider : providers) {
                if (provider != null) {
                    copy.add(provider);
                }
            }
        }
        this.providers = Collections.unmodifiableList(copy);
    }

    public boolean isAbilityActivationDenied(final Player source, final Location actionLocation) {
        return isLocationDenied(source, actionLocation);
    }

    public boolean isWorldEffectDenied(final Player source, final Location effectLocation) {
        return isLocationDenied(source, effectLocation);
    }

    public boolean isDamageDenied(final Player source, final LivingEntity target) {
        return isTargetDenied(source, target);
    }

    public boolean isEffectDenied(final Player source, final LivingEntity target) {
        return isTargetDenied(source, target);
    }

    private boolean isLocationDenied(final Player source, final Location actionLocation) {
        if (!this.enabled || source == null || actionLocation == null) {
            return false;
        }
        final Location sourceLocation = source.getLocation();
        if (sourceLocation == null) {
            return false;
        }
        if (isDeniedByProviders(source, sourceLocation)) {
            return true;
        }
        return !isSameBlock(sourceLocation, actionLocation)
                && isDeniedByProviders(source, actionLocation);
    }

    private boolean isTargetDenied(final Player source, final LivingEntity target) {
        if (!(target instanceof Player) || !this.enabled || source == null) {
            return false;
        }
        final Player targetPlayer = (Player) target;
        final Location sourceLocation = source.getLocation();
        final Location targetLocation = targetPlayer.getLocation();
        if (sourceLocation == null || targetLocation == null) {
            return false;
        }
        if (isDeniedByProviders(source, sourceLocation)) {
            return true;
        }
        return !isSameBlock(sourceLocation, targetLocation)
                && isDeniedByProviders(targetPlayer, targetLocation);
    }

    private boolean isDeniedByProviders(final Player player, final Location location) {
        for (final RegionProtectionProvider provider : this.providers) {
            try {
                if (provider.isDenied(player, location)) {
                    return true;
                }
            } catch (final RuntimeException exception) {
                if (!this.failOpen) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSameBlock(final Location first, final Location second) {
        return first.getWorld() != null && first.getWorld().equals(second.getWorld())
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }
}
