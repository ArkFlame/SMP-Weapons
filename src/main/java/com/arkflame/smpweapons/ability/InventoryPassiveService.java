package com.arkflame.smpweapons.ability;

import com.arkflame.smpweapons.SMPWeaponsPlugin;
import com.arkflame.smpweapons.compat.scheduler.TaskHandle;
import com.arkflame.smpweapons.model.WeaponDefinition;
import com.arkflame.smpweapons.util.PlayerItems;
import com.arkflame.smpweapons.util.PotionEffects;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class InventoryPassiveService {
    private final SMPWeaponsPlugin plugin;
    private TaskHandle task;

    private static final int FIRST_STORAGE_SLOT = 0;
    private static final int LAST_STORAGE_SLOT = 35;
    private static final int FIRST_ARMOR_SLOT = 36;
    private static final int LAST_ARMOR_SLOT = 39;
    private static final int OFF_HAND_SLOT = 40;
    private static final int MAX_LOGICAL_SLOT = OFF_HAND_SLOT;

    private final Object dirtyLock = new Object();
    private final Map<java.util.UUID, DirtyInventory> dirtyPlayers = new HashMap<java.util.UUID, DirtyInventory>();
    private final Map<java.util.UUID, PlayerInventoryState> states = new ConcurrentHashMap<java.util.UUID, PlayerInventoryState>();

    public InventoryPassiveService(final SMPWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        markAllOnlineFullScan();
        final long period = Math.max(20L, plugin.getConfig().getLong("engine.inventory-passive-period-ticks", 20L));
        this.task = plugin.getSchedulerBridge().runGlobalRepeating(new Runnable() {
            @Override
            public void run() {
                tickAll();
            }
        }, period, period);
    }

    public void stop() {
        if (this.task != null) {
            try {
                this.task.cancel();
            } catch (final Exception ignored) {
            }
            this.task = null;
        }
        this.states.clear();
        synchronized (this.dirtyLock) {
            this.dirtyPlayers.clear();
        }
    }

    public void markFullScan(final Player player) {
        if (player == null) {
            return;
        }
        final java.util.UUID pid = player.getUniqueId();
        synchronized (this.dirtyLock) {
            DirtyInventory dirty = this.dirtyPlayers.get(pid);
            if (dirty == null) {
                dirty = new DirtyInventory();
                this.dirtyPlayers.put(pid, dirty);
            }
            dirty.markFullScan();
        }
    }

    public void markSlot(final Player player, final int logicalSlot) {
        if (player == null) {
            return;
        }
        if (!isValidLogicalSlot(logicalSlot)) {
            markFullScan(player);
            return;
        }
        final java.util.UUID pid = player.getUniqueId();
        synchronized (this.dirtyLock) {
            DirtyInventory dirty = this.dirtyPlayers.get(pid);
            if (dirty == null) {
                dirty = new DirtyInventory();
                this.dirtyPlayers.put(pid, dirty);
            }
            dirty.markSlot(logicalSlot);
        }
    }

    public void markSlots(final Player player, final Collection<Integer> logicalSlots) {
        if (player == null) {
            return;
        }
        if (logicalSlots == null || logicalSlots.isEmpty()) {
            markFullScan(player);
            return;
        }
        for (final int slot : logicalSlots) {
            if (!isValidLogicalSlot(slot)) {
                markFullScan(player);
                return;
            }
        }
        final java.util.UUID pid = player.getUniqueId();
        synchronized (this.dirtyLock) {
            DirtyInventory dirty = this.dirtyPlayers.get(pid);
            if (dirty == null) {
                dirty = new DirtyInventory();
                this.dirtyPlayers.put(pid, dirty);
            }
            for (final int slot : logicalSlots) {
                dirty.markSlot(slot);
            }
        }
    }

    public void markMainHand(final Player player) {
        if (player == null || player.getInventory() == null) {
            return;
        }
        markSlot(player, player.getInventory().getHeldItemSlot());
    }

    public void markOffHand(final Player player) {
        markSlot(player, OFF_HAND_SLOT);
    }

    public void clear(final Player player) {
        if (player == null) {
            return;
        }
        final java.util.UUID pid = player.getUniqueId();
        this.states.remove(pid);
        synchronized (this.dirtyLock) {
            this.dirtyPlayers.remove(pid);
        }
    }

    private void tickAll() {
        final Set<java.util.UUID> candidates = new HashSet<java.util.UUID>();
        synchronized (this.dirtyLock) {
            candidates.addAll(this.dirtyPlayers.keySet());
        }
        candidates.addAll(this.states.keySet());
        if (candidates.isEmpty()) {
            return;
        }
        final List<Player> online = new ArrayList<Player>();
        for (final Player p : plugin.getServer().getOnlinePlayers()) {
            if (p != null && candidates.contains(p.getUniqueId())) {
                online.add(p);
            }
        }
        for (final Player player : online) {
            plugin.getSchedulerBridge().runEntityLater(player, new Runnable() {
                @Override
                public void run() {
                    tickPlayer(player);
                }
            }, null, 0L);
        }
    }

    private void tickPlayer(final Player player) {
        if (player == null || !player.isOnline()) {
            if (player != null) {
                this.states.remove(player.getUniqueId());
                synchronized (this.dirtyLock) {
                    this.dirtyPlayers.remove(player.getUniqueId());
                }
            }
            return;
        }
        final java.util.UUID pid = player.getUniqueId();
        final DirtyInventory dirty = drainDirty(pid);
        if (dirty == null && !this.states.containsKey(pid)) {
            return;
        }
        PlayerInventoryState state = this.states.get(pid);
        if (state == null) {
            state = new PlayerInventoryState();
            this.states.put(pid, state);
        }
        if (dirty != null) {
            if (dirty.isFullScan()) {
                scanFull(player, state);
            } else {
                scanSlots(player, state, dirty.slots());
                if (dirty.isEmpty()) {
                    // non-full marker; fallback scan nothing
                }
            }
        }
        if (state.isEmpty()) {
            this.states.remove(pid);
            return;
        }
        final Set<String> active = copyActiveWeaponIds(state);
        for (final String weaponId : active) {
            final Optional<WeaponDefinition> opt = plugin.getWeaponManager().get(weaponId);
            if (!opt.isPresent()) {
                continue;
            }
            final WeaponDefinition weapon = opt.get();
            if (!weapon.isEnabled()) {
                continue;
            }
            runInventoryPassive(player, weapon);
        }
    }

    private DirtyInventory drainDirty(final java.util.UUID playerId) {
        synchronized (this.dirtyLock) {
            return this.dirtyPlayers.remove(playerId);
        }
    }

    private void scanFull(final Player player, final PlayerInventoryState state) {
        state.clear();
        final Set<Integer> allSlots = new HashSet<Integer>();
        for (int i = FIRST_STORAGE_SLOT; i <= LAST_STORAGE_SLOT; i++) {
            allSlots.add(Integer.valueOf(i));
        }
        for (int i = FIRST_ARMOR_SLOT; i <= LAST_ARMOR_SLOT; i++) {
            allSlots.add(Integer.valueOf(i));
        }
        allSlots.add(Integer.valueOf(OFF_HAND_SLOT));
        scanSlots(player, state, allSlots);
    }

    private void scanSlots(final Player player, final PlayerInventoryState state, final Set<Integer> slots) {
        for (final int slot : slots) {
            final Optional<ItemStack> item = itemAt(player, slot);
            final Optional<String> wid;
            if (item.isPresent() && item.get().getType() != org.bukkit.Material.AIR) {
                wid = identifyInventoryPassiveWeaponId(item.get());
            } else {
                wid = Optional.empty();
            }
            state.updateSlot(slot, wid);
        }
    }

    private Optional<String> identifyInventoryPassiveWeaponId(final ItemStack item) {
        final Optional<WeaponDefinition> opt = plugin.getWeaponManager().identify(item);
        if (!opt.isPresent()) {
            return Optional.empty();
        }
        final WeaponDefinition weapon = opt.get();
        if (!weapon.isEnabled()) {
            return Optional.empty();
        }
        if (!hasInventoryPassive(weapon)) {
            return Optional.empty();
        }
        return Optional.of(weapon.getId());
    }

    private boolean hasInventoryPassive(final WeaponDefinition weapon) {
        final ConfigurationSection section = weapon.getPassivesSection();
        if (section == null) {
            return false;
        }
        for (final String key : section.getKeys(false)) {
            final ConfigurationSection passive = section.getConfigurationSection(key);
            if (passive == null) {
                continue;
            }
            if (matchesInventoryEvent(passive.getStringList("events"))) {
                return true;
            }
        }
        return false;
    }

    private Optional<ItemStack> itemAt(final Player player, final int logicalSlot) {
        if (logicalSlot >= FIRST_STORAGE_SLOT && logicalSlot <= LAST_STORAGE_SLOT) {
            return Optional.ofNullable(player.getInventory().getItem(logicalSlot));
        }
        if (logicalSlot >= FIRST_ARMOR_SLOT && logicalSlot <= LAST_ARMOR_SLOT) {
            final ItemStack[] armor = player.getInventory().getArmorContents();
            final int idx = logicalSlot - FIRST_ARMOR_SLOT;
            if (idx >= 0 && idx < armor.length) {
                return Optional.ofNullable(armor[idx]);
            }
            return Optional.empty();
        }
        if (logicalSlot == OFF_HAND_SLOT) {
            return Optional.ofNullable(PlayerItems.offHand(player));
        }
        return Optional.empty();
    }

    private static boolean isValidLogicalSlot(final int logicalSlot) {
        return logicalSlot >= 0 && logicalSlot <= MAX_LOGICAL_SLOT;
    }

    private void markAllOnlineFullScan() {
        for (final Player player : plugin.getServer().getOnlinePlayers()) {
            markFullScan(player);
        }
    }

    private static Set<String> copyActiveWeaponIds(final PlayerInventoryState state) {
        return new LinkedHashSet<String>(state.slotWeapons.values());
    }

    private void runInventoryPassive(final Player player, final WeaponDefinition weapon) {
        final ConfigurationSection passives = weapon.getPassivesSection();
        if (passives == null) {
            return;
        }
        for (final String key : passives.getKeys(false)) {
            final ConfigurationSection passive = passives.getConfigurationSection(key);
            if (passive == null) {
                continue;
            }
            if (!matchesInventoryEvent(passive.getStringList("events"))) {
                continue;
            }
            final List<String> effects = passive.getStringList("effects");
            if (effects != null) {
                for (final String effect : effects) {
                    if (effect != null && !effect.trim().isEmpty()) {
                        PotionEffects.apply(player, effect.trim());
                    }
                }
            }
            final String timeline = passive.getString("timeline", null);
            if (timeline != null && !timeline.trim().isEmpty()) {
                plugin.getAbilityEngine().executeNamedTimeline(player, weapon, timeline.trim(), null, player, null);
            }
        }
    }

    private boolean matchesInventoryEvent(final List<String> events) {
        if (events == null || events.isEmpty()) {
            return false;
        }
        for (final String raw : events) {
            final String normalized = normalize(raw);
            if ("INVENTORY_TICK".equals(normalized) || "INVENTORY".equals(normalized) || "HELD_OR_INVENTORY".equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(final String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static final class DirtyInventory {
        private boolean fullScan;
        private final Set<Integer> slots = new HashSet<Integer>();

        private void markFullScan() {
            this.fullScan = true;
            this.slots.clear();
        }

        private void markSlot(final int slot) {
            if (!this.fullScan) {
                this.slots.add(Integer.valueOf(slot));
            }
        }

        private boolean isFullScan() {
            return this.fullScan;
        }

        private Set<Integer> slots() {
            return this.slots;
        }

        private boolean isEmpty() {
            return !this.fullScan && this.slots.isEmpty();
        }
    }

    private static final class PlayerInventoryState {
        private final Map<Integer, String> slotWeapons = new HashMap<Integer, String>();

        private void clear() {
            this.slotWeapons.clear();
        }

        private void updateSlot(final int slot, final Optional<String> weaponId) {
            if (weaponId.isPresent()) {
                this.slotWeapons.put(Integer.valueOf(slot), weaponId.get());
            } else {
                this.slotWeapons.remove(Integer.valueOf(slot));
            }
        }

        private boolean isEmpty() {
            return this.slotWeapons.isEmpty();
        }
    }
}