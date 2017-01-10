/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.regions;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.collection.LocalBlockVectorSet;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.storage.ChunkStore;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An axis-aligned cuboid. It can be defined using two corners of the cuboid.
 */
public class CuboidRegion extends AbstractRegion implements FlatRegion {

    private Vector pos1;
    private Vector pos2;
    private int minX,minY,minZ,maxX,maxY,maxZ;

    /**
     * Construct a new instance of this cuboid using two corners of the cuboid.
     *
     * @param pos1 the first position
     * @param pos2 the second position
     */
    public CuboidRegion(Vector pos1, Vector pos2) {
        this(null, pos1, pos2);
    }

    /**
     * @deprecated cast {@code world} to {@link World}
     */
    @Deprecated
    public CuboidRegion(LocalWorld world, Vector pos1, Vector pos2) {
        this((World) world, pos1, pos2);
    }

    /**
     * Construct a new instance of this cuboid using two corners of the cuboid.
     *
     * @param world the world
     * @param pos1  the first position
     * @param pos2  the second position
     */
    public CuboidRegion(World world, Vector pos1, Vector pos2) {
        super(world);
        checkNotNull(pos1);
        checkNotNull(pos2);
        this.pos1 = pos1;
        this.pos2 = pos2;
        recalculate();
    }

    /**
     * Get the first cuboid-defining corner.
     *
     * @return a position
     */
    public Vector getPos1() {
        return pos1;
    }

    /**
     * Set the first cuboid-defining corner.
     *
     * @param pos1 a position
     */
    public void setPos1(Vector pos1) {
        this.pos1 = pos1;
        recalculate();
    }

    /**
     * Get the second cuboid-defining corner.
     *
     * @return a position
     */
    public Vector getPos2() {
        return pos2;
    }

    /**
     * Set the second cuboid-defining corner.
     *
     * @param pos2 a position
     */
    public void setPos2(Vector pos2) {
        this.pos2 = pos2;
        recalculate();
    }

    /**
     * Clamps the cuboid according to boundaries of the world.
     */
    private void recalculate() {
        if (pos1 == null || pos2 == null) {
            return;
        }
        pos1 = pos1.clampY(0, world == null ? 255 : world.getMaxY());
        pos2 = pos2.clampY(0, world == null ? 255 : world.getMaxY());
        Vector min = getMinimumPoint();
        Vector max = getMaximumPoint();
        minX = min.getBlockX();
        minY = min.getBlockY();
        minZ = min.getBlockZ();
        maxX = max.getBlockX();
        maxY = max.getBlockY();
        maxZ = max.getBlockZ();
    }

    /**
     * Get a region that contains the faces of this cuboid.
     *
     * @return a new complex region
     */
    public Region getFaces() {
        Vector min = getMinimumPoint();
        Vector max = getMaximumPoint();

        return new RegionIntersection(
                // Project to Z-Y plane
                new CuboidRegion(pos1.setX(min.getX()), pos2.setX(min.getX())),
                new CuboidRegion(pos1.setX(max.getX()), pos2.setX(max.getX())),

                // Project to X-Y plane
                new CuboidRegion(pos1.setZ(min.getZ()), pos2.setZ(min.getZ())),
                new CuboidRegion(pos1.setZ(max.getZ()), pos2.setZ(max.getZ())),

                // Project to the X-Z plane
                new CuboidRegion(pos1.setY(min.getY()), pos2.setY(min.getY())),
                new CuboidRegion(pos1.setY(max.getY()), pos2.setY(max.getY())));
    }

    /**
     * Get a region that contains the walls (all faces but the ones parallel to
     * the X-Z plane) of this cuboid.
     *
     * @return a new complex region
     */
    public Region getWalls() {
        Vector min = getMinimumPoint();
        Vector max = getMaximumPoint();

        return new RegionIntersection(
                // Project to Z-Y plane
                new CuboidRegion(pos1.setX(min.getX()), pos2.setX(min.getX())),
                new CuboidRegion(pos1.setX(max.getX()), pos2.setX(max.getX())),

                // Project to X-Y plane
                new CuboidRegion(pos1.setZ(min.getZ()), pos2.setZ(min.getZ())),
                new CuboidRegion(pos1.setZ(max.getZ()), pos2.setZ(max.getZ())));
    }

