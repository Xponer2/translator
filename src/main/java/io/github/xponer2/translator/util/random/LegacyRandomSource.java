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

/**
 * 1.21 {@code LegacyRandomSource}: the Java random's generator, which it is here (RandomSource is java.util.Random in
 * the tables). The carried class implemented java.util.Random as an interface and was no Random at all: every
 * structure and feature of a mod that seeded its own random (Biomes O' Plenty, Twilight Forest) failed to verify.
 */
public class LegacyRandomSource extends Random {
	public LegacyRandomSource(long seed) {
		super(seed);
	}

	/** 1.21 {@code fork()}: a new random seeded from this one. */
	public Random fork() {
		return new LegacyRandomSource(this.nextLong());
	}

	/** 1.21 {@code next(bits)} is public (WorldgenRandom draws its bits from it). */
	@Override
	public int next(int bits) {
		return super.next(bits);
	}
}
