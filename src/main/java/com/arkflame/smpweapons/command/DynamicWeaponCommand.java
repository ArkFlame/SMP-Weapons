package com.arkflame.smpweapons.command;

import com.arkflame.smpweapons.SMPWeaponsPlugin;
import com.arkflame.smpweapons.menu.MenuManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class DynamicWeaponCommand extends Command {
    private final SMPWeaponsPlugin plugin;
    private final MenuManager.MenuDefinition menu;

    public DynamicWeaponCommand(final SMPWeaponsPlugin plugin, final String name, final List<String> aliases, final MenuManager.MenuDefinition menu) {
        super(name, "Open a weapon menu.", "/" + name, aliases == null ? Collections.<String>emptyList() : aliases);
        this.plugin = plugin;
        this.menu = menu;
        setPermission(menu.getCommandPermission());
    }

    @Override
    public boolean execute(final CommandSender sender, final String commandLabel, final String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getText().send(sender, "only-player");
            return true;
        }
        if (getPermission() != null && !getPermission().isEmpty() && !sender.hasPermission(getPermission()) && !sender.hasPermission("smpweapons.admin")) {
            plugin.getText().send(sender, "no-permission");
            return true;
        }
        if (!plugin.getMenuManager().open((Player) sender, menu.getId())) {
            plugin.getText().send(sender, "unknown-menu");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String alias, final String[] args) {
        return Collections.emptyList();
    }
}
