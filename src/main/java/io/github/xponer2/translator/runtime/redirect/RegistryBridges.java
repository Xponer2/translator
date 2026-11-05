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
package io.github.xponer2.translator.runtime.redirect;

import java.util.Optional;

import io.github.xponer2.translator.registry.Registry;
import io.github.xponer2.translator.registry.ResourceKey;
import io.github.xponer2.translator.util.TagKey;

/**
 * The 1.21 {@code HolderGetter} calls a registry here lacks (a HolderGetter is a {@link Registry} in the tables): its
 * {@code get} by key gives a holder, not the object. The 1.21 codecs that read a registry entry by its name
 * ({@code RegistryFileCodec}: a placed feature's configured feature) and a tag ({@code HolderSetCodec}) call them.
 */
public final class RegistryBridges {
	private RegistryBridges() {
	}

	/** 1.21 {@code HolderGetter.get(key)}: the holder of the entry, empty when there is none. */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Optional<?> get(Registry registry, ResourceKey key) {
		return registry.getHolder(key);
	}

	/**
	 * 1.21 {@code HolderGetter.get(tag)}: the tag's named set. Like a registry lookup while data loads, a tag without
	 * members (yet) is a set too, so the data that names it still reads.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Optional<Object> get(Registry registry, TagKey tag) {
		return Optional.ofNullable(registry.getOrCreateTag(tag));
	}
}
