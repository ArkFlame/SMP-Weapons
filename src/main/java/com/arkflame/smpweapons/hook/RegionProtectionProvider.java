package com.arkflame.smpweapons.hook;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface RegionProtectionProvider {
    boolean isDenied(Player player, Location location);
}
