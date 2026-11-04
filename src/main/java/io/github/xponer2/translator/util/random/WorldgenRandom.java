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
package io.github.xponer2.translator.util.random;

import java.util.Random;
import java.util.function.LongFunction;

/**
 * 1.21 {@code WorldgenRandom}: the random of world generation with its seeding for decorations, features and
 * structures (the same arithmetic as 1.21), its bits drawn from the random it wraps.
 */
public class WorldgenRandom extends LegacyRandomSource {
	private final Random randomSource;
	private int count;

	public WorldgenRandom(Random randomSource) {
		super(0L);
		this.randomSource = randomSource;
	}

	public int getCount() {
		return this.count;
	}

	@Override
	public Random fork() {
		return new LegacyRandomSource(this.nextLong());
	}

	@Override
	public int next(int bits) {
		Random source = this.randomSource;
		if (source == null) {
			// java.util.Random's constructor, before the source is set
			return super.next(bits);
		}
		this.count++;
		if (source instanceof LegacyRandomSource legacy) {
			return legacy.next(bits);
		}
		return (int) (source.nextLong() >>> (64 - bits));
	}

	@Override
	public synchronized void setSeed(long seed) {
		if (this.randomSource == null) {
			super.setSeed(seed);
		} else {
			this.randomSource.setSeed(seed);
		}
	}

	public long setDecorationSeed(long levelSeed, int minChunkBlockX, int minChunkBlockZ) {
		this.setSeed(levelSeed);
		long a = this.nextLong() | 1L;
		long b = this.nextLong() | 1L;
		long seed = (long) minChunkBlockX * a + (long) minChunkBlockZ * b ^ levelSeed;
		this.setSeed(seed);
		return seed;
	}

	public void setFeatureSeed(long decorationSeed, int index, int decorationStep) {
		this.setSeed(decorationSeed + (long) index + (long) (10000 * decorationStep));
	}

	public void setLargeFeatureSeed(long baseSeed, int chunkX, int chunkZ) {
		this.setSeed(baseSeed);
		long a = this.nextLong();
		long b = this.nextLong();
		this.setSeed((long) chunkX * a ^ (long) chunkZ * b ^ baseSeed);
	}

	public void setLargeFeatureWithSalt(long levelSeed, int regionX, int regionZ, int salt) {
		this.setSeed((long) regionX * 341873128712L + (long) regionZ * 132897987541L + levelSeed + (long) salt);
	}

	public static Random seedSlimeChunk(int chunkX, int chunkZ, long levelSeed, long salt) {
		return new LegacyRandomSource(levelSeed + (long) (chunkX * chunkX * 4987142) + (long) (chunkX * 5947611) + (long) (chunkZ * chunkZ) * 4392871L
				+ (long) (chunkZ * 389711) ^ salt);
	}

	/** 1.21 {@code WorldgenRandom.Algorithm}: which random a seed makes. */
	public enum Algorithm {
		LEGACY(LegacyRandomSource::new),
		XOROSHIRO(XoroshiroRandomSource::new);

		private final LongFunction<Random> constructor;

		Algorithm(LongFunction<Random> constructor) {
			this.constructor = constructor;
		}

		public Random newInstance(long seed) {
			return this.constructor.apply(seed);
		}
	}
}
