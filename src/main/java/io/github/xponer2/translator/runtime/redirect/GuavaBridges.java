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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Newer Guava (23+) {@code builderWithExpectedSize} on the game's Guava 21: a plain builder (the size is only a hint).
 * Its carried code builds one with Guava 21's package-private {@code Builder(int)}, which a class in another package
 * can't call: Refurbished Furniture's grill and stove block entities failed with IllegalAccessError. One class per
 * Guava class (the helpers differ only in what they return).
 */
public final class GuavaBridges {
	private GuavaBridges() {
	}

	public static <E> ImmutableList.Builder<E> builderWithExpectedSize(int expectedSize) {
		return ImmutableList.builder();
	}

	/** {@code ImmutableSet.builderWithExpectedSize}. */
	public static final class Sets {
		private Sets() {
		}

		public static <E> ImmutableSet.Builder<E> builderWithExpectedSize(int expectedSize) {
			return ImmutableSet.builder();
		}
	}

	/** {@code ImmutableMap.builderWithExpectedSize}. */
	public static final class Maps {
		private Maps() {
		}

		public static <K, V> ImmutableMap.Builder<K, V> builderWithExpectedSize(int expectedSize) {
			return ImmutableMap.builder();
		}
	}

	/** Newer {@code ImmutableMap.Builder} methods. */
	public static final class MapBuilders {
		private static final java.lang.reflect.Field ENTRIES = field("entries");
		private static final java.lang.reflect.Field SIZE = field("size");

		private MapBuilders() {
		}

		@javax.annotation.Nullable
		private static java.lang.reflect.Field field(String name) {
			try {
				java.lang.reflect.Field field = ImmutableMap.Builder.class.getDeclaredField(name);
				field.setAccessible(true);
				return field;
			} catch (ReflectiveOperationException | RuntimeException e) {
				return null;
			}
		}

		/**
		 * Guava 31's {@code builder.buildKeepingLast()}: the map, a key put twice keeping its last value (Guava 21's
		 * build() throws on it). Curios' entity slots were built so: its server data came out null.
		 */
		@SuppressWarnings("unchecked")
		public static <K, V> ImmutableMap<K, V> buildKeepingLast(ImmutableMap.Builder<K, V> builder) {
			if (ENTRIES == null || SIZE == null) {
				return builder.build();
			}
			try {
				Object[] entries = (Object[]) ENTRIES.get(builder);
				int size = SIZE.getInt(builder);
				java.util.Map<K, V> map = new java.util.LinkedHashMap<>();
				for (int i = 0; i < size; i++) {
					java.util.Map.Entry<K, V> entry = (java.util.Map.Entry<K, V>) entries[i];
					map.put(entry.getKey(), entry.getValue());
				}
				return ImmutableMap.copyOf(map);
			} catch (ReflectiveOperationException | RuntimeException e) {
				return builder.build();
			}
		}

		/** Guava 31's {@code builder.buildOrThrow()}: Guava 21's build(). */
		public static <K, V> ImmutableMap<K, V> buildOrThrow(ImmutableMap.Builder<K, V> builder) {
			return builder.build();
		}
	}
}
