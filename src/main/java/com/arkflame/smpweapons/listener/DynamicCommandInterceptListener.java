package com.arkflame.smpweapons.listener;

import com.arkflame.smpweapons.SMPWeaponsPlugin;
import com.arkflame.smpweapons.menu.MenuManager;
import com.arkflame.smpweapons.model.WeaponDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

public final class DynamicCommandInterceptListener implements Listener {
    private final SMPWeaponsPlugin plugin;

    public DynamicCommandInterceptListener(final SMPWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(final PlayerCommandPreprocessEvent event) {
        final String message = event.getMessage();
        if (message == null || message.length() <= 1) {
            return;
        }
        final String label = firstToken(message.substring(1)).toLowerCase(Locale.ROOT);
        for (final WeaponDefinition weapon : plugin.getWeaponManager().all()) {
            if (!matches(label, weapon.getGetCommands())) {
                continue;
            }
            event.setCancelled(true);
            final Player player = event.getPlayer();
            if (!player.hasPermission("smpweapons.get") && !player.hasPermission("smpweapons.admin")) {
                plugin.getText().send(player, "no-permission");
                return;
            }
            final ItemStack item = plugin.getItemFactory().create(weapon, 1);
            player.getInventory().addItem(item);
            final java.util.Map<String, String> placeholders = new java.util.HashMap<String, String>();
            placeholders.put("weapon", weapon.getDisplayId());
            plugin.getText().send(player, "received", placeholders);
            return;
        }
        for (final MenuManager.MenuDefinition menu : plugin.getMenuManager().dynamicMenus()) {
            if (!matches(label, menu.getOpenCommands())) {
                continue;
            }
            event.setCancelled(true);
            final Player player = event.getPlayer();
            final String permission = menu.getCommandPermission();
            if (permission != null && !permission.isEmpty() && !player.hasPermission(permission) && !player.hasPermission("smpweapons.admin")) {
                plugin.getText().send(player, "no-permission");
                return;
            }
            if (!plugin.getMenuManager().open(player, menu.getId())) {
                plugin.getText().send(player, "unknown-menu");
            }
            return;
        }
    }

    private static boolean matches(final String label, final List<String> commands) {
        for (final String command : commands) {
            if (label.equals(firstToken(command).replace("/", "").toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String firstToken(final String input) {
        final String trimmed = input == null ? "" : input.trim();
        final int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }
}
