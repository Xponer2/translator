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
 * 1.21 {@code XoroshiroRandomSource} as a java.util.Random (the numbers differ from 1.21's xoroshiro128++, the uses are
 * the same: a seeded random of a mod's world generation).
 */
public class XoroshiroRandomSource extends Random {
	public XoroshiroRandomSource(long seed) {
		super(seed);
	}

	public XoroshiroRandomSource(long seedLo, long seedHi) {
		super(seedLo ^ Long.rotateLeft(seedHi, 32));
	}

	public Random fork() {
		return new XoroshiroRandomSource(this.nextLong(), this.nextLong());
	}

	public void consumeCount(int count) {
		for (int i = 0; i < count; i++) {
			this.nextLong();
		}
	}

	public long nextBits(int bits) {
		return this.nextLong() >>> (64 - bits);
	}
}
