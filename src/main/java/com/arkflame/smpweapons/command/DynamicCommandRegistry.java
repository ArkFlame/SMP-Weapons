package com.arkflame.smpweapons.command;

import com.arkflame.smpweapons.SMPWeaponsPlugin;
import com.arkflame.smpweapons.menu.MenuManager;
import com.arkflame.smpweapons.model.WeaponDefinition;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.SimplePluginManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class DynamicCommandRegistry {
    private final SMPWeaponsPlugin plugin;
    private final List<Command> registeredCommands = new ArrayList<Command>();
    private CommandMap commandMap;

    public DynamicCommandRegistry(final SMPWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        unregisterAll();
        final CommandMap map = commandMap();
        if (map == null) {
            plugin.getLogger().warning("Could not register custom commands.");
            return;
        }
        final Set<String> reserved = new HashSet<String>();
        reserved.add("smpweapons");
        reserved.add("smpweapon");
        reserved.add("weapons");
        for (final MenuManager.MenuDefinition menu : plugin.getMenuManager().dynamicMenus()) {
            final List<String> labels = commandLabels(menu.getOpenCommands());
            labels.removeAll(reserved);
            if (labels.isEmpty()) {
                continue;
            }
            final String primary = labels.get(0);
            final List<String> aliases = labels.size() > 1 ? labels.subList(1, labels.size()) : new ArrayList<String>();
            register(new DynamicWeaponCommand(plugin, primary, aliases, menu));
        }
        for (final WeaponDefinition weapon : plugin.getWeaponManager().all()) {
            final List<String> labels = commandLabels(weapon.getGetCommands());
            labels.removeAll(reserved);
            if (labels.isEmpty()) {
                continue;
            }
            final String primary = labels.get(0);
            final List<String> aliases = labels.size() > 1 ? labels.subList(1, labels.size()) : new ArrayList<String>();
            register(new DynamicGetWeaponCommand(plugin, primary, aliases, weapon));
        }
        refreshOnlineCommandTrees();
    }

    public void shutdown() {
        unregisterAll();
    }

    private void register(final Command command) {
        final CommandMap map = commandMap();
        if (map == null) {
            return;
        }
        map.register(plugin.getName().toLowerCase(Locale.ROOT), command);
        registeredCommands.add(command);
    }

    private List<String> commandLabels(final List<String> raw) {
        final List<String> output = new ArrayList<String>();
        final Set<String> seen = new HashSet<String>();
        for (final String command : raw) {
            final String label = firstToken(command).replace("/", "").toLowerCase(Locale.ROOT).trim();
            if (!label.isEmpty() && !label.contains(":") && seen.add(label)) {
                output.add(label);
            }
        }
        return output;
    }

    private static String firstToken(final String input) {
        if (input == null) {
            return "";
        }
        final String trimmed = input.trim();
        final int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }

    private CommandMap commandMap() {
        if (commandMap != null) {
            return commandMap;
        }
        try {
            if (Bukkit.getPluginManager() instanceof SimplePluginManager) {
                final Field field = SimplePluginManager.class.getDeclaredField("commandMap");
                field.setAccessible(true);
                commandMap = (CommandMap) field.get(Bukkit.getPluginManager());
            }
        } catch (final Exception e) {
            plugin.getLogger().warning("Could not access command map: " + e.getMessage());
        }
        return commandMap;
    }

    private void unregisterAll() {
        final CommandMap map = commandMap();
        if (map == null) {
            registeredCommands.clear();
            return;
        }
        final List<Command> copy = new ArrayList<Command>(registeredCommands);
        registeredCommands.clear();
        for (final Command command : copy) {
            command.unregister(map);
            removeKnownCommandEntries(command);
        }
    }

    @SuppressWarnings("unchecked")
    private void removeKnownCommandEntries(final Command command) {
        final CommandMap map = commandMap();
        if (map == null) {
            return;
        }
        Class<?> clazz = map.getClass();
        while (clazz != null) {
            try {
                final Field field = clazz.getDeclaredField("knownCommands");
                field.setAccessible(true);
                final Object raw = field.get(map);
                if (raw instanceof Map) {
                    final Map<String, Command> known = (Map<String, Command>) raw;
                    final List<String> remove = new ArrayList<String>();
                    for (final Map.Entry<String, Command> entry : known.entrySet()) {
                        if (entry.getValue() == command) {
                            remove.add(entry.getKey());
                        }
                    }
                    for (final String key : remove) {
                        known.remove(key);
                    }
                }
                return;
            } catch (final NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (final Exception ignored) {
                return;
            }
        }
    }

    private void refreshOnlineCommandTrees() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            plugin.getSchedulerBridge().runEntity(player, new Runnable() {
                @Override
                public void run() {
                    try {
                        player.getClass().getMethod("updateCommands").invoke(player);
                    } catch (final Exception ignored) {
                        // old server
                    }
                }
            });
        }
    }
}
