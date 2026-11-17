/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.client;

import java.util.function.Predicate;

/**
 * NeoForge {@code ModelProperty}: a key of the model data a block entity gives its model (AE2's cables, Create's
 * casings, Storage Drawers), with the values it accepts.
 */
public class ModelProperty<T> implements Predicate<T> {
	private final Predicate<T> predicate;

	public ModelProperty() {
		this(value -> true);
	}

	public ModelProperty(Predicate<T> predicate) {
		this.predicate = predicate;
	}

	@Override
	public boolean test(T value) {
		return this.predicate.test(value);
	}
}
