package com.arkflame.smpweapons.menu;

import com.arkflame.smpweapons.config.WeaponManager;
import com.arkflame.smpweapons.item.WeaponItemFactory;
import com.arkflame.smpweapons.model.WeaponDefinition;
import com.arkflame.smpweapons.util.Materials;
import com.arkflame.smpweapons.util.TextBridge;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MenuManager {
    private final TextBridge text;
    private final WeaponManager weaponManager;
    private final WeaponItemFactory itemFactory;
    private final Map<String, MenuDefinition> menus = new LinkedHashMap<String, MenuDefinition>();

    public MenuManager(final TextBridge text, final WeaponManager weaponManager, final WeaponItemFactory itemFactory) {
        this.text = text;
        this.weaponManager = weaponManager;
        this.itemFactory = itemFactory;
    }

    public void load(final FileConfiguration config) {
        this.menus.clear();
        final ConfigurationSection root = config.getConfigurationSection("menus");
        if (root == null) {
            return;
        }
        for (final String id : root.getKeys(false)) {
            final ConfigurationSection section = root.getConfigurationSection(id);
            if (section != null && section.getBoolean("enabled", true)) {
                this.menus.put(id.toLowerCase(java.util.Locale.ROOT), MenuDefinition.from(id, section));
            }
        }
    }

    public boolean open(final Player player, final String id) {
        return open(player, id, 0);
    }

    public boolean open(final Player player, final String id, final int page) {
        final MenuDefinition definition = this.menus.get(id.toLowerCase(java.util.Locale.ROOT));
        if (definition == null || player == null) {
            return false;
        }
        if (!definition.permission.isEmpty() && !player.hasPermission(definition.permission)) {
            return false;
        }
        final int safePage = Math.max(0, page);
        final WeaponMenu holder = new WeaponMenu(definition.id, safePage);
        final Inventory inventory = Bukkit.createInventory(holder, definition.size, this.text.legacy(definition.title));
        holder.setInventory(inventory);
        if (definition.fillEnabled) {
            final Material fillMaterial = Materials.fallback(Material.GLASS, definition.fillMaterials);
            final ItemStack fill = new ItemStack(fillMaterial, 1);
            final ItemMeta meta = fill.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(this.text.legacy(definition.fillName));
                fill.setItemMeta(meta);
            }
            for (int slot = 0; slot < definition.size; slot++) {
                inventory.setItem(slot, fill);
            }
        }
        final List<WeaponDefinition> weapons = weapons(definition);
        final int slotsPerPage = Math.max(1, definition.weaponSlots.size());
        final int start = definition.pagination ? safePage * slotsPerPage : 0;
        int index = 0;
        for (int weaponIndex = start; weaponIndex < weapons.size(); weaponIndex++) {
            if (index >= definition.weaponSlots.size()) {
                break;
            }
            final WeaponDefinition weapon = weapons.get(weaponIndex);
            final int slot = definition.weaponSlots.get(index).intValue();
            if (slot >= 0 && slot < definition.size) {
                inventory.setItem(slot, this.itemFactory.create(weapon, 1));
                holder.putWeapon(slot, weapon);
            }
            index++;
        }
        final boolean hasPrevious = definition.pagination && safePage > 0;
        final boolean hasNext = definition.pagination && start + slotsPerPage < weapons.size();
        holder.setNavigation(definition.previousSlot, definition.nextSlot, hasPrevious, hasNext);
        if (hasPrevious) {
            inventory.setItem(definition.previousSlot, navigationItem(definition.previousName));
        }
        if (hasNext) {
            inventory.setItem(definition.nextSlot, navigationItem(definition.nextName));
        }
        player.openInventory(inventory);
        return true;
    }

    private ItemStack navigationItem(final String name) {
        final Material material = Materials.fallback(Material.ARROW, java.util.Arrays.asList("ARROW", "PAPER"));
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(this.text.legacy(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    public List<String> menuIds() {
        return new ArrayList<String>(this.menus.keySet());
    }

    public Optional<MenuDefinition> menu(final String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.menus.get(id.toLowerCase(java.util.Locale.ROOT)));
    }

    public List<MenuDefinition> dynamicMenus() {
        final List<MenuDefinition> output = new ArrayList<MenuDefinition>();
        for (final MenuDefinition definition : this.menus.values()) {
            if (definition.registerCommand && !definition.openCommands.isEmpty()) {
                output.add(definition);
            }
        }
        return output;
    }

    private List<WeaponDefinition> weapons(final MenuDefinition definition) {
        if (definition.allWeapons) {
            return this.weaponManager.all();
        }
        final List<WeaponDefinition> output = new ArrayList<WeaponDefinition>();
        for (final String id : definition.weaponIds) {
            final Optional<WeaponDefinition> weapon = this.weaponManager.get(id);
            if (weapon.isPresent() && weapon.get().isEnabled()) {
                output.add(weapon.get());
            }
        }
        return output;
    }

    public static final class MenuDefinition {
        private final String id;
        private final String title;
        private final int size;
        private final String permission;
        private final String commandPermission;
        private final boolean registerCommand;
        private final List<String> openCommands;
        private final boolean fillEnabled;
        private final List<String> fillMaterials;
        private final String fillName;
        private final boolean allWeapons;
        private final List<String> weaponIds;
        private final List<Integer> weaponSlots;
        private final boolean pagination;
        private final int previousSlot;
        private final int nextSlot;
        private final String previousName;
        private final String nextName;
        private final List<String> clickActions;

        private MenuDefinition(final String id, final String title, final int size, final String permission, final String commandPermission, final boolean registerCommand, final List<String> openCommands, final boolean fillEnabled, final List<String> fillMaterials, final String fillName, final boolean allWeapons, final List<String> weaponIds, final List<Integer> weaponSlots, final boolean pagination, final int previousSlot, final int nextSlot, final String previousName, final String nextName, final List<String> clickActions) {
            this.id = id;
            this.title = title;
            this.size = size;
            this.permission = permission;
            this.commandPermission = commandPermission;
            this.registerCommand = registerCommand;
            this.openCommands = openCommands;
            this.fillEnabled = fillEnabled;
            this.fillMaterials = fillMaterials;
            this.fillName = fillName;
            this.allWeapons = allWeapons;
            this.weaponIds = weaponIds;
            this.weaponSlots = weaponSlots;
            this.pagination = pagination;
            this.previousSlot = previousSlot;
            this.nextSlot = nextSlot;
            this.previousName = previousName;
            this.nextName = nextName;
            this.clickActions = clickActions == null || clickActions.isEmpty() ? Collections.singletonList("GET") : new ArrayList<String>(clickActions);
        }

        public static MenuDefinition from(final String id, final ConfigurationSection section) {
            final ConfigurationSection fill = section.getConfigurationSection("fill");
            final Object weaponsRaw = section.get("weapons");
            final boolean allWeapons = weaponsRaw instanceof String && "all".equalsIgnoreCase(String.valueOf(weaponsRaw));
            final ConfigurationSection pagination = section.getConfigurationSection("pagination");
            return new MenuDefinition(
                    id,
                    section.getString("title", "&8SMP Weapons"),
                    normalizeSize(section.getInt("size", 54)),
                    section.getString("permission", ""),
                    section.getString("command-permission", section.getString("permission", "")),
                    section.getBoolean("register-command", false),
                    section.getStringList("open-commands"),
                    fill == null || fill.getBoolean("enabled", true),
                    fill == null ? Collections.singletonList("STAINED_GLASS_PANE") : Materials.list(fill.get("material")),
                    fill == null ? "&f" : fill.getString("name", "&f"),
                    allWeapons,
                    allWeapons ? Collections.<String>emptyList() : section.getStringList("weapons"),
                    integerList(section.getIntegerList("weapon-slots")),
                    pagination == null || pagination.getBoolean("enabled", true),
                    pagination == null ? 45 : pagination.getInt("previous-slot", 45),
                    pagination == null ? 53 : pagination.getInt("next-slot", 53),
                    pagination == null ? "&ePrevious" : pagination.getString("previous-name", "&ePrevious"),
                    pagination == null ? "&eNext" : pagination.getString("next-name", "&eNext"),
                    section.isList("click-actions") ? section.getStringList("click-actions") : Collections.singletonList(section.getString("click-action", "GET"))
            );
        }

        public String getId() { return id; }
        public String getCommandPermission() { return commandPermission; }
        public List<String> getOpenCommands() { return openCommands; }
        public List<String> getClickActions() { return Collections.unmodifiableList(clickActions); }

        private static int normalizeSize(final int size) {
            final int rows = Math.max(1, Math.min(6, (size + 8) / 9));
            return rows * 9;
        }

        private static List<Integer> integerList(final List<Integer> values) {
            return values == null ? Collections.<Integer>emptyList() : new ArrayList<Integer>(values);
        }
    }
}
