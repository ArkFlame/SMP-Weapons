package com.arkflame.smpweapons.ability;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FallProtectionService {
    private final Set<UUID> protectedPlayers = new HashSet<UUID>();

    public void protect(final UUID uuid) {
        if (uuid != null) {
            protectedPlayers.add(uuid);
        }
    }

    public void unprotect(final UUID uuid) {
        if (uuid != null) {
            protectedPlayers.remove(uuid);
        }
    }

    public boolean isProtected(final UUID uuid) {
        return uuid != null && protectedPlayers.contains(uuid);
    }
}
