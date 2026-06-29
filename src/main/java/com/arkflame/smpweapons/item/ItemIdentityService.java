package com.arkflame.smpweapons.item;

import com.arkflame.smpweapons.model.WeaponDefinition;
import com.arkflame.smpweapons.util.Materials;
import com.arkflame.smpweapons.util.TextBridge;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;

public final class ItemIdentityService {
    private final JavaPlugin plugin;
    private final Method metaGetPersistentDataContainer;
    private final Method containerSet;
    private final Method containerGet;
    private final Object stringPersistentDataType;
    private final Object key;
    private final Method metaHasCustomModelData;
    private final Method metaGetCustomModelData;
    private final Method metaSetCustomModelData;

    public ItemIdentityService(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.metaGetPersistentDataContainer = method("org.bukkit.inventory.meta.ItemMeta", "getPersistentDataContainer");
        this.containerSet = method("org.bukkit.persistence.PersistentDataContainer", "set", classOrNull("org.bukkit.NamespacedKey"), classOrNull("org.bukkit.persistence.PersistentDataType"), Object.class);
        this.containerGet = method("org.bukkit.persistence.PersistentDataContainer", "get", classOrNull("org.bukkit.NamespacedKey"), classOrNull("org.bukkit.persistence.PersistentDataType"));
        this.stringPersistentDataType = staticField("org.bukkit.persistence.PersistentDataType", "STRING");
        this.key = createKey(plugin, "weapon_id");
        this.metaHasCustomModelData = method("org.bukkit.inventory.meta.ItemMeta", "hasCustomModelData");
        this.metaGetCustomModelData = method("org.bukkit.inventory.meta.ItemMeta", "getCustomModelData");
        this.metaSetCustomModelData = method("org.bukkit.inventory.meta.ItemMeta", "setCustomModelData", Integer.class);
    }

    public void write(final ItemMeta meta, final String weaponId) {
        if (meta == null || weaponId == null || this.metaGetPersistentDataContainer == null || this.containerSet == null || this.key == null || this.stringPersistentDataType == null) {
            return;
        }
        try {
            final Object container = this.metaGetPersistentDataContainer.invoke(meta);
            this.containerSet.invoke(container, this.key, this.stringPersistentDataType, weaponId);
        } catch (final Exception ignored) {
            // old server, no PDC
        }
    }

    public Optional<String> read(final ItemMeta meta) {
        if (meta == null || this.metaGetPersistentDataContainer == null || this.containerGet == null || this.key == null || this.stringPersistentDataType == null) {
            return Optional.empty();
        }
        try {
            final Object container = this.metaGetPersistentDataContainer.invoke(meta);
            final Object value = this.containerGet.invoke(container, this.key, this.stringPersistentDataType);
            if (value instanceof String && !((String) value).trim().isEmpty()) {
                return Optional.of((String) value);
            }
        } catch (final Exception ignored) { }
        return Optional.empty();
    }

    public Optional<String> read(final ItemStack item) {
        if (item == null || !item.hasItemMeta() || this.metaGetPersistentDataContainer == null || this.containerGet == null || this.key == null || this.stringPersistentDataType == null) {
            return Optional.empty();
        }
        return read(item.getItemMeta());
    }

    public boolean matches(final ItemStack item, final WeaponDefinition definition) {
        if (item == null || definition == null || !item.hasItemMeta()) {
            return false;
        }
        final ItemMeta meta = item.getItemMeta();
        final Optional<String> stored = read(meta);
        if (stored.isPresent() && stored.get().equalsIgnoreCase(definition.getId())) {
            return true;
        }
        return matchesPrepared(item, meta, definition);
    }

    public boolean matchesPrepared(final ItemStack item, final ItemMeta meta, final WeaponDefinition definition) {
        if (item == null || meta == null || definition == null) {
            return false;
        }
        final Optional<org.bukkit.Material> resolvedMaterial = Materials.find(definition.getMaterialAliases());
        final boolean materialMatches = resolvedMaterial.isPresent() && item.getType() == resolvedMaterial.get();
        final Integer model = getCustomModelData(meta);
        if (materialMatches && definition.getLegacyCustomModelData() != null && model != null && definition.getLegacyCustomModelData().intValue() == model.intValue()) {
            return true;
        }
        if (materialMatches && definition.getCustomModelData() != null && model != null && definition.getCustomModelData().intValue() == model.intValue()) {
            return true;
        }
        if (meta.hasDisplayName()) {
            final String plain = TextBridge.normalizePlain(meta.getDisplayName());
            for (final String name : definition.getLegacyNamesContains()) {
                if (!name.trim().isEmpty() && plain.contains(TextBridge.normalizePlain(name))) {
                    return true;
                }
            }
            if (plain.equals(TextBridge.normalizePlain(definition.getName()))) {
                return true;
            }
        }
        if (meta.hasLore() && meta.getLore() != null && !definition.getLegacyLoreContains().isEmpty()) {
            final StringBuilder loreText = new StringBuilder();
            for (final String line : meta.getLore()) {
                loreText.append(TextBridge.normalizePlain(line)).append('\n');
            }
            final String plainLore = loreText.toString();
            for (final String fragment : definition.getLegacyLoreContains()) {
                if (!fragment.trim().isEmpty() && plainLore.contains(TextBridge.normalizePlain(fragment))) {
                    return true;
                }
            }
        }
        return false;
    }

    public Integer getCustomModelData(final ItemMeta meta) {
        if (meta == null || this.metaHasCustomModelData == null || this.metaGetCustomModelData == null) {
            return null;
        }
        try {
            final Object hasValue = this.metaHasCustomModelData.invoke(meta);
            if (!(hasValue instanceof Boolean) || !((Boolean) hasValue).booleanValue()) {
                return null;
            }
            final Object value = this.metaGetCustomModelData.invoke(meta);
            return value instanceof Integer ? (Integer) value : null;
        } catch (final Exception ignored) {
            return null;
        }
    }

    public void setCustomModelData(final ItemMeta meta, final Integer data) {
        if (meta == null || data == null || this.metaSetCustomModelData == null) {
            return;
        }
        try {
            this.metaSetCustomModelData.invoke(meta, data);
        } catch (final Exception ignored) {
            // 1.8 no custom model data API
        }
    }

    private static Object createKey(final JavaPlugin plugin, final String name) {
        try {
            final Class<?> keyClass = Class.forName("org.bukkit.NamespacedKey");
            return keyClass.getConstructor(org.bukkit.plugin.Plugin.class, String.class).newInstance(plugin, name);
        } catch (final Throwable ignored) {
            return null;
        }
    }

    private static Class<?> classOrNull(final String name) {
        try {
            return Class.forName(name);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private static Method method(final String owner, final String name, final Class<?>... parameterTypes) {
        try {
            final Class<?> clazz = Class.forName(owner);
            return clazz.getMethod(name, parameterTypes);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private static Object staticField(final String owner, final String name) {
        try {
            return Class.forName(owner).getField(name).get(null);
        } catch (final Exception ignored) {
            return null;
        }
    }
}
