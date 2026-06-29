package com.arkflame.smpweapons.command;

import com.arkflame.smpweapons.SMPWeaponsPlugin;
import com.arkflame.smpweapons.config.WeaponManager;
import com.arkflame.smpweapons.item.WeaponItemFactory;
import com.arkflame.smpweapons.model.WeaponDefinition;
import com.arkflame.smpweapons.util.Sounds;
import com.arkflame.smpweapons.util.TextBridge;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class SMPWeaponsCommand implements CommandExecutor, TabCompleter {
    private final SMPWeaponsPlugin plugin;

    public SMPWeaponsCommand(final SMPWeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            help(sender);
            return true;
        }
        final String sub = args[0].toLowerCase(Locale.ROOT);
        if ("reload".equals(sub)) {
            return reload(sender);
        }
        if ("list".equals(sub)) {
            return list(sender);
        }
        if ("menu".equals(sub)) {
            return menu(sender, args);
        }
        if ("get".equals(sub)) {
            return get(sender, args);
        }
        if ("give".equals(sub)) {
            return give(sender, args);
        }
        if ("cooldown".equals(sub) || "cooldowns".equals(sub)) {
            return cooldown(sender, args);
        }
        if ("debug".equals(sub)) {
            return debug(sender, args);
        }
        if ("create".equals(sub)) {
            return create(sender, args);
        }
        if ("delete".equals(sub) || "remove".equals(sub)) {
            return delete(sender, args);
        }
        text().send(sender, "unknown-command");
        return true;
    }

    private boolean reload(final CommandSender sender) {
        if (!has(sender, "smpweapons.reload")) {
            return true;
        }
        plugin.reloadPlugin();
        text().send(sender, "reload");
        if (sender instanceof Player) {
            Sounds.play((Player) sender, "ENTITY_PLAYER_LEVELUP", 1.0F, 1.0F);
        }
        return true;
    }

    private boolean list(final CommandSender sender) {
        if (!has(sender, "smpweapons.list")) {
            return true;
        }
        text().send(sender, "list-header");
        for (final WeaponDefinition weapon : weapons().all()) {
            if (!weapon.isEnabled()) {
                continue;
            }
            final Map<String, String> placeholders = new HashMap<String, String>();
            placeholders.put("id", weapon.getId());
            placeholders.put("weapon", weapon.getDisplayId());
            text().send(sender, "list-entry", placeholders);
        }
        return true;
    }

    private boolean menu(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player)) {
            text().send(sender, "only-player");
            return true;
        }
        if (!has(sender, "smpweapons.menu")) {
            return true;
        }
        final String id = args.length >= 2 ? args[1] : plugin.getConfig().getString("settings.default-menu", "admin");
        if (!plugin.getMenuManager().open((Player) sender, id)) {
            text().send(sender, "unknown-menu");
        }
        return true;
    }

    private boolean get(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player)) {
            text().send(sender, "only-player");
            return true;
        }
        if (!has(sender, "smpweapons.get")) {
            return true;
        }
        if (args.length < 2) {
            text().send(sender, "unknown-weapon");
            return true;
        }
        final Optional<WeaponDefinition> weapon = weapons().get(args[1]);
        if (!weapon.isPresent() || !weapon.get().isEnabled()) {
            text().send(sender, "unknown-weapon");
            return true;
        }
        final int amount = args.length >= 3 ? parseAmount(args[2]) : 1;
        if (amount <= 0) {
            text().send(sender, "invalid-amount");
            return true;
        }
        final Player player = (Player) sender;
        player.getInventory().addItem(items().create(weapon.get(), amount));
        if (plugin.getInventoryPassiveService() != null) {
            plugin.getInventoryPassiveService().markFullScan(player);
        }
        final Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("weapon", weapon.get().getDisplayId());
        text().send(sender, "received", placeholders);
        return true;
    }

    private boolean give(final CommandSender sender, final String[] args) {
        if (!has(sender, "smpweapons.give")) {
            return true;
        }
        if (args.length < 3) {
            text().send(sender, "unknown-command");
            return true;
        }
        final Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            text().send(sender, "player-not-found");
            return true;
        }
        final Optional<WeaponDefinition> weapon = weapons().get(args[2]);
        if (!weapon.isPresent() || !weapon.get().isEnabled()) {
            text().send(sender, "unknown-weapon");
            return true;
        }
        final int amount = args.length >= 4 ? parseAmount(args[3]) : 1;
        if (amount <= 0) {
            text().send(sender, "invalid-amount");
            return true;
        }
        target.getInventory().addItem(items().create(weapon.get(), amount));
        if (plugin.getInventoryPassiveService() != null) {
            plugin.getInventoryPassiveService().markFullScan(target);
        }
        final Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("weapon", weapon.get().getDisplayId());
        placeholders.put("player", target.getName());
        text().send(sender, "given", placeholders);
        return true;
    }

    private boolean cooldown(final CommandSender sender, final String[] args) {
        if (!has(sender, "smpweapons.cooldown")) {
            return true;
        }
        final Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            text().send(sender, "player-not-found");
            return true;
        }
        if (target == null) {
            text().send(sender, "player-not-found");
            return true;
        }
        final Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("player", target.getName());
        if (args.length >= 3 && !"all".equalsIgnoreCase(args[2])) {
            final Optional<WeaponDefinition> weapon = weapons().get(args[2]);
            if (!weapon.isPresent() || !weapon.get().isEnabled()) {
                text().send(sender, "unknown-weapon");
                return true;
            }
            plugin.getCooldownService().reset(target, weapon.get());
            placeholders.put("weapon", weapon.get().getDisplayId());
            text().send(sender, "cooldown-reset-one", placeholders);
            return true;
        }
        plugin.getCooldownService().reset(target);
        text().send(sender, "cooldowns-reset", placeholders);
        return true;
    }

    private boolean create(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player)) {
            text().send(sender, "only-player");
            return true;
        }
        if (!has(sender, "smpweapons.create")) {
            return true;
        }
        if (args.length < 2) {
            text().send(sender, "unknown-command");
            return true;
        }
        final Player player = (Player) sender;
        if (!weapons().createFromItem(args[1], player.getItemInHand())) {
            text().send(sender, "empty-hand");
            return true;
        }
        plugin.reloadPlugin();
        final Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("weapon", WeaponManager.normalizeId(args[1]));
        text().send(sender, "created", placeholders);
        return true;
    }

    private boolean delete(final CommandSender sender, final String[] args) {
        if (!has(sender, "smpweapons.delete")) {
            return true;
        }
        if (args.length < 2) {
            text().send(sender, "unknown-command");
            return true;
        }
        if (!weapons().deleteCustom(args[1])) {
            text().send(sender, "cannot-delete-default");
            return true;
        }
        plugin.reloadPlugin();
        final Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("weapon", WeaponManager.normalizeId(args[1]));
        text().send(sender, "deleted", placeholders);
        return true;
    }

    private boolean debug(final CommandSender sender, final String[] args) {
        if (!has(sender, "smpweapons.debug")) {
            return true;
        }
        if (args.length < 2 || "validate".equalsIgnoreCase(args[1])) {
            sender.sendMessage("§6SMP Weapons validation");
            sender.sendMessage("§7Weapons enabled: §f" + weapons().all().size());
            sender.sendMessage("§7Weapons loaded: §f" + weapons().allLoaded().size());
            if (weapons().getLoadErrors().isEmpty() && weapons().getLoadWarnings().isEmpty()) {
                sender.sendMessage("§aNo config problems detected.");
                return true;
            }
            for (final String error : weapons().getLoadErrors()) {
                sender.sendMessage("§cERROR §7" + error);
            }
            for (final String warning : weapons().getLoadWarnings()) {
                sender.sendMessage("§eWARN §7" + warning);
            }
            return true;
        }
        if ("blocks".equalsIgnoreCase(args[1])) {
            sender.sendMessage("§6SMP Weapons blocks: §f" + plugin.getTemporaryBlockService().activeBlockCount());
            return true;
        }
        if ("restoreblocks".equalsIgnoreCase(args[1])) {
            plugin.getTemporaryBlockService().restoreAll();
            sender.sendMessage("§aRestored active weapon blocks.");
            return true;
        }
        if ("item".equalsIgnoreCase(args[1])) {
            if (!(sender instanceof Player)) {
                text().send(sender, "only-player");
                return true;
            }
            final Player player = (Player) sender;
            final Optional<WeaponDefinition> weapon = weapons().identify(player.getItemInHand());
            sender.sendMessage("§6Held item weapon: §f" + (weapon.isPresent() ? weapon.get().getId() : "none"));
            return true;
        }
        text().send(sender, "unknown-command");
        return true;
    }

    private void help(final CommandSender sender) {
        for (final String line : text().messageList("usage")) {
            sender.sendMessage(line);
        }
    }

    private boolean has(final CommandSender sender, final String permission) {
        if (sender.hasPermission(permission) || sender.hasPermission("smpweapons.admin")) {
            return true;
        }
        text().send(sender, "no-permission");
        return false;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 1) {
            return starts(args[0], java.util.Arrays.asList("help", "menu", "list", "get", "give", "create", "delete", "cooldown", "debug", "reload"));
        }
        if (args.length == 2 && ("get".equalsIgnoreCase(args[0]) || "delete".equalsIgnoreCase(args[0]))) {
            return starts(args[1], weapons().ids());
        }
        if (args.length == 2 && "menu".equalsIgnoreCase(args[0])) {
            return starts(args[1], plugin.getMenuManager().menuIds());
        }
        if (args.length == 2 && ("give".equalsIgnoreCase(args[0]) || "cooldown".equalsIgnoreCase(args[0]))) {
            final List<String> names = new ArrayList<String>();
            for (final Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return starts(args[1], names);
        }
        if (args.length == 3 && "give".equalsIgnoreCase(args[0])) {
            return starts(args[2], weapons().ids());
        }
        if (args.length == 3 && "cooldown".equalsIgnoreCase(args[0])) {
            final List<String> ids = weapons().ids();
            ids.add(0, "all");
            return starts(args[2], ids);
        }
        if (args.length == 2 && "debug".equalsIgnoreCase(args[0])) {
            return starts(args[1], java.util.Arrays.asList("validate", "item", "blocks", "restoreblocks"));
        }
        return Collections.emptyList();
    }

    private static List<String> starts(final String prefix, final List<String> values) {
        final String normalized = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        final List<String> output = new ArrayList<String>();
        for (final String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                output.add(value);
            }
        }
        return output;
    }

    private static int parseAmount(final String raw) {
        try {
            return Math.max(1, Math.min(64, Integer.parseInt(raw)));
        } catch (final Exception ignored) {
            return -1;
        }
    }

    private TextBridge text() { return plugin.getText(); }
    private WeaponManager weapons() { return plugin.getWeaponManager(); }
    private WeaponItemFactory items() { return plugin.getItemFactory(); }
}
