package com.arkflame.smpweapons.listener;

import com.arkflame.smpweapons.SMPWeaponsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;

public final class GlideToggleListener implements Listener {
    private final SMPWeaponsPlugin plugin;

    public GlideToggleListener(final SMPWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    public void handle(final Event event) {
        if (event == null || !"org.bukkit.event.entity.EntityToggleGlideEvent".equals(event.getClass().getName())) {
            return;
        }
        final Object entity = invoke(event, "getEntity");
        if (!(entity instanceof Player)) {
            return;
        }
        final Object gliding = invoke(event, "isGliding");
        if (Boolean.TRUE.equals(gliding)) {
            return;
        }
        final Player player = (Player) entity;
        if (this.plugin.getGlideService() != null && this.plugin.getGlideService().shouldCancelDisable(player) && event instanceof Cancellable) {
            ((Cancellable) event).setCancelled(true);
        }
    }

    private static Object invoke(final Object target, final String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (final Exception ignored) {
            return null;
        }
    }
}
