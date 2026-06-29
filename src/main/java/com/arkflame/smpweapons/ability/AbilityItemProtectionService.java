package com.arkflame.smpweapons.ability;

import com.arkflame.smpweapons.SMPWeaponsPlugin;
import com.arkflame.smpweapons.model.WeaponDefinition;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class AbilityItemProtectionService {
    private static final int SLOT_MAIN_HAND = 0;
    private static final int SLOT_HOTBAR = 1;
    private static final int SLOT_OFF_HAND = 2;
    private static final int SLOT_UNKNOWN = -1;

    private final SMPWeaponsPlugin plugin;
    private final boolean enabled;
    private final long staleTimeoutTicks;

    private final Map<UUID, Map<Long, ProtectionState>> protections = new HashMap<UUID, Map<Long, ProtectionState>>();
    private long nextId = 1L;

    public AbilityItemProtectionService(final SMPWeaponsPlugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("ability-item-protection.enabled", true);
        this.staleTimeoutTicks = Math.max(1L, plugin.getConfig().getLong("ability-item-protection.stale-timeout-ticks", 2L));
    }

    public synchronized Protection start(final Player player, final WeaponDefinition weapon, final ItemStack sourceItem) {
        if (!this.enabled || player == null || weapon == null || sourceItem == null || sourceItem.getType() == Material.AIR) {
            return Protection.inactive();
        }
        final long now = System.currentTimeMillis();
        cleanupExpired(now);

        final long stateId = this.nextId++;
        final ItemStack snapshot = sourceItem.clone();
        final int[] slotInfo = locateSourceSlot(player, snapshot, weapon);
        final ProtectionState state = new ProtectionState(
                player.getUniqueId(),
                weapon.getId(),
                snapshot,
                weapon,
                now,
                now + this.staleTimeoutTicks * 50L,
                slotInfo[0],
                slotInfo[1]);

        Map<Long, ProtectionState> perPlayer = this.protections.get(player.getUniqueId());
        if (perPlayer == null) {
            perPlayer = new HashMap<Long, ProtectionState>();
            this.protections.put(player.getUniqueId(), perPlayer);
        }
        perPlayer.put(Long.valueOf(stateId), state);
        return new Protection(this, player.getUniqueId(), stateId, true, weapon.getId());
    }

    public synchronized boolean sourceStillPresent(final Protection protection) {
        if (protection == null || !protection.isActive() || !this.enabled) {
            return true;
        }
        cleanupExpired(System.currentTimeMillis());
        final UUID playerId = protection.getPlayerId();
        if (playerId == null) {
            return true;
        }
        final Map<Long, ProtectionState> perPlayer = this.protections.get(playerId);
        if (perPlayer == null) {
            return true;
        }
        final ProtectionState state = perPlayer.get(Long.valueOf(protection.stateId));
        if (state == null) {
            return true;
        }
        final Player player = resolvePlayer(playerId);
        if (player == null) {
            return true;
        }
        if (slotStillMatches(player, state)) {
            return true;
        }
        return inventoryContainsMatchingWeapon(player, state);
    }

    public synchronized boolean isProtected(final Player player, final ItemStack candidate) {
        if (player == null || candidate == null || candidate.getType() == Material.AIR || !this.enabled) {
            return false;
        }
        cleanupExpired(System.currentTimeMillis());
        final Map<Long, ProtectionState> perPlayer = this.protections.get(player.getUniqueId());
        if (perPlayer == null) {
            return false;
        }
        final Iterator<ProtectionState> iterator = perPlayer.values().iterator();
        while (iterator.hasNext()) {
            final ProtectionState state = iterator.next();
            if (matches(state, candidate)) {
                return true;
            }
        }
        return false;
    }

    public boolean isProtectedRawSlot(final Player player, final InventoryView view, final int rawSlot) {
        if (player == null || view == null || rawSlot < 0) {
            return false;
        }
        ItemStack item = null;
        try {
            item = view.getItem(rawSlot);
        } catch (final Throwable ignored) {
            return false;
        }
        return isProtected(player, item);
    }

    public synchronized boolean hasProtection(final Player player) {
        if (player == null || !this.enabled) {
            return false;
        }
        cleanupExpired(System.currentTimeMillis());
        final Map<Long, ProtectionState> perPlayer = this.protections.get(player.getUniqueId());
        return perPlayer != null && !perPlayer.isEmpty();
    }

    public synchronized void clear(final Player player) {
        if (player == null) {
            return;
        }
        this.protections.remove(player.getUniqueId());
    }

    public synchronized void clearAll() {
        this.protections.clear();
    }

    private synchronized void finish(final UUID playerId, final long stateId) {
        if (playerId == null || stateId < 0L) {
            return;
        }
        final Map<Long, ProtectionState> perPlayer = this.protections.get(playerId);
        if (perPlayer == null) {
            return;
        }
        perPlayer.remove(Long.valueOf(stateId));
        if (perPlayer.isEmpty()) {
            this.protections.remove(playerId);
        }
    }

    private void cleanupExpired(final long now) {
        final Iterator<Map.Entry<UUID, Map<Long, ProtectionState>>> players = this.protections.entrySet().iterator();
        while (players.hasNext()) {
            final Map.Entry<UUID, Map<Long, ProtectionState>> entry = players.next();
            final Map<Long, ProtectionState> perPlayer = entry.getValue();
            final Iterator<Map.Entry<Long, ProtectionState>> states = perPlayer.entrySet().iterator();
            while (states.hasNext()) {
                final Map.Entry<Long, ProtectionState> stateEntry = states.next();
                if (stateEntry.getValue().expiresAtMillis <= now) {
                    states.remove();
                }
            }
            if (perPlayer.isEmpty()) {
                players.remove();
            }
        }
    }

    private int[] locateSourceSlot(final Player player, final ItemStack snapshot, final WeaponDefinition weapon) {
        final ItemStack mainHand = player.getItemInHand();
        if (matchesSnapshot(snapshot, mainHand, weapon)) {
            return new int[] { SLOT_MAIN_HAND, player.getInventory().getHeldItemSlot() };
        }
        final ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (matchesSnapshot(snapshot, contents[i], weapon)) {
                return new int[] { SLOT_HOTBAR, i };
            }
        }
        ItemStack off = null;
        try {
            final Method method = player.getInventory().getClass().getMethod("getItemInOffHand");
            final Object raw = method.invoke(player.getInventory());
            if (raw instanceof ItemStack) {
                off = (ItemStack) raw;
            }
        } catch (final Throwable ignored) {
            // 1.8 server without offhand
        }
        if (matchesSnapshot(snapshot, off, weapon)) {
            return new int[] { SLOT_OFF_HAND, -2 };
        }
        return new int[] { SLOT_UNKNOWN, -1 };
    }

    private boolean slotStillMatches(final Player player, final ProtectionState state) {
        final PlayerInventory inventory = player.getInventory();
        if (inventory == null) {
            return false;
        }
        if (state.slotType == SLOT_MAIN_HAND || state.slotType == SLOT_HOTBAR) {
            final int idx = state.slotIndex;
            ItemStack current = null;
            final ItemStack[] contents = inventory.getContents();
            if (idx >= 0 && idx < contents.length) {
                current = contents[idx];
            } else if (state.slotType == SLOT_MAIN_HAND) {
                final int held = inventory.getHeldItemSlot();
                if (held >= 0 && held < contents.length) {
                    current = contents[held];
                }
            }
            return current != null && current.getType() != Material.AIR && matches(state, current);
        }
        if (state.slotType == SLOT_OFF_HAND) {
            try {
                final Object raw = inventory.getClass().getMethod("getItemInOffHand").invoke(inventory);
                if (raw instanceof ItemStack) {
                    final ItemStack current = (ItemStack) raw;
                    if (current.getType() != Material.AIR && matches(state, current)) {
                        return true;
                    }
                }
            } catch (final Throwable ignored) {
                // 1.8 no offhand
            }
        }
        return false;
    }

    private boolean inventoryContainsMatchingWeapon(final Player player, final ProtectionState state) {
        final PlayerInventory inventory = player.getInventory();
        if (inventory == null) {
            return false;
        }
        for (final ItemStack slot : inventory.getContents()) {
            if (matches(state, slot)) {
                return true;
            }
        }
        try {
            final Object raw = inventory.getClass().getMethod("getItemInOffHand").invoke(inventory);
            if (raw instanceof ItemStack && matches(state, (ItemStack) raw)) {
                return true;
            }
        } catch (final Throwable ignored) {
            // 1.8 no offhand
        }
        return false;
    }

    private boolean matches(final ProtectionState state, final ItemStack candidate) {
        if (state == null || candidate == null || candidate.getType() == Material.AIR) {
            return false;
        }
        if (candidate == state.snapshot || candidate.equals(state.snapshot)) {
            return true;
        }
        if (state.snapshot.isSimilar(candidate)) {
            return true;
        }
        try {
            if (this.plugin != null && this.plugin.getWeaponManager() != null) {
                final java.util.Optional<WeaponDefinition> identified = this.plugin.getWeaponManager().identify(candidate);
                if (identified.isPresent() && identified.get() != null && state.weaponId != null
                        && state.weaponId.equalsIgnoreCase(identified.get().getId())) {
                    return true;
                }
            }
        } catch (final Throwable ignored) {
            // weapon manager may not be ready; ignore
        }
        return false;
    }

    private static boolean matchesSnapshot(final ItemStack snapshot, final ItemStack candidate, final WeaponDefinition weapon) {
        if (candidate == null || candidate.getType() == Material.AIR) {
            return false;
        }
        if (candidate == snapshot || candidate.equals(snapshot)) {
            return true;
        }
        if (snapshot.isSimilar(candidate)) {
            return true;
        }
        return false;
    }

    private Player resolvePlayer(final UUID id) {
        if (this.plugin == null) {
            return null;
        }
        try {
            return this.plugin.getServer().getPlayer(id);
        } catch (final Throwable ignored) {
            return null;
        }
    }

    public static final class Protection implements AutoCloseable {
        private final AbilityItemProtectionService service;
        private final UUID playerId;
        private final long stateId;
        private final boolean active;
        private final String weaponId;
        private boolean closed;

        Protection(final AbilityItemProtectionService service, final UUID playerId, final long stateId, final boolean active, final String weaponId) {
            this.service = service;
            this.playerId = playerId;
            this.stateId = stateId;
            this.active = active;
            this.weaponId = weaponId;
        }

        private static Protection inactive() {
            return new Protection(null, null, -1L, false, null);
        }

        public boolean isActive() {
            return this.active;
        }

        public UUID getPlayerId() {
            return this.playerId;
        }

        public String getWeaponId() {
            return this.weaponId;
        }

        @Override
        public void close() {
            if (!this.active || this.service == null || this.closed) {
                return;
            }
            this.closed = true;
            try {
                this.service.finish(this.playerId, this.stateId);
            } catch (final Throwable ignored) {
                // never throw from close
            }
        }
    }

    private static final class ProtectionState {
        final UUID playerId;
        final String weaponId;
        final ItemStack snapshot;
        final WeaponDefinition weapon;
        final long createdAtMillis;
        long expiresAtMillis;
        final int slotType;
        final int slotIndex;

        ProtectionState(final UUID playerId, final String weaponId, final ItemStack snapshot, final WeaponDefinition weapon, final long createdAtMillis, final long expiresAtMillis, final int slotType, final int slotIndex) {
            this.playerId = playerId;
            this.weaponId = weaponId;
            this.snapshot = snapshot;
            this.weapon = weapon;
            this.createdAtMillis = createdAtMillis;
            this.expiresAtMillis = expiresAtMillis;
            this.slotType = slotType;
            this.slotIndex = slotIndex;
        }
    }
}
