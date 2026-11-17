/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.advancement;

import java.util.Optional;

/**
 * The 1.21 {@code MinMaxBounds} interface (neoforge-1.21.1.tsv). Its records {@code MinMaxBounds$Ints} and
 * {@code $Doubles} are carried from the 1.21.1 game and implement it. The 1.12.2 class of the same name is another
 * thing (a float range): with the 1.21 name mapped to it, the carried records weren't bounds, and every codec or
 * lambda that returns one failed (Twilight Forest's advancement triggers, so its whole mod didn't load). Its static
 * members (createCodec, fromReader, ERROR_EMPTY) come from the 1.21.1 class (the shadow).
 */
public interface MinMaxBounds<T extends Number> {
	Optional<T> min();

	Optional<T> max();

	default boolean isAny() {
		return !this.min().isPresent() && !this.max().isPresent();
	}

	default Optional<T> unwrapPoint() {
		Optional<T> min = this.min();
		Optional<T> max = this.max();
		return min.equals(max) ? min : Optional.empty();
	}
}
