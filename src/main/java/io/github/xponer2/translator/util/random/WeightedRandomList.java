package io.github.xponer2.translator.util.random;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;

/**
 * 1.21 list of weighted entries with a weighted random pick.
 */
public final class WeightedRandomList<E extends WeightedEntry> {
	private final int totalWeight;
	private final List<E> items;

	private WeightedRandomList(List<? extends E> items) {
		this.items = ImmutableList.copyOf(items);
		int total = 0;
		for (var item : this.items) {
			total += item.getWeight().asInt();
		}
		this.totalWeight = total;
	}

	public static <E extends WeightedEntry> WeightedRandomList<E> create() {
		return new WeightedRandomList<>(Collections.emptyList());
	}

	@SafeVarargs
	public static <E extends WeightedEntry> WeightedRandomList<E> create(E... items) {
		return new WeightedRandomList<>(Arrays.asList(items));
	}

	public static <E extends WeightedEntry> WeightedRandomList<E> create(List<E> items) {
		return new WeightedRandomList<>(items);
	}

	public static <E extends WeightedEntry> Codec<WeightedRandomList<E>> codec(Codec<E> elementCodec) {
		return elementCodec.listOf().xmap(WeightedRandomList::create, WeightedRandomList::unwrap);
	}

	public boolean isEmpty() {
		return this.items.isEmpty();
	}

	public Optional<E> getRandom(Random random) {
		if (this.totalWeight == 0) {
			return Optional.empty();
		}

		int pick = random.nextInt(this.totalWeight);
		for (var item : this.items) {
			pick -= item.getWeight().asInt();
			if (pick < 0) {
				return Optional.of(item);
			}
		}

		return Optional.empty();
	}

	public List<E> unwrap() {
		return this.items;
	}
}
