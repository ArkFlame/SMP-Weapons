package com.arkflame.smpweapons.listener;

import com.arkflame.smpweapons.SMPWeaponsPlugin;
import com.arkflame.smpweapons.menu.WeaponMenu;
import com.arkflame.smpweapons.model.WeaponDefinition;
import com.arkflame.smpweapons.util.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MenuClickListener implements Listener {
    private final SMPWeaponsPlugin plugin;
    private final Map<UUID, Long> lastClick = new HashMap<UUID, Long>();

    public MenuClickListener(final SMPWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        final InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof WeaponMenu)) {
            return;
        }
        event.setCancelled(true);
        final Player player = (Player) event.getWhoClicked();
        if (isCooling(player)) {
            return;
        }
        final WeaponMenu menu = (WeaponMenu) holder;
        if (menu.isPreviousSlot(event.getRawSlot())) {
            this.plugin.getMenuManager().open(player, menu.getId(), menu.getPage() - 1);
            Sounds.play(player, "UI_BUTTON_CLICK", 1.0F, 1.0F);
            return;
        }
        if (menu.isNextSlot(event.getRawSlot())) {
            this.plugin.getMenuManager().open(player, menu.getId(), menu.getPage() + 1);
            Sounds.play(player, "UI_BUTTON_CLICK", 1.0F, 1.0F);
            return;
        }
        final WeaponDefinition weapon = menu.weaponAt(event.getRawSlot());
        if (weapon == null) {
            return;
        }
        runClickActions(player, menu, weapon);
        Sounds.play(player, "UI_BUTTON_CLICK", 1.0F, 1.0F);
    }


    private void runClickActions(final Player player, final WeaponMenu menu, final WeaponDefinition weapon) {
        final java.util.Optional<com.arkflame.smpweapons.menu.MenuManager.MenuDefinition> definition = this.plugin.getMenuManager().menu(menu.getId());
        final java.util.List<String> actions = definition.isPresent() ? definition.get().getClickActions() : java.util.Collections.singletonList("GET");
        for (final String action : actions) {
            runClickAction(player, weapon, action);
        }
    }

    private void runClickAction(final Player player, final WeaponDefinition weapon, final String rawAction) {
        final String action = rawAction == null ? "GET" : rawAction.trim();
        final String upper = action.toUpperCase(java.util.Locale.ROOT);
        if ("GET".equals(upper) || "GIVE".equals(upper)) {
            player.getInventory().addItem(this.plugin.getItemFactory().create(weapon, 1));
            if (this.plugin.getInventoryPassiveService() != null) {
                this.plugin.getInventoryPassiveService().markFullScan(player);
            }
            final Map<String, String> placeholders = new HashMap<String, String>();
            placeholders.put("weapon", weapon.getDisplayId());
            this.plugin.getText().send(player, "received", placeholders);
            return;
        }
        if ("CLOSE".equals(upper)) {
            player.closeInventory();
            return;
        }
        if (upper.startsWith("OPEN_MENU:") || upper.startsWith("OPEN:")) {
            final String id = action.substring(action.indexOf(':') + 1).trim();
            if (!id.isEmpty()) {
                this.plugin.getMenuManager().open(player, id);
            }
            return;
        }
        if (upper.startsWith("PLAYER_COMMAND:") || upper.startsWith("PLAYER:") || upper.startsWith("COMMAND:")) {
            final String command = replace(action.substring(action.indexOf(':') + 1), player, weapon);
            if (!command.trim().isEmpty()) {
                player.performCommand(command.startsWith("/") ? command.substring(1) : command);
            }
            return;
        }
        if (upper.startsWith("CONSOLE_COMMAND:") || upper.startsWith("CONSOLE:")) {
            final String command = replace(action.substring(action.indexOf(':') + 1), player, weapon);
            if (!command.trim().isEmpty()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.startsWith("/") ? command.substring(1) : command);
            }
        }
    }

    private String replace(final String input, final Player player, final WeaponDefinition weapon) {
        return (input == null ? "" : input)
                .replace("{player}", player.getName())
                .replace("<player>", player.getName())
                .replace("{weapon}", weapon.getId())
                .replace("<weapon>", weapon.getId());
    }

    private boolean isCooling(final Player player) {
        final long cooldownMs = this.plugin.getConfig().getLong("settings.menu-click-cooldown-ms", 150L);
        final long now = System.currentTimeMillis();
        final Long previous = this.lastClick.get(player.getUniqueId());
        if (previous != null && now - previous.longValue() < cooldownMs) {
            return true;
        }
        this.lastClick.put(player.getUniqueId(), Long.valueOf(now));
        return false;
    }
}
