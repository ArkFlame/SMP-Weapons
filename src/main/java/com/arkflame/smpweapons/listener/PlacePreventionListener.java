package com.arkflame.smpweapons.listener;

import com.arkflame.smpweapons.SMPWeaponsPlugin;
import com.arkflame.smpweapons.config.WeaponManager;
import com.arkflame.smpweapons.model.WeaponDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class PlacePreventionListener implements Listener {
    private final SMPWeaponsPlugin plugin;

    public PlacePreventionListener(final SMPWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final WeaponManager weaponManager = this.plugin.getWeaponManager();
        if (weaponManager == null) {
            return;
        }
        final ItemStack item = player.getItemInHand();
        if (item == null) {
            return;
        }
        final Optional<WeaponDefinition> weapon = weaponManager.identify(item);
        if (!weapon.isPresent()) {
            return;
        }
        if (!weapon.get().isPreventPlace()) {
            return;
        }
        event.setCancelled(true);
        this.plugin.getText().send(player, "prevent-place");
    }
}
