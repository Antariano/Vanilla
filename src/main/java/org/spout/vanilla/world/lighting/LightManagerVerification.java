/*
 * This file is part of Vanilla.
 *
 * Copyright (c) 2011-2012, Spout LLC <http://www.spout.org/>
 * Vanilla is licensed under the Spout License Version 1.
 *
 * Vanilla is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the Spout License Version 1.
 *
 * Vanilla is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the Spout License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://spout.in/licensev1> for the full license, including
 * the MIT license.
 */
package org.spout.vanilla.world.lighting;

import java.util.Collection;

import org.spout.api.Spout;
import org.spout.api.geo.LoadOption;
import org.spout.api.geo.World;
import org.spout.api.geo.cuboid.Chunk;
import org.spout.api.geo.cuboid.Region;
import org.spout.api.material.BlockMaterial;
import org.spout.api.material.block.BlockFace;
import org.spout.api.material.block.BlockFaces;
import org.spout.api.math.IntVector3;
import org.spout.api.math.Vector3;
import org.spout.api.util.bytebit.ByteBitSet;
import org.spout.api.util.cuboid.CuboidBlockMaterialBuffer;

public class LightManagerVerification {
	
	private final static BlockFace[] allFaces = BlockFaces.NESWBT.toArray();
	
	public static void checkAll(World w) {
		Collection<Region> regions = w.getRegions();
		for (Region r : regions) {
			checkRegion(r);
		}
	}
	
	private static void checkRegion(Region r) {
		for (int x = 0; x < Region.CHUNKS.SIZE; x++) {
			for (int y = 0; y < Region.CHUNKS.SIZE; y++) {
				for (int z = 0; z < Region.CHUNKS.SIZE; z++) {
					Chunk c = r.getChunk(x, y, z, LoadOption.NO_LOAD);
					if (c != null) {
						checkChunk(c);
					}
				}
			}
		}
	}
	
	public static void checkChunk(Chunk c) {
		Spout.getLogger().info("Testing skylight for chunk at " + c.getBase().toBlockString());
		checkChunk(c, VanillaLighting.SKY_LIGHT);
		Spout.getLogger().info("Testing blocklight for chunk at " + c.getBase().toBlockString());
		checkChunk(c, VanillaLighting.BLOCK_LIGHT);
	}
	
	private static void checkChunk(Chunk c, VanillaLightingManager manager) {
		final VanillaCuboidLightBuffer[][][] lightBuffers = getLightBuffers(c, manager);
		final CuboidBlockMaterialBuffer[][][] materialBuffers = getBlockMaterialBuffers(c);
		final int[][] height = getHeightMap(c);
		final int bx = c.getBlockX();
		final int by = c.getBlockY();
		final int bz = c.getBlockZ();
		LightGenerator lightSource = manager == VanillaLighting.SKY_LIGHT ?
				new LightGenerator() {
					@Override
					public int getEmittedLight(int x, int y, int z) {
						if (y + by > height[x][z]) {
							return 15;
						} else {
							return 0;
						}
					}
				}:
				new LightGenerator() {
					@Override
					public int getEmittedLight(int x, int y, int z) {
						CuboidBlockMaterialBuffer b = materialBuffers[1][1][1];
						if (b == null) {
							return 0;
						}
						BlockMaterial m = b.get(bx + x, by + y, bz + z);
						short data = b.getData(bx + x, by + y, bz + z);
						return m.getLightLevel(data);
					}
				};
		for (int x = 0; x < Chunk.BLOCKS.SIZE; x++) {
			for (int y = 0; y < Chunk.BLOCKS.SIZE; y++) {
				for (int z = 0; z < Chunk.BLOCKS.SIZE; z++) {
					testLight(x, y, z, materialBuffers, lightBuffers, lightSource);
				}
			}
		}
	}
	
