package com.arkflame.smpweapons.command;

import com.arkflame.smpweapons.SMPWeaponsPlugin;
import com.arkflame.smpweapons.model.WeaponDefinition;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public final class DynamicGetWeaponCommand extends Command {
    private final SMPWeaponsPlugin plugin;
    private final WeaponDefinition weapon;

    public DynamicGetWeaponCommand(final SMPWeaponsPlugin plugin, final String name, final List<String> aliases, final WeaponDefinition weapon) {
        super(name, "Get a weapon.", "/" + name, aliases == null ? Collections.<String>emptyList() : aliases);
        this.plugin = plugin;
        this.weapon = weapon;
        setPermission("smpweapons.get");
    }

    @Override
    public boolean execute(final CommandSender sender, final String commandLabel, final String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getText().send(sender, "only-player");
            return true;
        }
        if (!sender.hasPermission("smpweapons.get") && !sender.hasPermission("smpweapons.admin")) {
            plugin.getText().send(sender, "no-permission");
            return true;
        }
        final Player player = (Player) sender;
        final ItemStack item = plugin.getItemFactory().create(weapon, 1);
        player.getInventory().addItem(item);
        if (plugin.getInventoryPassiveService() != null) {
            plugin.getInventoryPassiveService().markFullScan(player);
        }
        final java.util.Map<String, String> placeholders = new java.util.HashMap<String, String>();
        placeholders.put("weapon", weapon.getDisplayId());
        plugin.getText().send(player, "received", placeholders);
        return true;
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String alias, final String[] args) {
        return Collections.emptyList();
    }
}