    @Override
    public Vector getMinimumPoint() {
        return new Vector(Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ()));
    }

    @Override
    public Vector getMaximumPoint() {
        return new Vector(Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ()));
    }

    @Override
    public int getMinimumY() {
        return Math.min(pos1.getBlockY(), pos2.getBlockY());
    }

    @Override
    public int getMaximumY() {
        return Math.max(pos1.getBlockY(), pos2.getBlockY());
    }

    @Override
    public void expand(Vector... changes) {
        checkNotNull(changes);

        for (Vector change : changes) {
            if (change.getX() > 0) {
                if (Math.max(pos1.getX(), pos2.getX()) == pos1.getX()) {
                    pos1 = pos1.add(new Vector(change.getX(), 0, 0));
                } else {
                    pos2 = pos2.add(new Vector(change.getX(), 0, 0));
                }
            } else {
                if (Math.min(pos1.getX(), pos2.getX()) == pos1.getX()) {
                    pos1 = pos1.add(new Vector(change.getX(), 0, 0));
                } else {
                    pos2 = pos2.add(new Vector(change.getX(), 0, 0));
                }
            }

            if (change.getY() > 0) {
                if (Math.max(pos1.getY(), pos2.getY()) == pos1.getY()) {
                    pos1 = pos1.add(new Vector(0, change.getY(), 0));
                } else {
                    pos2 = pos2.add(new Vector(0, change.getY(), 0));
                }
            } else {
                if (Math.min(pos1.getY(), pos2.getY()) == pos1.getY()) {
                    pos1 = pos1.add(new Vector(0, change.getY(), 0));
                } else {
                    pos2 = pos2.add(new Vector(0, change.getY(), 0));
                }
            }

            if (change.getZ() > 0) {
                if (Math.max(pos1.getZ(), pos2.getZ()) == pos1.getZ()) {
                    pos1 = pos1.add(new Vector(0, 0, change.getZ()));
                } else {
                    pos2 = pos2.add(new Vector(0, 0, change.getZ()));
                }
            } else {
                if (Math.min(pos1.getZ(), pos2.getZ()) == pos1.getZ()) {
                    pos1 = pos1.add(new Vector(0, 0, change.getZ()));
                } else {
                    pos2 = pos2.add(new Vector(0, 0, change.getZ()));
                }
            }
        }

        recalculate();
    }

    @Override
    public void contract(Vector... changes) {
        checkNotNull(changes);

        for (Vector change : changes) {
            if (change.getX() < 0) {
                if (Math.max(pos1.getX(), pos2.getX()) == pos1.getX()) {
                    pos1 = pos1.add(new Vector(change.getX(), 0, 0));
                } else {
                    pos2 = pos2.add(new Vector(change.getX(), 0, 0));
                }
            } else {
                if (Math.min(pos1.getX(), pos2.getX()) == pos1.getX()) {
                    pos1 = pos1.add(new Vector(change.getX(), 0, 0));
                } else {
                    pos2 = pos2.add(new Vector(change.getX(), 0, 0));
                }
            }

            if (change.getY() < 0) {
                if (Math.max(pos1.getY(), pos2.getY()) == pos1.getY()) {
                    pos1 = pos1.add(new Vector(0, change.getY(), 0));
                } else {
                    pos2 = pos2.add(new Vector(0, change.getY(), 0));
                }
            } else {
                if (Math.min(pos1.getY(), pos2.getY()) == pos1.getY()) {
                    pos1 = pos1.add(new Vector(0, change.getY(), 0));
                } else {
                    pos2 = pos2.add(new Vector(0, change.getY(), 0));
                }
            }

            if (change.getZ() < 0) {
                if (Math.max(pos1.getZ(), pos2.getZ()) == pos1.getZ()) {
                    pos1 = pos1.add(new Vector(0, 0, change.getZ()));
                } else {
                    pos2 = pos2.add(new Vector(0, 0, change.getZ()));
                }
            } else {
                if (Math.min(pos1.getZ(), pos2.getZ()) == pos1.getZ()) {
                    pos1 = pos1.add(new Vector(0, 0, change.getZ()));
                } else {
                    pos2 = pos2.add(new Vector(0, 0, change.getZ()));
                }
            }
        }

        recalculate();
    }

    @Override
    public void shift(Vector change) throws RegionOperationException {
        pos1 = pos1.add(change);
        pos2 = pos2.add(change);

        recalculate();
    }

    @Override
    public Set<Vector2D> getChunks() {
        Set<Vector2D> chunks = new LinkedHashSet<Vector2D>();

        Vector min = getMinimumPoint();
        Vector max = getMaximumPoint();

        for (int x = max.getBlockX() >> ChunkStore.CHUNK_SHIFTS; x >= min.getBlockX() >> ChunkStore.CHUNK_SHIFTS; --x) {
            for (int z = max.getBlockZ() >> ChunkStore.CHUNK_SHIFTS; z >= min.getBlockZ() >> ChunkStore.CHUNK_SHIFTS; --z) {
                chunks.add(new Vector2D(x, z));
            }
        }

        return chunks;
    }

    @Override
    public Set<Vector> getChunkCubes() {
        Set chunks = new LocalBlockVectorSet();

        Vector min = getMinimumPoint();
        Vector max = getMaximumPoint();

        for (int x = max.getBlockX() >> ChunkStore.CHUNK_SHIFTS; x >= min.getBlockX() >> ChunkStore.CHUNK_SHIFTS; --x) {
            for (int z = max.getBlockZ() >> ChunkStore.CHUNK_SHIFTS; z >= min.getBlockZ() >> ChunkStore.CHUNK_SHIFTS; --z) {
                for (int y = max.getBlockY() >> ChunkStore.CHUNK_SHIFTS; y >= min.getBlockY() >> ChunkStore.CHUNK_SHIFTS; --y) {
                    chunks.add(new Vector(x, y, z));
                }
            }
        }

        return chunks;
    }

    @Override
    public boolean contains(Vector position) {
        int x = position.getBlockX();
        int y = position.getBlockY();
        int z = position.getBlockZ();
        return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ && y >= this.minY && y <= this.maxY;
    }

    @Override
    public Iterator<BlockVector> iterator() {
        if (Settings.HISTORY.COMPRESSION_LEVEL >= 9) {
            return iterator_old();
        }
        final BlockVector v = new BlockVector(0,0,0);
        return new Iterator<BlockVector>() {
            private Vector min = getMinimumPoint();
            private Vector max = getMaximumPoint();

            int bx = min.getBlockX();
            int by = min.getBlockY();
            int bz = min.getBlockZ();

            int tx = max.getBlockX();
            int ty = max.getBlockY();
            int tz = max.getBlockZ();

            private int x = min.getBlockX();
            private int y = min.getBlockY();
            private int z = min.getBlockZ();

            int cx = x >> 4;
            int cz = z >> 4;
            int cbx = Math.max(bx, cx << 4);
            int cbz = Math.max(bz, cz << 4);
            int ctx = Math.min(tx, 15 + (cx << 4));
            int ctz = Math.min(tz, 15 + (cz << 4));

            public boolean hasNext = true;

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public BlockVector next() {
                v.x = x;
                v.y = y;
                v.z = z;
                if (++x > ctx) {
                    if (++z > ctz) {
                        if (++y > ty) {
                            y = by;
                            if (x > tx) {
                                x = bx;
                                if (z > tz) {
                                    hasNext = false;
                                    return v;
                                }
                            } else {
                                z = cbz;
                            }
                            cx = x >> 4;
                            cz = z >> 4;
                            cbx = Math.max(bx, cx << 4);
                            cbz = Math.max(bz, cz << 4);
                            ctx = Math.min(tx, 15 + (cx << 4));
                            ctz = Math.min(tz, 15 + (cz << 4));
                        } else {
                            x = cbx;
                            z = cbz;
                        }
                    } else {
                        x = cbx;
                    }
                }
                return v;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public Iterator<BlockVector> iterator_old() {
        final BlockVector v = new BlockVector(0,0,0);
        return new Iterator<BlockVector>() {
            private Vector min = getMinimumPoint();
            private Vector max = getMaximumPoint();
            private int nextX = min.getBlockX();
            private int nextY = min.getBlockY();
            private int nextZ = min.getBlockZ();

            @Override
            public boolean hasNext() {
                return (nextX != Integer.MIN_VALUE);
            }

            @Override
            public BlockVector next() {
                v.x = nextX;
                v.y = nextY;
                v.z = nextZ;
                if (++nextX > max.getBlockX()) {
                    nextX = min.getBlockX();
                    if (++nextY > max.getBlockY()) {
                        nextY = min.getBlockY();
                        if (++nextZ > max.getBlockZ()) {
                            nextX = Integer.MIN_VALUE;
                        }
                    }
                }
                return v;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }


    @Override
    public Iterable<Vector2D> asFlatRegion() {
        return new Iterable<Vector2D>() {
            @Override
            public Iterator<Vector2D> iterator() {
                return new Iterator<Vector2D>() {
                    private Vector min = getMinimumPoint();
                    private Vector max = getMaximumPoint();
                    private int nextX = min.getBlockX();
                    private int nextZ = min.getBlockZ();

                    @Override
                    public boolean hasNext() {
                        return (nextX != Integer.MIN_VALUE);
                    }

                    @Override
                    public Vector2D next() {
                        if (!hasNext()) throw new java.util.NoSuchElementException();
                        Vector2D answer = new Vector2D(nextX, nextZ);
                        if (++nextX > max.getBlockX()) {
                            nextX = min.getBlockX();
                            if (++nextZ > max.getBlockZ()) {
                                nextX = Integer.MIN_VALUE;
                            }
                        }
                        return answer;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @Override
    public String toString() {
        return getMinimumPoint() + " - " + getMaximumPoint();
    }

    @Override
    public CuboidRegion clone() {
        return (CuboidRegion) super.clone();
    }

    /**
     * Make a cuboid region out of the given region using the minimum and maximum
     * bounds of the provided region.
     *
     * @param region the region
     * @return a new cuboid region
     */
    public static CuboidRegion makeCuboid(Region region) {
        checkNotNull(region);
        return new CuboidRegion(region.getMinimumPoint(), region.getMaximumPoint());
    }

    /**
     * Make a cuboid from the center.
     *
     * @param origin the origin
     * @param apothem the apothem, where 0 is the minimum value to make a 1x1 cuboid
     * @return a cuboid region
     */
    public static CuboidRegion fromCenter(Vector origin, int apothem) {
        checkNotNull(origin);
        checkArgument(apothem >= 0, "apothem => 0 required");
        Vector size = new Vector(1, 1, 1).multiply(apothem);
        return new CuboidRegion(origin.subtract(size), origin.add(size));
    }

    public static Class<?> inject() {
        return CuboidRegion.class;
    }
}