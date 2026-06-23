package com.arkflame.smpweapons.block;

import com.arkflame.smpweapons.shape.BlockOffset;
import com.arkflame.smpweapons.shape.ShapeEngine;
import com.arkflame.smpweapons.util.FoliaAPI;
import com.arkflame.smpweapons.util.Materials;
import com.arkflame.smpweapons.util.Particles;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TemporaryBlockService {
    private final JavaPlugin plugin;
    private final FoliaAPI scheduler;
    private final ShapeEngine shapes;
    private final Map<BlockKey, List<CreatedBlock>> createdBlocks = new HashMap<BlockKey, List<CreatedBlock>>();
    private final int maxBlocksPerTick;
    private final int maxCreatedBlocks;
    private final double viewerRadius;
    private final boolean forcePacketOnly;
    private final String cleanupParticle;
    private long sequence;

    public TemporaryBlockService(final JavaPlugin plugin, final FoliaAPI scheduler, final ShapeEngine shapes, final int maxBlocksPerTick, final int maxCreatedBlocks) {
        this(plugin, scheduler, shapes, maxBlocksPerTick, maxCreatedBlocks, 48.0D, true, "WHITE_SMOKE");
    }

    public TemporaryBlockService(final JavaPlugin plugin, final FoliaAPI scheduler, final ShapeEngine shapes, final int maxBlocksPerTick, final int maxCreatedBlocks, final double viewerRadius) {
        this(plugin, scheduler, shapes, maxBlocksPerTick, maxCreatedBlocks, viewerRadius, true, "WHITE_SMOKE");
    }

    public TemporaryBlockService(final JavaPlugin plugin, final FoliaAPI scheduler, final ShapeEngine shapes, final int maxBlocksPerTick, final int maxCreatedBlocks, final double viewerRadius, final boolean forcePacketOnly, final String cleanupParticle) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.shapes = shapes;
        this.maxBlocksPerTick = Math.max(1, maxBlocksPerTick);
        this.maxCreatedBlocks = Math.max(1, maxCreatedBlocks);
        this.viewerRadius = Math.max(8.0D, viewerRadius);
        this.forcePacketOnly = forcePacketOnly;
        this.cleanupParticle = cleanupParticle == null || cleanupParticle.trim().isEmpty() ? "WHITE_SMOKE" : cleanupParticle.trim();
    }

    public int activeBlockCount() {
        int count = 0;
        for (final List<CreatedBlock> stack : this.createdBlocks.values()) {
            count += stack == null ? 0 : stack.size();
        }
        return count;
    }

    public void placeTemporary(final Location location, final Material material, final long ttlTicks, final String mode) {
        if (location == null || material == null) {
            return;
        }
        placeVirtual(location, material, ttlTicks);
    }

    public void placeReal(final Location location, final Material material, final long ttlTicks) {
        placeVirtual(location, material, ttlTicks);
    }

    public void placeVirtual(final Location location, final Material material, final long ttlTicks) {
        if (location == null || material == null) {
            return;
        }
        this.scheduler.runRegion(location, new Runnable() {
            @Override
            public void run() {
                if (activeBlockCount() >= maxCreatedBlocks) {
                    return;
                }
                final Block block = location.getBlock();
                final BlockKey key = BlockKey.from(block.getLocation());
                final CreatedBlock created = new CreatedBlock(nextId(), block.getLocation(), block.getType(), material, "fake");
                push(key, created);
                syncNearby(block.getLocation(), material);
                scheduler.runRegionLater(block.getLocation(), new Runnable() {
                    @Override
                    public void run() {
                        expire(key, created);
                    }
                }, Math.max(1L, ttlTicks));
            }
        });
    }

    public void waveSphere(final Location center, final Material material, final int radius, final long ttlTicks, final String mode, final int realRadius, final long stepTicks, final long collapseDelayTicks) {
        if (center == null || material == null) {
            return;
        }
        final int safeRadius = Math.max(1, radius);
        final long safeStep = Math.max(1L, stepTicks);
        for (int current = 1; current <= safeRadius; current++) {
            final int currentRadius = current;
            this.scheduler.runRegionLater(center, new Runnable() {
                @Override
                public void run() {
                    placeSphereShell(center, material, currentRadius, ttlTicks, resolveMode(mode, currentRadius, realRadius));
                }
            }, current * safeStep);
        }
        for (int current = safeRadius; current >= 1; current--) {
            final int currentRadius = current;
            final long delay = safeRadius * safeStep + Math.max(0L, collapseDelayTicks) + (safeRadius - currentRadius + 1L) * safeStep;
            this.scheduler.runRegionLater(center, new Runnable() {
                @Override
                public void run() {
                    restoreSphereShell(center, currentRadius);
                }
            }, delay);
        }
    }

    public void placeShape(final Location center, final Material material, final String shapeType, final int radius, final int height, final boolean hollow, final long ttlTicks, final String mode) {
        if (center == null || material == null) {
            return;
        }
        final List<BlockOffset> offsets = shapeOffsets(shapeType, radius, height, hollow);
        processOffsets(center, offsets, new OffsetConsumer() {
            @Override
            public void accept(final Location location) {
                placeTemporary(location, material, ttlTicks, mode);
            }
        });
    }

    private List<BlockOffset> shapeOffsets(final String rawType, final int radius, final int height, final boolean hollow) {
        final String type = rawType == null ? "SPHERE" : rawType.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        final int safeRadius = Math.max(1, radius);
        final int safeHeight = Math.max(1, height);
        if ("CUBE".equals(type)) {
            return this.shapes.cube(safeRadius, hollow);
        }
        if ("CYLINDER".equals(type)) {
            return this.shapes.cylinder(safeRadius, safeHeight, hollow);
        }
        if ("CONE".equals(type)) {
            return this.shapes.cone(safeRadius, safeHeight, hollow);
        }
        if ("DISC".equals(type) || "RING".equals(type)) {
            return this.shapes.disc(safeRadius, true);
        }
        if ("WALL".equals(type)) {
            return this.shapes.wall(safeRadius * 2 + 1, safeHeight, hollow);
        }
        if ("ELLIPSOID".equals(type)) {
            return this.shapes.ellipsoid(safeRadius, Math.max(1, safeHeight / 2), safeRadius, hollow);
        }
        return this.shapes.sphere(safeRadius, hollow);
    }

    public void clearCobwebs(final Location center, final int radius) {
        if (center == null || center.getWorld() == null) {
            return;
        }
        final List<BlockOffset> offsets = this.shapes.sphere(Math.max(1, radius), false);
        processOffsets(center, offsets, new OffsetConsumer() {
            @Override
            public void accept(final Location location) {
                final Block block = location.getBlock();
                if (Materials.isCobweb(block.getType())) {
                    block.setType(Material.AIR);
                }
            }
        });
    }

    public void syncBlock(final Player player, final Location location) {
        if (player == null || location == null) {
            return;
        }
        final CreatedBlock created = top(BlockKey.from(location));
        if (created == null) {
            sendRealBlock(player, location);
            return;
        }
        sendBlock(player, created.getLocation(), created.getMaterial());
    }

    public void syncAllTo(final Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        for (final List<CreatedBlock> stack : new ArrayList<List<CreatedBlock>>(this.createdBlocks.values())) {
            final CreatedBlock created = top(stack);
            if (created == null || !"fake".equals(created.getMode())) {
                continue;
            }
            if (created.getLocation().getWorld() == player.getWorld() && created.getLocation().distanceSquared(player.getLocation()) <= this.viewerRadius * this.viewerRadius) {
                sendBlock(player, created.getLocation(), created.getMaterial());
            }
        }
    }

    public void restoreChunk(final Chunk chunk) {
        if (chunk == null) {
            return;
        }
        final List<BlockKey> keys = new ArrayList<BlockKey>();
        for (final Map.Entry<BlockKey, List<CreatedBlock>> entry : this.createdBlocks.entrySet()) {
            final CreatedBlock created = top(entry.getValue());
            if (created == null) {
                continue;
            }
            final Location location = created.getLocation();
            if (location.getWorld() == chunk.getWorld() && location.getBlockX() >> 4 == chunk.getX() && location.getBlockZ() >> 4 == chunk.getZ()) {
                keys.add(entry.getKey());
            }
        }
        for (final BlockKey key : keys) {
            restore(key);
        }
    }

    public void restore(final BlockKey key) {
        if (key == null) {
            return;
        }
        final CreatedBlock removed = pop(key);
        if (removed == null) {
            return;
        }
        finishRemoval(key, removed);
    }

    public void restoreAll() {
        final List<BlockKey> keys = new ArrayList<BlockKey>(this.createdBlocks.keySet());
        for (final BlockKey key : keys) {
            while (top(key) != null) {
                restore(key);
            }
        }
    }

    public void restoreAllNow() {
        final List<CreatedBlock> blocks = new ArrayList<CreatedBlock>();
        for (final List<CreatedBlock> stack : this.createdBlocks.values()) {
            if (stack != null) {
                blocks.addAll(stack);
            }
        }
        this.createdBlocks.clear();
        final java.util.HashSet<BlockKey> sent = new java.util.HashSet<BlockKey>();
        for (final CreatedBlock created : blocks) {
            if (created == null) {
                continue;
            }
            final BlockKey key = BlockKey.from(created.getLocation());
            if ("fake".equals(created.getMode())) {
                if (sent.add(key)) {
                    spawnCleanup(created.getLocation());
                    sendRealNearby(created.getLocation());
                }
                continue;
            }
            spawnCleanup(created.getLocation());
            sendRealNearby(created.getLocation());
        }
    }

    private void placeSphereShell(final Location center, final Material material, final int radius, final long ttlTicks, final String mode) {
        final List<BlockOffset> offsets = this.shapes.sphere(radius, true);
        processOffsets(center, offsets, new OffsetConsumer() {
            @Override
            public void accept(final Location location) {
                placeTemporary(location, material, ttlTicks, mode);
            }
        });
    }

    private void restoreSphereShell(final Location center, final int radius) {
        final List<BlockOffset> offsets = this.shapes.sphere(radius, true);
        processOffsets(center, offsets, new OffsetConsumer() {
            @Override
            public void accept(final Location location) {
                final BlockKey key = BlockKey.from(location);
                if (top(key) != null) {
                    restore(key);
                }
            }
        });
    }

    private void processOffsets(final Location center, final List<BlockOffset> offsets, final OffsetConsumer consumer) {
        if (center == null || offsets == null || consumer == null) {
            return;
        }
        int processed = 0;
        for (final BlockOffset offset : offsets) {
            final long delay = processed / this.maxBlocksPerTick;
            processed++;
            final Location location = center.clone().add(offset.getX(), offset.getY(), offset.getZ());
            this.scheduler.runRegionLater(location, new Runnable() {
                @Override
                public void run() {
                    consumer.accept(location);
                }
            }, delay);
        }
    }

    private String resolveMode(final String mode, final int radius, final int realRadius) {
        return "fake";
    }

    private void syncNearby(final Location location, final Material material) {
        for (final Player player : nearbyPlayers(location, this.viewerRadius)) {
            sendBlock(player, location, material);
        }
    }

    private void sendRealNearby(final Location location) {
        for (final Player player : nearbyPlayers(location, this.viewerRadius)) {
            sendRealBlock(player, location);
        }
    }

    private Collection<Player> nearbyPlayers(final Location location, final double radius) {
        final List<Player> players = new ArrayList<Player>();
        if (location == null || location.getWorld() == null) {
            return players;
        }
        final double radiusSquared = radius * radius;
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline() || player.getWorld() != location.getWorld()) {
                continue;
            }
            if (player.getLocation().distanceSquared(location) <= radiusSquared) {
                players.add(player);
            }
        }
        return players;
    }

    private synchronized long nextId() {
        this.sequence++;
        return this.sequence;
    }

    private void push(final BlockKey key, final CreatedBlock created) {
        List<CreatedBlock> stack = this.createdBlocks.get(key);
        if (stack == null) {
            stack = new ArrayList<CreatedBlock>();
            this.createdBlocks.put(key, stack);
        }
        stack.add(created);
    }

    private CreatedBlock pop(final BlockKey key) {
        final List<CreatedBlock> stack = this.createdBlocks.get(key);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        final CreatedBlock removed = stack.remove(stack.size() - 1);
        if (stack.isEmpty()) {
            this.createdBlocks.remove(key);
        }
        return removed;
    }

    private void expire(final BlockKey key, final CreatedBlock created) {
        if (key == null || created == null) {
            return;
        }
        final List<CreatedBlock> stack = this.createdBlocks.get(key);
        if (stack == null || !stack.remove(created)) {
            return;
        }
        if (stack.isEmpty()) {
            this.createdBlocks.remove(key);
        }
        finishRemoval(key, created);
    }

    private void finishRemoval(final BlockKey key, final CreatedBlock removed) {
        if (removed == null) {
            return;
        }
        if ("fake".equals(removed.getMode())) {
            final CreatedBlock next = top(key);
            if (next == null) {
                spawnCleanup(removed.getLocation());
                sendRealNearby(removed.getLocation());
            } else {
                sendNearbyTop(next);
            }
            return;
        }
        spawnCleanup(removed.getLocation());
        sendRealNearby(removed.getLocation());
    }

    private void sendNearbyTop(final CreatedBlock created) {
        for (final Player player : nearbyPlayers(created.getLocation(), this.viewerRadius)) {
            sendBlock(player, created.getLocation(), created.getMaterial());
        }
    }

    private CreatedBlock top(final BlockKey key) {
        return top(this.createdBlocks.get(key));
    }

    private CreatedBlock top(final List<CreatedBlock> stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.get(stack.size() - 1);
    }

    private void spawnCleanup(final Location location) {
        if (location != null) {
            Particles.spawn(location.clone().add(0.5D, 0.5D, 0.5D), this.cleanupParticle, 4);
        }
    }

    private void sendRealBlock(final Player player, final Location location) {
        if (player == null || location == null) {
            return;
        }
        final Block block = location.getBlock();
        try {
            final Method getBlockData = block.getClass().getMethod("getBlockData");
            final Object blockData = getBlockData.invoke(block);
            final Class<?> blockDataClass = Class.forName("org.bukkit.block.data.BlockData");
            final Method method = player.getClass().getMethod("sendBlockChange", Location.class, blockDataClass);
            method.invoke(player, block.getLocation(), blockData);
            return;
        } catch (final Exception ignored) {
            // legacy fallback below
        }
        sendBlockLegacy(player, block.getLocation(), block.getType());
    }

    private void sendBlock(final Player player, final Location location, final Material material) {
        if (player == null || location == null || material == null) {
            return;
        }
        try {
            final Method createBlockData = material.getClass().getMethod("createBlockData");
            final Object blockData = createBlockData.invoke(material);
            final Class<?> blockDataClass = Class.forName("org.bukkit.block.data.BlockData");
            final Method method = player.getClass().getMethod("sendBlockChange", Location.class, blockDataClass);
            method.invoke(player, location, blockData);
            return;
        } catch (final Exception ignored) {
            // legacy fallback below
        }
        sendBlockLegacy(player, location, material);
    }

    private void sendBlockLegacy(final Player player, final Location location, final Material material) {
        try {
            final Method method = player.getClass().getMethod("sendBlockChange", Location.class, Material.class, byte.class);
            method.invoke(player, location, material, Byte.valueOf((byte) 0));
        } catch (final Exception ignored) {
            // visual sync only
        }
    }

    private interface OffsetConsumer {
        void accept(Location location);
    }

    private static final class CreatedBlock {
        private final long id;
        private final Location location;
        private final Material original;
        private final Material material;
        private final String mode;

        private CreatedBlock(final long id, final Location location, final Material original, final Material material, final String mode) {
            this.id = id;
            this.location = location.clone();
            this.original = original == null ? Material.AIR : original;
            this.material = material;
            this.mode = mode;
        }

        private Location getLocation() {
            return location.clone();
        }

        private Material getOriginal() {
            return original;
        }

        private Material getMaterial() {
            return material;
        }

        private String getMode() {
            return mode;
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof CreatedBlock && ((CreatedBlock) other).id == this.id;
        }

        @Override
        public int hashCode() {
            return Long.valueOf(this.id).hashCode();
        }
    }
}
