package com.arkflame.smpweapons.config;

import com.arkflame.smpweapons.item.ItemIdentityService;
import com.arkflame.smpweapons.model.WeaponDefinition;
import com.arkflame.smpweapons.util.Materials;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class WeaponManager {
    private final JavaPlugin plugin;
    private final ItemIdentityService identityService;
    private final Map<String, WeaponDefinition> weapons = new LinkedHashMap<String, WeaponDefinition>();
    private final List<String> loadWarnings = new ArrayList<String>();
    private final List<String> loadErrors = new ArrayList<String>();

    public WeaponManager(final JavaPlugin plugin, final ItemIdentityService identityService) {
        this.plugin = plugin;
        this.identityService = identityService;
    }

    public void load() {
        final Map<String, WeaponDefinition> loaded = new LinkedHashMap<String, WeaponDefinition>();
        this.loadWarnings.clear();
        this.loadErrors.clear();
        final File folder = new File(this.plugin.getDataFolder(), this.plugin.getConfig().getString("settings.weapons-folder", "weapons"));
        if (!folder.exists() && !folder.mkdirs()) {
            this.loadErrors.add("Could not create weapons folder: " + folder.getPath());
        }
        final File[] files = folder.listFiles();
        if (files != null) {
            java.util.Arrays.sort(files);
            for (final File file : files) {
                if (!file.isFile() || !file.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) {
                    continue;
                }
                loadFile(file, loaded);
            }
        }
        if (!this.loadErrors.isEmpty() && !this.weapons.isEmpty()) {
            this.plugin.getLogger().warning("Weapon reload had errors. Keeping previous active weapon registry.");
        } else {
            this.weapons.clear();
            this.weapons.putAll(loaded);
        }
        for (final String warning : this.loadWarnings) {
            this.plugin.getLogger().warning(warning);
        }
        for (final String error : this.loadErrors) {
            this.plugin.getLogger().warning(error);
        }
    }

    public List<WeaponDefinition> all() {
        final List<WeaponDefinition> output = new ArrayList<WeaponDefinition>();
        for (final WeaponDefinition weapon : this.weapons.values()) {
            if (weapon != null && weapon.isEnabled()) {
                output.add(weapon);
            }
        }
        return output;
    }

    public List<WeaponDefinition> allLoaded() {
        return new ArrayList<WeaponDefinition>(this.weapons.values());
    }

    public Optional<WeaponDefinition> get(final String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.weapons.get(normalizeId(id)));
    }

    public Optional<WeaponDefinition> identify(final ItemStack item) {
        final Optional<String> storedId = this.identityService.read(item);
        if (storedId.isPresent()) {
            final Optional<WeaponDefinition> storedWeapon = get(storedId.get());
            if (storedWeapon.isPresent()) {
                return storedWeapon;
            }
        }
        for (final WeaponDefinition definition : this.weapons.values()) {
            if (definition.isEnabled() && this.identityService.matches(item, definition)) {
                return Optional.of(definition);
            }
        }
        return Optional.empty();
    }

    public List<String> ids() {
        final List<String> ids = new ArrayList<String>();
        for (final Map.Entry<String, WeaponDefinition> entry : this.weapons.entrySet()) {
            if (entry.getValue() != null && entry.getValue().isEnabled()) {
                ids.add(entry.getKey());
            }
        }
        return ids;
    }

    public List<String> getLoadWarnings() {
        return new ArrayList<String>(this.loadWarnings);
    }

    public List<String> getLoadErrors() {
        return new ArrayList<String>(this.loadErrors);
    }

    public boolean hasLoadProblems() {
        return !this.loadWarnings.isEmpty() || !this.loadErrors.isEmpty();
    }

    public boolean deleteCustom(final String id) {
        final String normalized = normalizeId(id);
        final File file = customFile();
        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.isConfigurationSection("weapons." + normalized)) {
            return false;
        }
        yaml.set("weapons." + normalized, null);
        try {
            yaml.save(file);
            return true;
        } catch (final IOException e) {
            this.plugin.getLogger().warning("Could not save custom weapons file: " + e.getMessage());
            return false;
        }
    }

    public boolean createFromItem(final String id, final ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        final String normalized = normalizeId(id);
        final File file = customFile();
        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        final String root = "weapons." + normalized;
        yaml.set(root + ".enabled", Boolean.TRUE);
        yaml.set(root + ".override", Boolean.TRUE);
        yaml.set(root + ".display-id", normalized.replace('_', ' '));
        yaml.set(root + ".item.material", item.getType().name());
        if (item.hasItemMeta()) {
            final org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            yaml.set(root + ".item.name", meta.hasDisplayName() ? meta.getDisplayName() : normalized);
            yaml.set(root + ".item.lore", meta.hasLore() ? meta.getLore() : Collections.<String>emptyList());
            final Integer data = this.identityService.getCustomModelData(meta);
            if (data != null) {
                yaml.set(root + ".item.custom-model-data", data);
                yaml.set(root + ".legacy.custom-model-data", data);
            }
            if (meta.hasDisplayName()) {
                yaml.set(root + ".legacy.names-contains", Collections.singletonList(com.arkflame.smpweapons.util.TextBridge.stripColor(meta.getDisplayName())));
            }
        } else {
            yaml.set(root + ".item.name", normalized);
            yaml.set(root + ".item.lore", Collections.<String>emptyList());
        }
        yaml.set(root + ".trigger.type", "SNEAK_RIGHT_CLICK");
        yaml.set(root + ".trigger.cooldown", Integer.valueOf(30));
        yaml.set(root + ".ability.type", "NONE");
        try {
            yaml.save(file);
            return true;
        } catch (final IOException e) {
            this.plugin.getLogger().warning("Could not save custom weapons file: " + e.getMessage());
            return false;
        }
    }

    private void loadFile(final File file, final Map<String, WeaponDefinition> loaded) {
        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        final ConfigurationSection root = yaml.getConfigurationSection("weapons");
        if (root == null) {
            return;
        }
        for (final String id : root.getKeys(false)) {
            final ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            final String normalized = normalizeId(id);
            if (normalized.isEmpty() || !normalized.matches("[a-z0-9_]+")) {
                this.loadErrors.add(file.getName() + ": invalid weapon id '" + id + "'");
                continue;
            }
            final WeaponDefinition definition = WeaponDefinition.from(normalized, section, file.getName());
            final List<String> problems = validate(definition);
            if (!problems.isEmpty()) {
                for (final String problem : problems) {
                    this.loadErrors.add(file.getName() + ": " + normalized + ": " + problem);
                }
                continue;
            }
            if (loaded.containsKey(normalized) && !definition.isOverride()) {
                this.loadWarnings.add(file.getName() + ": duplicate weapon '" + normalized + "' skipped. Add override: true to replace the earlier definition.");
                continue;
            }
            if (loaded.containsKey(normalized) && definition.isOverride()) {
                loaded.remove(normalized);
            }
            final String duplicateCommand = duplicateCommand(definition, loaded);
            if (duplicateCommand != null) {
                this.loadErrors.add(file.getName() + ": " + normalized + ": duplicate getter command '" + duplicateCommand + "' already used by another weapon");
                continue;
            }
            loaded.put(normalized, definition);
        }
    }

    private List<String> validate(final WeaponDefinition definition) {
        final List<String> problems = new ArrayList<String>();
        if (definition.getMaterialAliases().isEmpty() || !Materials.find(definition.getMaterialAliases()).isPresent()) {
            problems.add("no usable material alias in item.material");
        }
        if (definition.getCooldownSeconds() < 0) {
            problems.add("trigger.cooldown cannot be negative");
        }
        if (definition.getName() == null || definition.getName().trim().isEmpty()) {
            problems.add("item.name cannot be empty");
        }
        if (definition.getPassiveSection() != null) {
            final double chance = definition.getPassiveSection().getDouble("chance", 100.0D);
            if (chance < 0.0D || chance > 100.0D) {
                problems.add("passive.chance must be between 0 and 100");
            }
        }
        if (definition.getTriggersSection() != null) {
            for (final String key : definition.getTriggersSection().getKeys(false)) {
                final ConfigurationSection trigger = definition.getTriggersSection().getConfigurationSection(key);
                if (trigger == null) {
                    continue;
                }
                final String timeline = trigger.getString("timeline", null);
                if (timeline != null && !timelineExists(definition, timeline)) {
                    problems.add("triggers." + key + ".timeline references missing timeline '" + timeline + "'");
                }
                final String cooldown = trigger.getString("cooldown", null);
                if (cooldown != null && definition.getCooldownsSection() != null && !definition.getCooldownsSection().isConfigurationSection(cooldown)) {
                    problems.add("triggers." + key + ".cooldown references missing cooldown '" + cooldown + "'");
                }
            }
        }
        if (definition.getPassivesSection() != null) {
            for (final String key : definition.getPassivesSection().getKeys(false)) {
                final ConfigurationSection passive = definition.getPassivesSection().getConfigurationSection(key);
                if (passive == null) {
                    continue;
                }
                final double chance = passive.getDouble("chance", 100.0D);
                if (chance < 0.0D || chance > 100.0D) {
                    problems.add("passives." + key + ".chance must be between 0 and 100");
                }
                final String timeline = passive.getString("timeline", null);
                if (timeline != null && !timelineExists(definition, timeline)) {
                    problems.add("passives." + key + ".timeline references missing timeline '" + timeline + "'");
                }
            }
        }
        validateProjectiles(definition, definition.getProjectilesSection(), "projectiles", problems);
        final Set<String> commands = new HashSet<String>();
        for (final String command : definition.getGetCommands()) {
            final String label = firstToken(command).replace("/", "").toLowerCase(Locale.ROOT).trim();
            if (label.contains(":")) {
                problems.add("commands.get contains namespaced command '" + label + "'");
            }
            if (!label.isEmpty() && !commands.add(label)) {
                problems.add("duplicate getter command '" + label + "'");
            }
        }
        if (definition.getTriggerTimeline() != null && definition.getAbilitySection() != null) {
            final ConfigurationSection abilityTimelines = definition.getAbilitySection().getConfigurationSection("timelines");
            final ConfigurationSection rootTimelines = definition.getTimelinesSection();
            if ((abilityTimelines == null || abilityTimelines.getConfigurationSection(definition.getTriggerTimeline()) == null) && (rootTimelines == null || rootTimelines.getConfigurationSection(definition.getTriggerTimeline()) == null)) {
                problems.add("trigger timeline '" + definition.getTriggerTimeline() + "' does not exist under ability.timelines or timelines");
            }
        }
        if (definition.getAbilitySection() != null) {
            final ConfigurationSection timeline = definition.getAbilitySection().getConfigurationSection("timeline");
            if (timeline != null && timeline.getKeys(false).size() > this.plugin.getConfig().getInt("engine.max-timeline-entries", 512)) {
                problems.add("ability.timeline has too many entries");
            }
            final ConfigurationSection projectile = definition.getAbilitySection().getConfigurationSection("projectile");
            if (projectile != null && projectile.getInt("lifetime-ticks", 100) > 600) {
                problems.add("projectile.lifetime-ticks cannot exceed 600");
            }
            validateProjectiles(definition, definition.getAbilitySection().getConfigurationSection("projectiles"), "ability.projectiles", problems);
        }
        return problems;
    }

    private void validateProjectiles(final WeaponDefinition definition, final ConfigurationSection projectiles, final String path, final List<String> problems) {
        if (projectiles == null) {
            return;
        }
        final int maxLifetime = this.plugin.getConfig().getInt("engine.max-projectile-lifetime-ticks", 600);
        for (final String key : projectiles.getKeys(false)) {
            final ConfigurationSection projectile = projectiles.getConfigurationSection(key);
            if (projectile == null) {
                continue;
            }
            if (projectile.getInt("lifetime-ticks", 100) > maxLifetime) {
                problems.add(path + "." + key + ".lifetime-ticks cannot exceed " + maxLifetime);
            }
            validateProjectileTimeline(definition, projectile, path + "." + key + ".on_tick", "on_tick", problems);
            validateProjectileTimeline(definition, projectile, path + "." + key + ".on_hit", "on_hit", problems);
            validateProjectileTimeline(definition, projectile, path + "." + key + ".on_expire", "on_expire", problems);
        }
    }

    private void validateProjectileTimeline(final WeaponDefinition definition, final ConfigurationSection projectile, final String path, final String child, final List<String> problems) {
        final ConfigurationSection section = projectile.getConfigurationSection(child);
        if (section == null) {
            return;
        }
        final String timeline = section.getString("timeline", null);
        if (timeline != null && !timelineExists(definition, timeline)) {
            problems.add(path + ".timeline references missing timeline '" + timeline + "'");
        }
    }

    private boolean timelineExists(final WeaponDefinition definition, final String timeline) {
        if (timeline == null || timeline.trim().isEmpty()) {
            return false;
        }
        final ConfigurationSection rootTimelines = definition.getTimelinesSection();
        if (rootTimelines != null && rootTimelines.isConfigurationSection(timeline)) {
            return true;
        }
        final ConfigurationSection ability = definition.getAbilitySection();
        if (ability != null) {
            final ConfigurationSection abilityTimelines = ability.getConfigurationSection("timelines");
            return abilityTimelines != null && abilityTimelines.isConfigurationSection(timeline);
        }
        return false;
    }

    private static String duplicateCommand(final WeaponDefinition definition, final Map<String, WeaponDefinition> loaded) {
        final Set<String> seen = new HashSet<String>();
        for (final WeaponDefinition existing : loaded.values()) {
            for (final String command : existing.getGetCommands()) {
                final String label = firstToken(command).replace("/", "").toLowerCase(Locale.ROOT).trim();
                if (!label.isEmpty()) {
                    seen.add(label);
                }
            }
        }
        for (final String command : definition.getGetCommands()) {
            final String label = firstToken(command).replace("/", "").toLowerCase(Locale.ROOT).trim();
            if (!label.isEmpty() && seen.contains(label)) {
                return label;
            }
        }
        return null;
    }

    private static String firstToken(final String input) {
        if (input == null) {
            return "";
        }
        final String trimmed = input.trim();
        final int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }

    private File customFile() {
        final File file = new File(this.plugin.getDataFolder(), this.plugin.getConfig().getString("settings.custom-weapons-file", "weapons/custom-weapons.yml"));
        final File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (final IOException e) {
                this.plugin.getLogger().warning("Could not create custom weapons file: " + e.getMessage());
            }
        }
        return file;
    }

    public static String normalizeId(final String input) {
        if (input == null) {
            return "";
        }
        return input.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
