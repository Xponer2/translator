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

import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.BlockFalling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.NoiseGeneratorSimplex;

/**
 * The terrain of a 1.21.1 mod's dimension: the 1.12.2 overworld terrain of its biomes (their heights), with the
 * dimension's own stone; floating islands for a sky dimension (the Aether); nothing for a void one. Its biomes decorate
 * it (their grass, flowers and trees).
 */
final class DataChunkGenerator implements IChunkGenerator {
	private final World world;
	@Nullable
	private final DataDimensions.Data data;
	@Nullable
	private final ChunkGeneratorOverworld overworld;
	private final NoiseGeneratorSimplex shape;
	private final NoiseGeneratorSimplex detail;

	DataChunkGenerator(World world, @Nullable DataDimensions.Data data) {
		this.world = world;
		this.data = data;
		DataDimensions.Data.Terrain terrain = data != null ? data.terrain : DataDimensions.Data.Terrain.OVERWORLD;
		this.overworld = terrain == DataDimensions.Data.Terrain.OVERWORLD ? new ChunkGeneratorOverworld(world, world.getSeed(), false, "") : null;
		this.shape = new NoiseGeneratorSimplex(new Random(world.getSeed()));
		this.detail = new NoiseGeneratorSimplex(new Random(world.getSeed() + 1));
	}

	private IBlockState stone() {
		return this.data != null && this.data.stone != null ? this.data.stone : Blocks.STONE.getDefaultState();
	}

	@Override
	public Chunk generateChunk(int x, int z) {
		if (this.overworld != null) {
			Chunk chunk = this.overworld.generateChunk(x, z);
			if (this.data != null && this.data.stone != null) {
				this.replaceStone(chunk);
			}
			return chunk;
		}
		var primer = new ChunkPrimer();
		Biome[] biomes = this.world.getBiomeProvider().getBiomes(null, x * 16, z * 16, 16, 16);
		if (this.data != null && this.data.terrain == DataDimensions.Data.Terrain.ISLANDS) {
			this.islands(primer, x, z, biomes);
		}
		var chunk = new Chunk(this.world, primer, x, z);
		byte[] ids = chunk.getBiomeArray();
		for (int i = 0; i < ids.length && i < biomes.length; i++) {
			ids[i] = (byte) Biome.getIdForBiome(biomes[i]);
		}
		chunk.generateSkylightMap();
		return chunk;
	}

	/** Lens-shaped islands where a broad noise is high, their top at about y 96 to 120, the biome's ground on top. */
	private void islands(ChunkPrimer primer, int chunkX, int chunkZ, Biome[] biomes) {
		IBlockState stone = this.stone();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				double wx = chunkX * 16 + x;
				double wz = chunkZ * 16 + z;
				double n = this.shape.getValue(wx / 160.0, wz / 160.0) + 0.35 * this.detail.getValue(wx / 40.0, wz / 40.0);
				if (n < 0.25) {
					continue;
				}
				double strength = n - 0.25;
				int top = 96 + (int) (strength * 24) + (int) (4 * this.detail.getValue(wx / 80.0 + 7, wz / 80.0 + 7));
				int bottom = top - 3 - (int) (strength * 48);
				Biome biome = biomes[z * 16 + x];
				for (int y = Math.max(1, bottom); y <= top && y < 255; y++) {
					IBlockState state = y == top ? biome.topBlock : y >= top - 3 ? biome.fillerBlock : stone;
					primer.setBlockState(x, y, z, state);
				}
			}
		}
	}

	private void replaceStone(Chunk chunk) {
		IBlockState plain = Blocks.STONE.getDefaultState();
		IBlockState stone = this.stone();
		for (ExtendedBlockStorage storage : chunk.getBlockStorageArray()) {
			if (storage == null || storage.isEmpty()) {
				continue;
			}
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						if (storage.get(x, y, z) == plain) {
							storage.set(x, y, z, stone);
						}
					}
				}
			}
		}
	}

	@Override
	public void populate(int x, int z) {
		if (this.overworld != null) {
			this.overworld.populate(x, z);
			return;
		}
		if (this.data == null || this.data.terrain == DataDimensions.Data.Terrain.VOID) {
			return;
		}
		BlockFalling.fallInstantly = true;
		try {
			var pos = new BlockPos(x * 16, 0, z * 16);
			Biome biome = this.world.getBiome(pos.add(16, 0, 16));
			var random = new Random(this.world.getSeed());
			long a = random.nextLong() / 2L * 2L + 1L;
			long b = random.nextLong() / 2L * 2L + 1L;
			random.setSeed(x * a + z * b ^ this.world.getSeed());
			biome.decorate(this.world, random, pos);
		} finally {
			BlockFalling.fallInstantly = false;
		}
	}

	@Override
	public boolean generateStructures(Chunk chunk, int x, int z) {
		return this.overworld != null && this.overworld.generateStructures(chunk, x, z);
	}

	@Override
	public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType type, BlockPos pos) {
		if (this.overworld != null) {
			return this.overworld.getPossibleCreatures(type, pos);
		}
		return this.data == null || this.data.terrain == DataDimensions.Data.Terrain.VOID ? Collections.emptyList()
				: this.world.getBiome(pos).getSpawnableList(type);
	}

	@Nullable
	@Override
	public BlockPos getNearestStructurePos(World world, String structureName, BlockPos position, boolean findUnexplored) {
		return null;
	}

	@Override
	public void recreateStructures(Chunk chunk, int x, int z) {
	}

	@Override
	public boolean isInsideStructure(World world, String structureName, BlockPos pos) {
		return false;
	}
}