	private static VanillaCuboidLightBuffer[][][] getLightBuffers(Chunk c, VanillaLightingManager manager) {
		VanillaCuboidLightBuffer[][][] buffers = new VanillaCuboidLightBuffer[3][3][3];
		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) {
				for (int z = 0; z < 3; z++) {
					buffers[x][y][z] = getLightBuffer(c.getWorld(), c.getX() - 1 + x, c.getY() - 1 + y, c.getZ() - 1 + z, manager.getId());
				}
			}
		}
		return buffers;
	}
	
	private static VanillaCuboidLightBuffer getLightBuffer(World w, int cx, int cy, int cz, short id) {
		Chunk c = w.getChunk(cx, cy, cz, LoadOption.NO_LOAD);
		if (c == null) {
			return null;
		}
		return (VanillaCuboidLightBuffer) c.getLightBuffer(id);
	}
	
	private static CuboidBlockMaterialBuffer[][][] getBlockMaterialBuffers(Chunk c) {
		CuboidBlockMaterialBuffer[][][] buffers = new CuboidBlockMaterialBuffer[3][3][3];
		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) {
				for (int z = 0; z < 3; z++) {
					buffers[x][y][z] = getBlockMaterialBuffer(c.getWorld(), c.getX() - 1 + x, c.getY() - 1 + y, c.getZ() - 1 + z);
				}
			}
		}
		return buffers;
	}
	
	private static CuboidBlockMaterialBuffer getBlockMaterialBuffer(World w, int cx, int cy, int cz) {
		Chunk c = w.getChunk(cx, cy, cz, LoadOption.NO_LOAD);
		if (c == null) {
			return null;
		}
		return (CuboidBlockMaterialBuffer) c.getCuboid(false);
	}
	
	private static int[][] getHeightMap(Chunk c) {
		int[][] heights = new int[Chunk.BLOCKS.SIZE][Chunk.BLOCKS.SIZE];
		for (int x = 0; x < heights.length; x++) {
			for (int z = 0; z < heights.length; z++) {
				heights[x][z] = c.getWorld().getSurfaceHeight(c.getBlockX() + x, c.getBlockZ() + z);
			}
		}
		return heights;
	}
	
	public static void testLight(int x, int y, int z, CuboidBlockMaterialBuffer[][][] materialBuffers, VanillaCuboidLightBuffer[][][] lightBuffers, LightGenerator lightSource) {
		int emitted = lightSource.getEmittedLight(x, y, z);
		BlockMaterial[][][] materials = getNeighborMaterials(x, y, z, materialBuffers);
		int[][][] lightLevels = getNeighborLight(x, y, z, lightBuffers);
		if (emitted > lightLevels[1][1][1]) {
			log("Light below emit level " + emitted + " > " + lightLevels[1][1][1], x, y, z, materialBuffers, lightBuffers, materials, lightLevels);
		}
		int inward = checkFlowFrom(materials, lightLevels);
		if (inward > lightLevels[1][1][1]) {
			log("Light below support level " + inward + " > " + lightLevels[1][1][1], x, y, z, materialBuffers, lightBuffers, materials, lightLevels);
		}
		if (inward < lightLevels[1][1][1] && emitted != lightLevels[1][1][1]) {
			log("Light above support level " + inward + " < " + lightLevels[1][1][1] + " and not equal to emitted level " + emitted, x, y, z, materialBuffers, lightBuffers, materials, lightLevels);
		}
	}
	
	public static int checkFlowFrom(BlockMaterial[][][] materials, int[][][] lightLevels) {
		int bestInward = 0;
		for (BlockFace face : allFaces) {
			int flowFrom = checkFlowFrom(face, materials, lightLevels);
			bestInward = Math.max(bestInward, flowFrom);
		}
		return bestInward;
	}
	
	public static int checkFlowFrom(BlockFace face, BlockMaterial[][][] materials, int[][][] lightLevels) {
		IntVector3 o = face.getIntOffset();
		int ox = o.getX() + 1;
		int oy = o.getY() + 1;
		int oz = o.getZ() + 1;
		BlockMaterial neighbor = materials[ox][oy][oz];
		if (neighbor == null) {
			return 0;
		}
		BlockMaterial center = materials[1][1][1];
		ByteBitSet occulusion = center.getOcclusion(center.getData());
		if (occulusion.get(face)) {
			return 0;
		}
		return lightLevels[ox][oy][oz] - 1 - center.getOpacity();
	}
	
	private static BlockMaterial[][][] getNeighborMaterials(int x, int y, int z, CuboidBlockMaterialBuffer[][][] buffers) {
		BlockMaterial[][][] materials = new BlockMaterial[3][3][3];
		for (int xx = x - 1; xx <= x + 1; xx++) {
			for (int yy = y - 1; yy <= y + 1; yy++) {
				for (int zz = z - 1; zz <= z + 1; zz++) {
					materials[xx + 1 - x][yy + 1 - y][zz + 1 - z] = getMaterial(xx, yy, zz, buffers);
				}
			}
		}
		return materials;
	}
	
	private static BlockMaterial getMaterial(int x, int y, int z, CuboidBlockMaterialBuffer[][][] buffers) {
		int xi = 1;
		int yi = 1;
		int zi = 1;

		if (x < 0) {
			xi--;
			x += Chunk.BLOCKS.SIZE;
		}
		if (x >= Chunk.BLOCKS.SIZE) {
			xi++;
			x -= Chunk.BLOCKS.SIZE;
		}
		if (y < 0) {
			yi--;
			y += Chunk.BLOCKS.SIZE;
		}
		if (y >= Chunk.BLOCKS.SIZE) {
			yi++;
			y -= Chunk.BLOCKS.SIZE;
		}
		if (z < 0) {
			zi--;
			z += Chunk.BLOCKS.SIZE;
		}
		if (z >= Chunk.BLOCKS.SIZE) {
			zi++;
			z -= Chunk.BLOCKS.SIZE;
		}
		CuboidBlockMaterialBuffer b = buffers[xi][yi][zi];
		if (buffers[xi][yi][zi] == null) {
			return null;
		}
		Vector3 base = b.getBase();
		return b.get(x + base.getFloorX(), y + base.getFloorY(), z + base.getFloorZ());
	}
	
	private static int[][][] getNeighborLight(int x, int y, int z, VanillaCuboidLightBuffer[][][] buffers) {
		int[][][] light = new int[3][3][3];
		for (int xx = x - 1; xx <= x + 1; xx++) {
			for (int yy = y - 1; yy <= y + 1; yy++) {
				for (int zz = z - 1; zz <= z + 1; zz++) {
					light[xx + 1 - x][yy + 1 - y][zz + 1 - z] = getLight(xx, yy, zz, buffers);
				}
			}
		}
		return light;
	}
	
	private static int getLight(int x, int y, int z, VanillaCuboidLightBuffer[][][] buffers) {
		int xi = 1;
		int yi = 1;
		int zi = 1;
		if (x < 0) {
			xi--;
			x += Chunk.BLOCKS.SIZE;
		}
		if (x >= Chunk.BLOCKS.SIZE) {
			xi++;
			x -= Chunk.BLOCKS.SIZE;
		}
		if (y < 0) {
			yi--;
			y += Chunk.BLOCKS.SIZE;
		}
		if (y >= Chunk.BLOCKS.SIZE) {
			yi++;
			y -= Chunk.BLOCKS.SIZE;
		}
		if (z < 0) {
			zi--;
			z += Chunk.BLOCKS.SIZE;
		}
		if (z >= Chunk.BLOCKS.SIZE) {
			zi++;
			z -= Chunk.BLOCKS.SIZE;
		}
		VanillaCuboidLightBuffer b = buffers[1][1][1];
		return buffers[xi][yi][zi].get(x + b.getBase().getFloorX(), y + b.getBase().getFloorY(), z + b.getBase().getFloorZ()) & 0xFF;
	}
	
	private static interface LightGenerator {
		public int getEmittedLight(int x, int y, int z);
	}
	
	private static void log(String message, int x, int y, int z, CuboidBlockMaterialBuffer[][][] material, VanillaCuboidLightBuffer[][][] light, BlockMaterial[][][] localMaterials, int[][][] localLight) {
		Vector3 base = material[1][1][1].getBase();
		x += base.getFloorX();
		y += base.getFloorY();
		z += base.getFloorZ();
		Spout.getLogger().info(message + " at " + x + ", " + y + ", " + z);
	}
}
