package com.arkflame.smpweapons.util;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class Materials {
    private Materials() {
    }

    public static Optional<Material> find(final Iterable<String> aliases) {
        if (aliases == null) {
            return Optional.empty();
        }
        for (final String alias : aliases) {
            final Optional<Material> material = find(alias);
            if (material.isPresent()) {
                return material;
            }
        }
        return Optional.empty();
    }

    public static Optional<Material> find(final String alias) {
        if (alias == null || alias.trim().isEmpty()) {
            return Optional.empty();
        }
        final String normalized = alias.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        final Material direct = Material.getMaterial(normalized);
        if (direct != null) {
            return Optional.of(direct);
        }
        for (final String legacy : legacyAliases(normalized)) {
            final Material legacyMaterial = Material.getMaterial(legacy);
            if (legacyMaterial != null) {
                return Optional.of(legacyMaterial);
            }
        }
        return Optional.empty();
    }

    public static Material fallback(final Material fallback, final Iterable<String> aliases) {
        return find(aliases).orElse(fallback);
    }

    public static List<String> list(final Object raw) {
        if (raw instanceof List) {
            final List<?> list = (List<?>) raw;
            final List<String> output = new ArrayList<String>(list.size());
            for (final Object value : list) {
                output.add(String.valueOf(value));
            }
            return output;
        }
        if (raw == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(String.valueOf(raw));
    }

    public static boolean isCobweb(final Material material) {
        if (material == null) {
            return false;
        }
        return material.name().equals("WEB") || material.name().equals("COBWEB");
    }

    public static boolean isAir(final Material material) {
        if (material == null) {
            return true;
        }
        return material == Material.AIR || material.name().endsWith("_AIR");
    }

    private static String[] legacyAliases(final String normalized) {
        if ("COBWEB".equals(normalized)) {
            return new String[]{"WEB"};
        }
        if ("GRAY_STAINED_GLASS_PANE".equals(normalized) || "GREY_STAINED_GLASS_PANE".equals(normalized)) {
            return new String[]{"STAINED_GLASS_PANE"};
        }
        if ("GRAY_STAINED_GLASS".equals(normalized) || "GREY_STAINED_GLASS".equals(normalized)) {
            return new String[]{"STAINED_GLASS"};
        }
        if ("NETHERITE_SWORD".equals(normalized)) {
            return new String[]{"DIAMOND_SWORD"};
        }
        if ("NETHERITE_AXE".equals(normalized)) {
            return new String[]{"DIAMOND_AXE"};
        }
        if ("MACE".equals(normalized)) {
            return new String[]{"DIAMOND_AXE"};
        }
        if ("NETHERITE_SPEAR".equals(normalized)) {
            return new String[]{"TRIDENT"};
        }
        if ("TRIDENT".equals(normalized)) {
            return new String[]{"DIAMOND_SWORD"};
        }
        if ("CROSSBOW".equals(normalized)) {
            return new String[]{"BOW"};
        }
        if ("SHIELD".equals(normalized)) {
            return new String[]{"BANNER", "WALL_BANNER", "SIGN"};
        }
        if ("TOTEM_OF_UNDYING".equals(normalized)) {
            return new String[]{"GOLDEN_APPLE"};
        }
        if ("HEAVY_CORE".equals(normalized)) {
            return new String[]{"NETHER_STAR"};
        }
        if ("HEART_OF_THE_SEA".equals(normalized)) {
            return new String[]{"PRISMARINE_CRYSTALS", "NETHER_STAR"};
        }
        if ("TIPPED_ARROW".equals(normalized) || "SPECTRAL_ARROW".equals(normalized)) {
            return new String[]{"ARROW"};
        }
        return new String[0];
    }
}
