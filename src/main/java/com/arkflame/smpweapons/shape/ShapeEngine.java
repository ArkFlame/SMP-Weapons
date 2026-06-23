package com.arkflame.smpweapons.shape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ShapeEngine {
    private final Map<String, List<BlockOffset>> cache = new HashMap<String, List<BlockOffset>>();
    private final int maxPoints;

    public ShapeEngine(final int maxPoints) {
        this.maxPoints = Math.max(1, maxPoints);
    }

    public List<BlockOffset> sphere(final int radius, final boolean hollow) {
        final int safeRadius = Math.max(0, radius);
        final String key = "sphere:" + safeRadius + ':' + hollow;
        final List<BlockOffset> cached = this.cache.get(key);
        if (cached != null) {
            return cached;
        }
        final List<BlockOffset> offsets = new ArrayList<BlockOffset>();
        final int radiusSquared = safeRadius * safeRadius;
        final int inner = Math.max(0, safeRadius - 1);
        final int innerSquared = inner * inner;
        for (int x = -safeRadius; x <= safeRadius; x++) {
            for (int y = -safeRadius; y <= safeRadius; y++) {
                for (int z = -safeRadius; z <= safeRadius; z++) {
                    final int distanceSquared = x * x + y * y + z * z;
                    if (distanceSquared > radiusSquared) {
                        continue;
                    }
                    if (hollow && safeRadius > 0 && distanceSquared <= innerSquared) {
                        continue;
                    }
                    addLimited(offsets, new BlockOffset(x, y, z));
                    if (offsets.size() >= this.maxPoints) {
                        return cache(key, offsets);
                    }
                }
            }
        }
        return cache(key, offsets);
    }

    public List<BlockOffset> cube(final int radius, final boolean hollow) {
        final int safeRadius = Math.max(0, radius);
        final String key = "cube:" + safeRadius + ':' + hollow;
        final List<BlockOffset> cached = this.cache.get(key);
        if (cached != null) {
            return cached;
        }
        return cuboid(-safeRadius, -safeRadius, -safeRadius, safeRadius, safeRadius, safeRadius, hollow, key);
    }

    public List<BlockOffset> cuboid(final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ, final boolean hollow) {
        final String key = "cuboid:" + minX + ':' + minY + ':' + minZ + ':' + maxX + ':' + maxY + ':' + maxZ + ':' + hollow;
        final List<BlockOffset> cached = this.cache.get(key);
        if (cached != null) {
            return cached;
        }
        return cuboid(minX, minY, minZ, maxX, maxY, maxZ, hollow, key);
    }

    public List<BlockOffset> cylinder(final int radius, final int height, final boolean hollow) {
        final int safeRadius = Math.max(0, radius);
        final int safeHeight = Math.max(1, height);
        final String key = "cylinder:" + safeRadius + ':' + safeHeight + ':' + hollow;
        final List<BlockOffset> cached = this.cache.get(key);
        if (cached != null) {
            return cached;
        }
        final List<BlockOffset> offsets = new ArrayList<BlockOffset>();
        final int radiusSquared = safeRadius * safeRadius;
        final int inner = Math.max(0, safeRadius - 1);
        final int innerSquared = inner * inner;
        for (int y = 0; y < safeHeight; y++) {
            for (int x = -safeRadius; x <= safeRadius; x++) {
                for (int z = -safeRadius; z <= safeRadius; z++) {
                    final int distanceSquared = x * x + z * z;
                    if (distanceSquared > radiusSquared) {
                        continue;
                    }
                    if (hollow && distanceSquared <= innerSquared && y > 0 && y < safeHeight - 1) {
                        continue;
                    }
                    addLimited(offsets, new BlockOffset(x, y, z));
                    if (offsets.size() >= this.maxPoints) {
                        return cache(key, offsets);
                    }
                }
            }
        }
        return cache(key, offsets);
    }



    public List<BlockOffset> ellipsoid(final int radiusX, final int radiusY, final int radiusZ, final boolean hollow) {
        final int rx = Math.max(1, radiusX);
        final int ry = Math.max(1, radiusY);
        final int rz = Math.max(1, radiusZ);
        final String key = "ellipsoid:" + rx + ':' + ry + ':' + rz + ':' + hollow;
        final List<BlockOffset> cached = this.cache.get(key);
        if (cached != null) {
            return cached;
        }
        final List<BlockOffset> offsets = new ArrayList<BlockOffset>();
        for (int x = -rx; x <= rx; x++) {
            for (int y = -ry; y <= ry; y++) {
                for (int z = -rz; z <= rz; z++) {
                    final double value = (x * x) / (double) (rx * rx) + (y * y) / (double) (ry * ry) + (z * z) / (double) (rz * rz);
                    if (value > 1.0D) {
                        continue;
                    }
                    if (hollow) {
                        final double inner = ((Math.abs(x) + 1) * (Math.abs(x) + 1)) / (double) (rx * rx) + ((Math.abs(y) + 1) * (Math.abs(y) + 1)) / (double) (ry * ry) + ((Math.abs(z) + 1) * (Math.abs(z) + 1)) / (double) (rz * rz);
                        if (inner <= 1.0D) {
                            continue;
                        }
                    }
                    addLimited(offsets, new BlockOffset(x, y, z));
                    if (offsets.size() >= this.maxPoints) {
                        return cache(key, offsets);
                    }
                }
            }
        }
        return cache(key, offsets);
    }

    public List<BlockOffset> cone(final int radius, final int height, final boolean hollow) {
        final int safeRadius = Math.max(1, radius);
        final int safeHeight = Math.max(1, height);
        final String key = "cone:" + safeRadius + ':' + safeHeight + ':' + hollow;
        final List<BlockOffset> cached = this.cache.get(key);
        if (cached != null) {
            return cached;
        }
        final List<BlockOffset> offsets = new ArrayList<BlockOffset>();
        for (int y = 0; y < safeHeight; y++) {
            final double progress = safeHeight <= 1 ? 1.0D : 1.0D - (y / (double) (safeHeight - 1));
            final int layerRadius = Math.max(0, (int) Math.ceil(safeRadius * progress));
            final int radiusSquared = layerRadius * layerRadius;
            final int inner = Math.max(0, layerRadius - 1);
            final int innerSquared = inner * inner;
            for (int x = -layerRadius; x <= layerRadius; x++) {
                for (int z = -layerRadius; z <= layerRadius; z++) {
                    final int distanceSquared = x * x + z * z;
                    if (distanceSquared > radiusSquared) {
                        continue;
                    }
                    if (hollow && distanceSquared <= innerSquared && y > 0 && y < safeHeight - 1) {
                        continue;
                    }
                    addLimited(offsets, new BlockOffset(x, y, z));
                    if (offsets.size() >= this.maxPoints) {
                        return cache(key, offsets);
                    }
                }
            }
        }
        return cache(key, offsets);
    }

    public List<BlockOffset> disc(final int radius, final boolean hollow) {
        final int safeRadius = Math.max(0, radius);
        final String key = "disc:" + safeRadius + ':' + hollow;
        final List<BlockOffset> cached = this.cache.get(key);
        if (cached != null) {
            return cached;
        }
        final List<BlockOffset> offsets = new ArrayList<BlockOffset>();
        final int radiusSquared = safeRadius * safeRadius;
        final int inner = Math.max(0, safeRadius - 1);
        final int innerSquared = inner * inner;
        for (int x = -safeRadius; x <= safeRadius; x++) {
            for (int z = -safeRadius; z <= safeRadius; z++) {
                final int distanceSquared = x * x + z * z;
                if (distanceSquared > radiusSquared) {
                    continue;
                }
                if (hollow && distanceSquared <= innerSquared) {
                    continue;
                }
                addLimited(offsets, new BlockOffset(x, 0, z));
                if (offsets.size() >= this.maxPoints) {
                    return cache(key, offsets);
                }
            }
        }
        return cache(key, offsets);
    }

    public List<BlockOffset> wall(final int width, final int height, final boolean hollow) {
        final int half = Math.max(0, width / 2);
        final int safeHeight = Math.max(1, height);
        final String key = "wall:" + width + ':' + safeHeight + ':' + hollow;
        final List<BlockOffset> cached = this.cache.get(key);
        if (cached != null) {
            return cached;
        }
        final List<BlockOffset> offsets = new ArrayList<BlockOffset>();
        for (int x = -half; x <= half; x++) {
            for (int y = 0; y < safeHeight; y++) {
                if (hollow && x > -half && x < half && y > 0 && y < safeHeight - 1) {
                    continue;
                }
                addLimited(offsets, new BlockOffset(x, y, 0));
                if (offsets.size() >= this.maxPoints) {
                    return cache(key, offsets);
                }
            }
        }
        return cache(key, offsets);
    }

    private List<BlockOffset> cuboid(final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ, final boolean hollow, final String key) {
        final List<BlockOffset> offsets = new ArrayList<BlockOffset>();
        final int lowX = Math.min(minX, maxX);
        final int highX = Math.max(minX, maxX);
        final int lowY = Math.min(minY, maxY);
        final int highY = Math.max(minY, maxY);
        final int lowZ = Math.min(minZ, maxZ);
        final int highZ = Math.max(minZ, maxZ);
        for (int x = lowX; x <= highX; x++) {
            for (int y = lowY; y <= highY; y++) {
                for (int z = lowZ; z <= highZ; z++) {
                    if (hollow && x > lowX && x < highX && y > lowY && y < highY && z > lowZ && z < highZ) {
                        continue;
                    }
                    addLimited(offsets, new BlockOffset(x, y, z));
                    if (offsets.size() >= this.maxPoints) {
                        return cache(key, offsets);
                    }
                }
            }
        }
        return cache(key, offsets);
    }

    private void addLimited(final List<BlockOffset> offsets, final BlockOffset offset) {
        if (offsets.size() < this.maxPoints) {
            offsets.add(offset);
        }
    }

    private List<BlockOffset> cache(final String key, final List<BlockOffset> offsets) {
        final List<BlockOffset> immutable = Collections.unmodifiableList(new ArrayList<BlockOffset>(offsets));
        this.cache.put(key, immutable);
        return immutable;
    }
}
