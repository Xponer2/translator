/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package io.github.xponer2.translator.world;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

/**
 * A 1.21 {@code Heightmap} of a chunk, read from 1.12.2's: the height of each column by the kind of the heightmap
 * ({@code Heightmap.Types}: the world surface, the blocks that stop motion, without leaves, the ocean floor). A map mod
 * reads a chunk's surface so (JourneyMap's chunk snapshots: {@code getOrCreateHeightmapUnprimed(type)
 * .getFirstAvailable(x, z)}; there was none and no map tile was drawn).
 */
public final class ChunkHeightmap {
	private final Chunk chunk;
	private final String type;

	public ChunkHeightmap(Chunk chunk, Object type) {
		this.chunk = chunk;
		this.type = String.valueOf(type);
	}

	/** One above the highest block of the column this kind counts (the chunk's minimum when there is none). */
	public int getFirstAvailable(int x, int z) {
		int top = this.chunk.getHeightValue(x & 15, z & 15);
		boolean oceanFloor = this.type.contains("OCEAN_FLOOR");
		boolean noLeaves = this.type.contains("NO_LEAVES");
		if (!oceanFloor && !noLeaves) {
			return top;
		}
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int y = top - 1; y >= 0; y--) {
			IBlockState state = this.chunk.getBlockState(pos.setPos(x & 15, y, z & 15));
			Material material = state.getMaterial();
			if (material == Material.AIR || noLeaves && material == Material.LEAVES || oceanFloor && material.isLiquid()) {
				continue;
			}
			if (oceanFloor && !material.blocksMovement()) {
				continue;
			}
			return y + 1;
		}
		return 0;
	}

	/** The highest block of the column this kind counts. */
	public int getHighestTaken(int x, int z) {
		return this.getFirstAvailable(x, z) - 1;
	}

	/** 1.21 {@code getFirstAvailable(index)} (index = x + z * 16). */
	public int getFirstAvailable(int index) {
		return this.getFirstAvailable(index & 15, index >> 4 & 15);
	}
}
