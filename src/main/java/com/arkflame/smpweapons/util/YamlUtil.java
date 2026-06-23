package com.arkflame.smpweapons.util;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class YamlUtil {
    private YamlUtil() {
    }

    public static Map<String, Object> map(final ConfigurationSection section) {
        if (section == null) {
            return Collections.emptyMap();
        }
        final Map<String, Object> output = new LinkedHashMap<String, Object>();
        for (final String key : section.getKeys(false)) {
            final Object value = section.get(key);
            if (value instanceof ConfigurationSection) {
                output.put(key, map((ConfigurationSection) value));
            } else {
                output.put(key, value);
            }
        }
        return output;
    }

    public static List<String> stringList(final ConfigurationSection section, final String path) {
        if (section == null) {
            return Collections.emptyList();
        }
        if (section.isList(path)) {
            return section.getStringList(path);
        }
        final String value = section.getString(path, "");
        if (value.isEmpty()) {
            return Collections.emptyList();
        }
        final List<String> list = new ArrayList<String>(1);
        list.add(value);
        return list;
    }
}
