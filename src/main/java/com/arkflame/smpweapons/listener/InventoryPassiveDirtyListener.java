package com.arkflame.smpweapons.listener;

import com.arkflame.smpweapons.SMPWeaponsPlugin;
import com.arkflame.smpweapons.ability.InventoryPassiveService;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.InventoryView;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public final class InventoryPassiveDirtyListener implements Listener {
    private static final int OFF_HAND_SLOT = 40;
    private final SMPWeaponsPlugin plugin;

    public InventoryPassiveDirtyListener(final SMPWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    private InventoryPassiveService service() {
        return this.plugin.getInventoryPassiveService();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        markFull(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(final PlayerQuitEvent event) {
        clear(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(final PlayerDeathEvent event) {
        clear(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(final PlayerRespawnEvent event) {
        markFull(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(final InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            markFull((Player) event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        final Player player = (Player) event.getWhoClicked();
        if (isBroadClick(event)) {
            markFull(player);
            return;
        }
        final Set<Integer> slots = new HashSet<Integer>();
        final int mapped = logicalSlotFromRaw(event.getView(), event.getRawSlot());
        if (mapped >= 0) {
            slots.add(Integer.valueOf(mapped));
        }
        final int hotbarButton = event.getHotbarButton();
        if (hotbarButton >= 0) {
            slots.add(Integer.valueOf(hotbarButton));
        }
        if (slots.isEmpty()) {
            markFull(player);
            return;
        }
        service().markSlots(player, slots);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrag(final InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        final Player player = (Player) event.getWhoClicked();
        final Set<Integer> playerSlots = new HashSet<Integer>();
        for (final Integer raw : event.getRawSlots()) {
            if (raw == null) {
                continue;
            }
            final int mapped = logicalSlotFromRaw(event.getView(), raw.intValue());
            if (mapped >= 0) {
                playerSlots.add(Integer.valueOf(mapped));
            }
        }
        if (!playerSlots.isEmpty()) {
            service().markSlots(player, playerSlots);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(final PlayerDropItemEvent event) {
        markFull(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(final PlayerPickupItemEvent event) {
        markFull(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeld(final PlayerItemHeldEvent event) {
        final Player player = event.getPlayer();
        service().markSlot(player, event.getPreviousSlot());
        service().markSlot(player, event.getNewSlot());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(final PlayerItemConsumeEvent event) {
        markFull(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(final PlayerInteractEvent event) {
        final String action = event.getAction() == null ? "" : event.getAction().name();
        if (action.startsWith("RIGHT_CLICK")) {
            final Player player = event.getPlayer();
            service().markMainHand(player);
            service().markOffHand(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        service().markMainHand(event.getPlayer());
    }

    public void handleSwapHandItems(final Event event) {
        final Object rawPlayer = invokeNoArg(event, "getPlayer");
        if (rawPlayer instanceof Player) {
            final Player player = (Player) rawPlayer;
            service().markMainHand(player);
            service().markOffHand(player);
        }
    }

    private void markFull(final Player player) {
        if (player == null) {
            return;
        }
        service().markFullScan(player);
    }

    private void clear(final Player player) {
        if (player == null) {
            return;
        }
        service().clear(player);
    }

    private int logicalSlotFromRaw(final InventoryView view, final int rawSlot) {
        if (view == null || rawSlot < 0) {
            return -1;
        }
        if (rawSlot < view.getTopInventory().getSize()) {
            return -1;
        }
        final int converted = view.convertSlot(rawSlot);
        if (converted >= 0 && converted <= 39) {
            return converted;
        }
        return -1;
    }

    private boolean isBroadClick(final InventoryClickEvent event) {
        final String action = event.getAction() == null ? "" : event.getAction().name();
        final String click = event.getClick() == null ? "" : event.getClick().name();
        if (action.indexOf("MOVE_TO_OTHER_INVENTORY") >= 0) {
            return true;
        }
        if (action.indexOf("COLLECT_TO_CURSOR") >= 0) {
            return true;
        }
        if (click.indexOf("SHIFT") >= 0) {
            return true;
        }
        if (click.indexOf("DOUBLE") >= 0) {
            return true;
        }
        return false;
    }

    private static Object invokeNoArg(final Object target, final String method) {
        if (target == null || method == null) {
            return null;
        }
        try {
            final Method resolved = target.getClass().getMethod(method);
            return resolved.invoke(target);
        } catch (final Throwable ignored) {
            return null;
        }
    }
}
