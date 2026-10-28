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
