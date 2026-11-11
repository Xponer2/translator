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
package io.github.xponer2.translator.registry;

/**
 * 1.21 has two {@code getOrThrow(key)} on a registry: {@code Registry.getOrThrow} gives the value, {@code
 * HolderGetter.getOrThrow} its holder. The Translator's Registry is both and has the holder one; a mod's call of the
 * value one (its descriptor returns Object) comes here (JourneyMap cast the holder to a Biome: every chunk of its map
 * failed).
 */
public final class RegistryValues {
	private RegistryValues() {
	}

	/** 1.21 {@code Registry.getOrThrow(key)}: the value registered under the key. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static Object getOrThrow(Registry registry, ResourceKey key) {
		return registry.getOrThrow(key).value();
	}
}
