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

/**
 * 1.21 entry of a weighted random list.
 */
public interface WeightedEntry {
	Weight getWeight();

	static <T> Wrapper<T> wrap(T data, int weight) {
		return new Wrapper<>(data, Weight.of(weight));
	}

	final class Wrapper<T> implements WeightedEntry {
		private final T data;
		private final Weight weight;

		public Wrapper(T data, Weight weight) {
			this.data = data;
			this.weight = weight;
		}

		public T data() {
			return this.data;
		}

		public Weight weight() {
			return this.weight;
		}

		@Override
		public Weight getWeight() {
			return this.weight;
		}
	}
}
