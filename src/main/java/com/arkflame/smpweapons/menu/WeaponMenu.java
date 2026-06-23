package com.arkflame.smpweapons.menu;

import com.arkflame.smpweapons.model.WeaponDefinition;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public final class WeaponMenu implements InventoryHolder {
    private final String id;
    private final int page;
    private final Map<Integer, WeaponDefinition> weaponsBySlot = new HashMap<Integer, WeaponDefinition>();
    private Inventory inventory;
    private int previousSlot = -1;
    private int nextSlot = -1;
    private boolean hasPrevious;
    private boolean hasNext;

    public WeaponMenu(final String id) {
        this(id, 0);
    }

    public WeaponMenu(final String id, final int page) {
        this.id = id;
        this.page = Math.max(0, page);
    }

    public String getId() {
        return id;
    }

    public int getPage() {
        return page;
    }

    public void setInventory(final Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void putWeapon(final int slot, final WeaponDefinition weapon) {
        weaponsBySlot.put(Integer.valueOf(slot), weapon);
    }

    public WeaponDefinition weaponAt(final int slot) {
        return weaponsBySlot.get(Integer.valueOf(slot));
    }

    public void setNavigation(final int previousSlot, final int nextSlot, final boolean hasPrevious, final boolean hasNext) {
        this.previousSlot = previousSlot;
        this.nextSlot = nextSlot;
        this.hasPrevious = hasPrevious;
        this.hasNext = hasNext;
    }

    public boolean isPreviousSlot(final int slot) {
        return hasPrevious && slot == previousSlot;
    }

    public boolean isNextSlot(final int slot) {
        return hasNext && slot == nextSlot;
    }
}
