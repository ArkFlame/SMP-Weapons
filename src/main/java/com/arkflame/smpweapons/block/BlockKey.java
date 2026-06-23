package com.arkflame.smpweapons.block;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public final class BlockKey {
    private final UUID worldId;
    private final int x;
    private final int y;
    private final int z;

    public BlockKey(final UUID worldId, final int x, final int y, final int z) {
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static BlockKey from(final Location location) {
        final World world = location == null ? null : location.getWorld();
        return new BlockKey(world == null ? new UUID(0L, 0L) : world.getUID(), location == null ? 0 : location.getBlockX(), location == null ? 0 : location.getBlockY(), location == null ? 0 : location.getBlockZ());
    }

    public UUID getWorldId() {
        return worldId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BlockKey)) {
            return false;
        }
        final BlockKey that = (BlockKey) other;
        return this.x == that.x && this.y == that.y && this.z == that.z && this.worldId.equals(that.worldId);
    }

    @Override
    public int hashCode() {
        int result = this.worldId.hashCode();
        result = 31 * result + Integer.valueOf(this.x).hashCode();
        result = 31 * result + Integer.valueOf(this.y).hashCode();
        result = 31 * result + Integer.valueOf(this.z).hashCode();
        return result;
    }

    @Override
    public String toString() {
        return this.worldId + ":" + this.x + ':' + this.y + ':' + this.z;
    }
}
