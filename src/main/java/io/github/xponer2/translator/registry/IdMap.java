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

import javax.annotation.Nullable;

import net.minecraft.util.ObjectIntIdentityMap;

/**
 * 1.21 {@code IdMap}: values by their numeric ids. 1.21's registries are id maps, and so are its id mappers (the block
 * states' ids); here the Translator's registries extend it, and 1.12.2's ObjectIntIdentityMap (the block states' ids,
 * the mods' IdMappers) implements it through the AccessWidener. It was 1.12.2's ObjectIntIdentityMap, a class: a
 * registry passed as an id map didn't verify (resourcefullib's byte codecs of items and fluids: the whole class didn't
 * load). An interface takes both.
 */
public interface IdMap<T> extends Iterable<T> {
	/** 1.21 {@code IdMap.DEFAULT}: the id of a value that has none. */
	int DEFAULT = -1;

	/** The id of a value, {@link #DEFAULT} when it has none. */
	@SuppressWarnings("unchecked")
	default int getId(@Nullable T value) {
		if ((Object) this instanceof ObjectIntIdentityMap) {
			return ((ObjectIntIdentityMap<T>) (Object) this).get(value);
		}
		int id = 0;
		for (T entry : this) {
			if (entry == value) {
				return id;
			}
			id++;
		}
		return DEFAULT;
	}

	/** The value of an id, null when there is none. */
	@Nullable
	@SuppressWarnings("unchecked")
	default T byId(int id) {
		if ((Object) this instanceof ObjectIntIdentityMap) {
			return ((ObjectIntIdentityMap<T>) (Object) this).getByValue(id);
		}
		int index = 0;
		for (T entry : this) {
			if (index++ == id) {
				return entry;
			}
		}
		return null;
	}

	default int getIdOrThrow(T value) {
		int id = this.getId(value);
		if (id == DEFAULT) {
			throw new IllegalArgumentException("Can't find id for '" + value + "' in map " + this);
		}
		return id;
	}

	default T byIdOrThrow(int id) {
		T value = this.byId(id);
		if (value == null) {
			throw new IllegalArgumentException("No value with id " + id);
		}
		return value;
	}

	default int size() {
		if ((Object) this instanceof ObjectIntIdentityMap) {
			return ((ObjectIntIdentityMap<?>) (Object) this).size();
		}
		int size = 0;
		for (T ignored : this) {
			size++;
		}
		return size;
	}
}
