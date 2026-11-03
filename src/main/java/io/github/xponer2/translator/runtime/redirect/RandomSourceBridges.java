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
package io.github.xponer2.translator.runtime.redirect;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The 1.21 {@code RandomSource} methods {@link Random} lacks (RandomSource is java.util.Random here): its factories
 * (Mining Gadgets renders with {@code RandomSource.create(42)}) and its helpers for ranges.
 */
public final class RandomSourceBridges {
	private RandomSourceBridges() {
	}

	public static Random create() {
		return new Random();
	}

	public static Random create(long seed) {
		return new Random(seed);
	}

	public static Random createNewThreadLocalInstance() {
		return new Random(ThreadLocalRandom.current().nextLong());
	}

	/** java.util.Random is thread safe already. */
	public static Random createThreadSafe() {
		return new Random();
	}

	public static Random fork(Random random) {
		return new Random(random.nextLong());
	}

	public static int nextInt(Random random, int origin, int bound) {
		if (origin >= bound) {
			throw new IllegalArgumentException("bound - origin is non positive");
		}
		return origin + random.nextInt(bound - origin);
	}

	public static int nextIntBetweenInclusive(Random random, int min, int max) {
		return random.nextInt(max - min + 1) + min;
	}

	public static double triangle(Random random, double mode, double deviation) {
		return mode + deviation * (random.nextDouble() - random.nextDouble());
	}

	public static void consumeCount(Random random, int count) {
		for (int i = 0; i < count; i++) {
			random.nextInt();
		}
	}
}
