package com.arkflame.smpweapons.ability;

import com.arkflame.smpweapons.util.Entities;
import com.arkflame.smpweapons.util.FoliaAPI;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class GlideService {
    private final FoliaAPI scheduler;
    private final Map<UUID, GlideSession> sessions = new ConcurrentHashMap<UUID, GlideSession>();
    private final AtomicLong sequence = new AtomicLong();

    public GlideService(final FoliaAPI scheduler) {
        this.scheduler = scheduler;
    }

    public void start(final Player player, final int maxTicks) {
        if (player == null || maxTicks <= 0) {
            return;
        }
        final UUID uuid = player.getUniqueId();
        final GlideSession session = new GlideSession(this.sequence.incrementAndGet(), Math.max(1, maxTicks));
        this.sessions.put(uuid, session);
        sustain(player, uuid, session, 0);
    }

    public boolean isActive(final Player player) {
        return player != null && this.sessions.containsKey(player.getUniqueId());
    }

    public boolean shouldCancelDisable(final Player player) {
        return isActive(player);
    }

    public void stop(final Player player) {
        if (player == null) {
            return;
        }
        this.sessions.remove(player.getUniqueId());
        Entities.setGliding(player, false);
    }

    public void clear(final UUID uuid) {
        if (uuid != null) {
            this.sessions.remove(uuid);
        }
    }

    public void clearAll() {
        this.sessions.clear();
    }

    private void sustain(final Player player, final UUID uuid, final GlideSession session, final int tick) {
        final GlideSession current = this.sessions.get(uuid);
        if (current == null || current.id != session.id) {
            return;
        }
        if (!player.isOnline() || player.isOnGround() || tick >= session.maxTicks) {
            stop(player);
            return;
        }
        player.setFallDistance(0.0F);
        Entities.setGliding(player, true);
        this.scheduler.runEntityLater(player, new Runnable() {
            @Override
            public void run() {
                sustain(player, uuid, session, tick + 1);
            }
        }, null, 1L);
    }

    private static final class GlideSession {
        private final long id;
        private final int maxTicks;

        private GlideSession(final long id, final int maxTicks) {
            this.id = id;
            this.maxTicks = maxTicks;
        }
    }
}
