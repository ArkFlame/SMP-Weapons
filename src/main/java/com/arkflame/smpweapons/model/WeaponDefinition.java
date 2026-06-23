package com.arkflame.smpweapons.model;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class WeaponDefinition {
    private final String id;
    private final boolean enabled;
    private final boolean override;
    private final String displayId;
    private final List<String> materialAliases;
    private final String name;
    private final List<String> lore;
    private final Integer customModelData;
    private final List<String> enchants;
    private final List<String> itemFlags;
    private final boolean unbreakable;
    private final List<String> legacyNamesContains;
    private final List<String> legacyLoreContains;
    private final Integer legacyCustomModelData;
    private final List<String> getCommands;
    private final String triggerType;
    private final List<String> triggerEvents;
    private final List<String> triggerConditions;
    private final String triggerTimeline;
    private final String triggerCooldownKey;
    private final int cooldownSeconds;
    private final ConfigurationSection triggersSection;
    private final ConfigurationSection cooldownsSection;
    private final String abilityType;
    private final ConfigurationSection abilitySection;
    private final ConfigurationSection timelinesSection;
    private final ConfigurationSection projectilesSection;
    private final String passiveType;
    private final ConfigurationSection passiveSection;
    private final ConfigurationSection passivesSection;
    private final String sourceFile;

    private WeaponDefinition(
            final String id,
            final boolean enabled,
            final boolean override,
            final String displayId,
            final List<String> materialAliases,
            final String name,
            final List<String> lore,
            final Integer customModelData,
            final List<String> enchants,
            final List<String> itemFlags,
            final boolean unbreakable,
            final List<String> legacyNamesContains,
            final List<String> legacyLoreContains,
            final Integer legacyCustomModelData,
            final List<String> getCommands,
            final String triggerType,
            final List<String> triggerEvents,
            final List<String> triggerConditions,
            final String triggerTimeline,
            final String triggerCooldownKey,
            final int cooldownSeconds,
            final ConfigurationSection triggersSection,
            final ConfigurationSection cooldownsSection,
            final String abilityType,
            final ConfigurationSection abilitySection,
            final ConfigurationSection timelinesSection,
            final ConfigurationSection projectilesSection,
            final String passiveType,
            final ConfigurationSection passiveSection,
            final ConfigurationSection passivesSection,
            final String sourceFile
    ) {
        this.id = id;
        this.enabled = enabled;
        this.override = override;
        this.displayId = displayId;
        this.materialAliases = new ArrayList<String>(materialAliases);
        this.name = name;
        this.lore = new ArrayList<String>(lore);
        this.customModelData = customModelData;
        this.enchants = new ArrayList<String>(enchants);
        this.itemFlags = new ArrayList<String>(itemFlags);
        this.unbreakable = unbreakable;
        this.legacyNamesContains = new ArrayList<String>(legacyNamesContains);
        this.legacyLoreContains = new ArrayList<String>(legacyLoreContains);
        this.legacyCustomModelData = legacyCustomModelData;
        this.getCommands = new ArrayList<String>(getCommands);
        this.triggerType = triggerType;
        this.triggerEvents = new ArrayList<String>(triggerEvents);
        this.triggerConditions = new ArrayList<String>(triggerConditions);
        this.triggerTimeline = triggerTimeline;
        this.triggerCooldownKey = triggerCooldownKey;
        this.cooldownSeconds = cooldownSeconds;
        this.triggersSection = triggersSection;
        this.cooldownsSection = cooldownsSection;
        this.abilityType = abilityType;
        this.abilitySection = abilitySection;
        this.timelinesSection = timelinesSection;
        this.projectilesSection = projectilesSection;
        this.passiveType = passiveType;
        this.passiveSection = passiveSection;
        this.passivesSection = passivesSection;
        this.sourceFile = sourceFile;
    }

    public static WeaponDefinition from(final String id, final ConfigurationSection section, final String sourceFile) {
        final ConfigurationSection item = section.getConfigurationSection("item");
        final ConfigurationSection legacy = section.getConfigurationSection("legacy");
        final ConfigurationSection trigger = section.getConfigurationSection("trigger");
        final ConfigurationSection triggers = section.getConfigurationSection("triggers");
        final ConfigurationSection commands = section.getConfigurationSection("commands");
        final ConfigurationSection ability = section.getConfigurationSection("ability");
        final ConfigurationSection passive = section.getConfigurationSection("passive");
        final TriggerData triggerData = TriggerData.from(trigger, triggers, section.getConfigurationSection("cooldowns"));
        final List<String> materials = item == null ? Collections.singletonList("DIAMOND_SWORD") : MaterialsList.from(item.get("material"));
        final Integer model = integerOrNull(item == null ? null : item.get("custom-model-data"));
        final Integer legacyModel = integerOrNull(legacy == null ? null : legacy.get("custom-model-data"));
        return new WeaponDefinition(
                id,
                section.getBoolean("enabled", true),
                section.getBoolean("override", false),
                section.getString("display-id", id.replace('_', ' ')),
                materials,
                item == null ? id : item.getString("name", id),
                item == null ? Collections.<String>emptyList() : item.getStringList("lore"),
                model,
                item == null ? Collections.<String>emptyList() : item.getStringList("enchants"),
                item == null || !item.isList("item-flags") ? defaultItemFlags() : item.getStringList("item-flags"),
                item == null || item.getBoolean("unbreakable", true),
                legacy == null ? Collections.<String>emptyList() : legacy.getStringList("names-contains"),
                legacy == null ? Collections.<String>emptyList() : legacy.getStringList("lore-contains"),
                legacyModel,
                commands == null ? Collections.<String>emptyList() : commands.getStringList("get"),
                triggerData.type,
                triggerData.events,
                triggerData.conditions,
                triggerData.timeline,
                triggerData.cooldownKey,
                triggerData.cooldownSeconds,
                triggers,
                section.getConfigurationSection("cooldowns"),
                ability == null && triggerData.timeline != null ? "TIMELINE" : (ability == null ? "NONE" : ability.getString("type", "NONE")),
                ability,
                section.getConfigurationSection("timelines"),
                section.getConfigurationSection("projectiles"),
                passive == null ? "NONE" : passive.getString("type", "NONE"),
                passive,
                section.getConfigurationSection("passives"),
                sourceFile
        );
    }

    public String getId() { return id; }
    public boolean isEnabled() { return enabled; }
    public boolean isOverride() { return override; }
    public String getDisplayId() { return displayId; }
    public List<String> getMaterialAliases() { return Collections.unmodifiableList(materialAliases); }
    public String getName() { return name; }
    public List<String> getLore() { return Collections.unmodifiableList(lore); }
    public Integer getCustomModelData() { return customModelData; }
    public List<String> getEnchants() { return Collections.unmodifiableList(enchants); }
    public List<String> getItemFlags() { return Collections.unmodifiableList(itemFlags); }
    public boolean isUnbreakable() { return unbreakable; }
    public List<String> getLegacyNamesContains() { return Collections.unmodifiableList(legacyNamesContains); }
    public List<String> getLegacyLoreContains() { return Collections.unmodifiableList(legacyLoreContains); }
    public Integer getLegacyCustomModelData() { return legacyCustomModelData; }
    public List<String> getGetCommands() { return Collections.unmodifiableList(getCommands); }
    public String getTriggerType() { return triggerType; }
    public List<String> getTriggerEvents() { return Collections.unmodifiableList(triggerEvents); }
    public List<String> getTriggerConditions() { return Collections.unmodifiableList(triggerConditions); }
    public String getTriggerTimeline() { return triggerTimeline; }
    public String getTriggerCooldownKey() { return triggerCooldownKey; }
    public int getCooldownSeconds() { return cooldownSeconds; }
    public ConfigurationSection getTriggersSection() { return triggersSection; }
    public ConfigurationSection getCooldownsSection() { return cooldownsSection; }
    public String getAbilityType() { return abilityType; }
    public ConfigurationSection getAbilitySection() { return abilitySection; }
    public ConfigurationSection getTimelinesSection() { return timelinesSection; }
    public ConfigurationSection getProjectilesSection() { return projectilesSection; }
    public String getPassiveType() { return passiveType; }
    public ConfigurationSection getPassiveSection() { return passiveSection; }
    public ConfigurationSection getPassivesSection() { return passivesSection; }
    public String getSourceFile() { return sourceFile; }

    private static List<String> defaultItemFlags() {
        final List<String> flags = new ArrayList<String>();
        flags.add("HIDE_ATTRIBUTES");
        flags.add("HIDE_ENCHANTS");
        flags.add("HIDE_UNBREAKABLE");
        return flags;
    }

    private static Integer integerOrNull(final Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return Integer.valueOf(((Number) raw).intValue());
        }
        try {
            return Integer.valueOf(String.valueOf(raw));
        } catch (final Exception ignored) {
            return null;
        }
    }


    private static final class TriggerData {
        private final String type;
        private final List<String> events;
        private final List<String> conditions;
        private final String timeline;
        private final String cooldownKey;
        private final int cooldownSeconds;

        private TriggerData(final String type, final List<String> events, final List<String> conditions, final String timeline, final String cooldownKey, final int cooldownSeconds) {
            this.type = type;
            this.events = events;
            this.conditions = conditions;
            this.timeline = timeline;
            this.cooldownKey = cooldownKey;
            this.cooldownSeconds = cooldownSeconds;
        }

        private static TriggerData from(final ConfigurationSection legacyTrigger, final ConfigurationSection triggers, final ConfigurationSection cooldowns) {
            if (triggers != null && !triggers.getKeys(false).isEmpty()) {
                final String key = triggers.getKeys(false).iterator().next();
                final ConfigurationSection trigger = triggers.getConfigurationSection(key);
                if (trigger != null) {
                    final List<String> events = stringList(trigger.get("events"));
                    final List<String> conditions = stringList(trigger.get("conditions"));
                    final String cooldownKey = trigger.getString("cooldown", "primary");
                    final int seconds = cooldownSeconds(cooldowns, cooldownKey, trigger.getInt("cooldown-seconds", 30));
                    final String timeline = trigger.getString("timeline", null);
                    return new TriggerData(typeFrom(events, conditions), events, conditions, timeline, cooldownKey, seconds);
                }
            }
            final String type = legacyTrigger == null ? "SNEAK_RIGHT_CLICK" : legacyTrigger.getString("type", "SNEAK_RIGHT_CLICK");
            return new TriggerData(type, eventsFromType(type), conditionsFromType(type), null, "primary", legacyTrigger == null ? 30 : legacyTrigger.getInt("cooldown", 30));
        }

        private static int cooldownSeconds(final ConfigurationSection cooldowns, final String key, final int fallback) {
            if (cooldowns == null || key == null) {
                return fallback;
            }
            final ConfigurationSection section = cooldowns.getConfigurationSection(key);
            return section == null ? fallback : section.getInt("seconds", fallback);
        }

        private static String typeFrom(final List<String> events, final List<String> conditions) {
            final String event = events.isEmpty() ? "RIGHT_CLICK" : events.get(0);
            final StringBuilder builder = new StringBuilder();
            for (final String condition : conditions) {
                final String normalized = normalizeToken(condition);
                if ("SNEAKING".equals(normalized)) {
                    builder.append("SNEAK_");
                } else if ("NOT_SNEAKING".equals(normalized)) {
                    builder.append("NOT_SNEAK_");
                } else if ("OFF_HAND".equals(normalized)) {
                    builder.append("OFF_HAND_");
                }
            }
            builder.append(normalizeToken(event));
            return builder.toString();
        }

        private static List<String> eventsFromType(final String raw) {
            final String normalized = normalizeToken(raw);
            if (normalized.contains("LEFT")) {
                return Collections.singletonList("LEFT_CLICK");
            }
            return Collections.singletonList("RIGHT_CLICK");
        }

        private static List<String> conditionsFromType(final String raw) {
            final String normalized = normalizeToken(raw);
            final List<String> conditions = new ArrayList<String>();
            if (normalized.contains("NOT_SNEAK")) {
                conditions.add("NOT_SNEAKING");
            } else if (normalized.contains("SNEAK")) {
                conditions.add("SNEAKING");
            }
            if (normalized.contains("OFF_HAND")) {
                conditions.add("OFF_HAND");
            } else {
                conditions.add("MAIN_HAND");
            }
            return conditions;
        }

        private static List<String> stringList(final Object raw) {
            if (raw instanceof List) {
                final List<?> values = (List<?>) raw;
                final List<String> output = new ArrayList<String>(values.size());
                for (final Object value : values) {
                    output.add(String.valueOf(value));
                }
                return output;
            }
            if (raw == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(String.valueOf(raw));
        }

        private static String normalizeToken(final String raw) {
            return raw == null ? "" : raw.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
        }
    }

    private static final class MaterialsList {
        private static List<String> from(final Object raw) {
            if (raw instanceof List) {
                final List<?> values = (List<?>) raw;
                final List<String> output = new ArrayList<String>(values.size());
                for (final Object value : values) {
                    output.add(String.valueOf(value));
                }
                return output;
            }
            if (raw == null) {
                return Collections.singletonList("DIAMOND_SWORD");
            }
            return Collections.singletonList(String.valueOf(raw));
        }
    }
}
