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
