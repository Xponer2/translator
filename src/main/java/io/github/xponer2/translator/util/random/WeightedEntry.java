/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
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

		/** The codec of a wrapper: {data, weight}. */
		public static <E> com.mojang.serialization.Codec<Wrapper<E>> codec(com.mojang.serialization.Codec<E> elementCodec) {
			return com.mojang.serialization.codecs.RecordCodecBuilder.create(instance -> instance.group(
					elementCodec.fieldOf("data").forGetter(Wrapper::data),
					Weight.CODEC.fieldOf("weight").forGetter(Wrapper::weight)).apply(instance, Wrapper::new));
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
