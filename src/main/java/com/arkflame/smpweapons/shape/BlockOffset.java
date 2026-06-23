package com.arkflame.smpweapons.shape;

public final class BlockOffset {
    private final int x;
    private final int y;
    private final int z;

    public BlockOffset(final int x, final int y, final int z) {
        this.x = x;
        this.y = y;
        this.z = z;
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
        if (!(other instanceof BlockOffset)) {
            return false;
        }
        final BlockOffset that = (BlockOffset) other;
        return this.x == that.x && this.y == that.y && this.z == that.z;
    }

    @Override
    public int hashCode() {
        int result = Integer.valueOf(x).hashCode();
        result = 31 * result + Integer.valueOf(y).hashCode();
        result = 31 * result + Integer.valueOf(z).hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BlockOffset{x=" + x + ", y=" + y + ", z=" + z + '}';
    }
}
