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

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

/**
 * The biomes of a mod's dimension: patches of its biomes, each a cell of a jittered grid (about 200 blocks across),
 * the biome of a cell picked from its seed. The 1.21 noise parameters of the biomes aren't followed.
 */
final class DataBiomeProvider extends BiomeProvider {
	private static final int CELL = 192;

	private final long seed;
	private final Biome[] biomes;
	/** The biome within {@link #innerRadius} of the centre (the End's main island), or null. */
	@Nullable
	private final Biome inner;
	private final long innerRadius;

	DataBiomeProvider(long seed, List<Biome> biomes) {
		this(seed, biomes, null, 0);
	}

	DataBiomeProvider(long seed, List<Biome> biomes, @Nullable Biome inner, int innerRadius) {
		this.seed = seed;
		this.biomes = biomes.toArray(new Biome[0]);
		this.inner = inner;
		this.innerRadius = innerRadius;
	}

	/** The biome at a block: the one of the nearest cell centre. */
	private Biome at(int x, int z) {
		if (this.inner != null && (long) x * x + (long) z * z < this.innerRadius * this.innerRadius) {
			return this.inner;
		}
		int cellX = Math.floorDiv(x, CELL);
		int cellZ = Math.floorDiv(z, CELL);
		long best = Long.MAX_VALUE;
		int pick = 0;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				long hash = hash(cellX + dx, cellZ + dz);
				int centerX = (cellX + dx) * CELL + (int) ((hash >>> 8) % CELL);
				int centerZ = (cellZ + dz) * CELL + (int) ((hash >>> 24) % CELL);
				long distance = (long) (x - centerX) * (x - centerX) + (long) (z - centerZ) * (z - centerZ);
				if (distance < best) {
					best = distance;
					pick = (int) ((hash >>> 40) % this.biomes.length);
				}
			}
		}
		return this.biomes[pick];
	}

	private long hash(int x, int z) {
		long h = this.seed ^ (x * 341873128712L) ^ (z * 132897987541L);
		h ^= h >>> 33;
		h *= 0xff51afd7ed558ccdL;
		h ^= h >>> 33;
		h *= 0xc4ceb9fe1a85ec53L;
		h ^= h >>> 33;
		return h & Long.MAX_VALUE;
	}

	@Override
	public List<Biome> getBiomesToSpawnIn() {
		return java.util.Arrays.asList(this.biomes);
	}

	@Override
	public Biome getBiome(BlockPos pos) {
		return this.at(pos.getX(), pos.getZ());
	}

	@Override
	public Biome getBiome(BlockPos pos, Biome defaultBiome) {
		return this.at(pos.getX(), pos.getZ());
	}

	/** At quarter resolution (4 blocks), as the terrain generator asks. */
	@Override
	public Biome[] getBiomesForGeneration(@Nullable Biome[] biomes, int x, int z, int width, int height) {
		if (biomes == null || biomes.length < width * height) {
			biomes = new Biome[width * height];
		}
		for (int dz = 0; dz < height; dz++) {
			for (int dx = 0; dx < width; dx++) {
				biomes[dz * width + dx] = this.at((x + dx) * 4, (z + dz) * 4);
			}
		}
		return biomes;
	}

	@Override
	public Biome[] getBiomes(@Nullable Biome[] biomes, int x, int z, int width, int depth) {
		return this.getBiomes(biomes, x, z, width, depth, true);
	}

	@Override
	public Biome[] getBiomes(@Nullable Biome[] biomes, int x, int z, int width, int depth, boolean cache) {
		if (biomes == null || biomes.length < width * depth) {
			biomes = new Biome[width * depth];
		}
		for (int dz = 0; dz < depth; dz++) {
			for (int dx = 0; dx < width; dx++) {
				biomes[dz * width + dx] = this.at(x + dx, z + dz);
			}
		}
		return biomes;
	}

	@Override
	public boolean areBiomesViable(int x, int z, int radius, List<Biome> allowed) {
		for (int dx = -radius; dx <= radius; dx += 4) {
			for (int dz = -radius; dz <= radius; dz += 4) {
				if (!allowed.contains(this.at(x + dx, z + dz))) {
					return false;
				}
			}
		}
		return true;
	}

	@Nullable
	@Override
	public BlockPos findBiomePosition(int x, int z, int range, List<Biome> allowed, Random random) {
		BlockPos found = null;
		int seen = 0;
		for (int dx = -range; dx <= range; dx += 4) {
			for (int dz = -range; dz <= range; dz += 4) {
				if (allowed.contains(this.at(x + dx, z + dz)) && (found == null || random.nextInt(++seen) == 0)) {
					found = new BlockPos(x + dx, 0, z + dz);
				}
			}
		}
		return found;
	}

	@Override
	public boolean isFixedBiome() {
		return this.biomes.length == 1;
	}

	@Override
	public Biome getFixedBiome() {
		return this.biomes[0];
	}
}
