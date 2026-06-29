package com.arkflame.smpweapons.listener;

import com.arkflame.smpweapons.SMPWeaponsPlugin;
import com.arkflame.smpweapons.ability.AbilityItemProtectionService;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

public final class AbilityItemProtectionListener implements Listener {
    private static final String MAIN_HAND_METHOD = "getMainHandItem";
    private static final String OFF_HAND_METHOD = "getOffHandItem";
    private final SMPWeaponsPlugin plugin;

    public AbilityItemProtectionListener(final SMPWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    private AbilityItemProtectionService service() {
        return plugin.getAbilityItemProtectionService();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(final InventoryClickEvent event) {
        final AbilityItemProtectionService svc = service();
        if (svc == null) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        final Player player = (Player) event.getWhoClicked();
        final ItemStack current = event.getCurrentItem();
        final ItemStack cursor = event.getCursor();
        final InventoryView view = event.getView();
        final int rawSlot = event.getRawSlot();
        final int hotbarButton = event.getHotbarButton();
        boolean cancel = false;
        if (svc.isProtected(player, current)) {
            cancel = true;
        } else if (svc.isProtected(player, cursor)) {
            cancel = true;
        } else if (svc.isProtectedRawSlot(player, view, rawSlot)) {
            cancel = true;
        } else if (hotbarButton >= 0) {
            final ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
            if (svc.isProtected(player, hotbarItem)) {
                cancel = true;
            }
        }
        if (cancel) {
            cancelAndResync(event, player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(final InventoryDragEvent event) {
        final AbilityItemProtectionService svc = service();
        if (svc == null) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        final Player player = (Player) event.getWhoClicked();
        final InventoryView view = event.getView();
        boolean cancel = false;
        if (svc.isProtected(player, event.getOldCursor())) {
            cancel = true;
        } else {
            final Map<Integer, ItemStack> newItems = event.getNewItems();
            if (newItems != null) {
                for (final ItemStack value : newItems.values()) {
                    if (svc.isProtected(player, value)) {
                        cancel = true;
                        break;
                    }
                }
            }
        }
        if (!cancel) {
            final Set<Integer> rawSlots = event.getRawSlots();
            if (rawSlots != null) {
                for (final Integer rawSlot : rawSlots) {
                    if (rawSlot == null) {
                        continue;
                    }
                    if (svc.isProtectedRawSlot(player, view, rawSlot.intValue())) {
                        cancel = true;
                        break;
                    }
                }
            }
        }
        if (cancel) {
            cancelAndResync(event, player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrop(final PlayerDropItemEvent event) {
        final AbilityItemProtectionService svc = service();
        if (svc == null) {
            return;
        }
        final ItemStack dropped = event.getItemDrop() == null ? null : event.getItemDrop().getItemStack();
        if (svc.isProtected(event.getPlayer(), dropped)) {
            cancelAndResync(event, event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onHeld(final PlayerItemHeldEvent event) {
        final AbilityItemProtectionService svc = service();
        if (svc == null) {
            return;
        }
        final Player player = event.getPlayer();
        final int previousSlot = event.getPreviousSlot();
        final ItemStack previous = previousSlot >= 0 ? player.getInventory().getItem(previousSlot) : null;
        if (svc.isProtected(player, previous)) {
            cancelAndResync(event, player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(final PlayerQuitEvent event) {
        final AbilityItemProtectionService svc = service();
        if (svc == null) {
            return;
        }
        svc.clear(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(final PlayerDeathEvent event) {
        final AbilityItemProtectionService svc = service();
        if (svc == null) {
            return;
        }
        svc.clear(event.getEntity());
    }

    public void handleSwapHandItems(final Event event) {
        final AbilityItemProtectionService svc = service();
        if (svc == null || event == null) {
            return;
        }
        final Object rawPlayer = invokeNoArg(event, "getPlayer");
        if (!(rawPlayer instanceof Player)) {
            return;
        }
        final Player player = (Player) rawPlayer;
        final ItemStack main = asItemStack(invokeNoArg(event, MAIN_HAND_METHOD));
        final ItemStack off = asItemStack(invokeNoArg(event, OFF_HAND_METHOD));
        boolean cancel = false;
        if (svc.isProtected(player, main)) {
            cancel = true;
        } else if (svc.isProtected(player, off)) {
            cancel = true;
        }
        if (cancel && event instanceof Cancellable) {
            cancelAndResync((Cancellable) event, player);
        }
    }

    private void cancelAndResync(final Cancellable event, final Player player) {
        event.setCancelled(true);
        final long delay = Math.max(1L, plugin.getConfig().getLong("ability-item-protection.update-inventory-delay-ticks", 1L));
        plugin.getSchedulerBridge().runEntityLater(player, new Runnable() {
            @Override
            public void run() {
                if (player == null || !player.isOnline()) {
                    return;
                }
                try {
                    player.updateInventory();
                } catch (final Throwable ignored) {
                }
            }
        }, null, delay);
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

    private static ItemStack asItemStack(final Object raw) {
        if (raw instanceof ItemStack) {
            return (ItemStack) raw;
        }
        return null;
    }
}