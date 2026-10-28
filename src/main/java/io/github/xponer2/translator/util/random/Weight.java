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

import com.mojang.serialization.Codec;

/**
 * 1.21 weight of a weighted entry (a non-negative int).
 */
public final class Weight {
	public static final Codec<Weight> CODEC = Codec.INT.xmap(Weight::of, Weight::asInt);

	private final int value;

	private Weight(int value) {
		this.value = value;
	}

	public static Weight of(int value) {
		if (value < 0) {
			throw new IllegalArgumentException("Weight should be >= 0");
		}

		return new Weight(value);
	}

	public int asInt() {
		return this.value;
	}

	@Override
	public String toString() {
		return Integer.toString(this.value);
	}
}
