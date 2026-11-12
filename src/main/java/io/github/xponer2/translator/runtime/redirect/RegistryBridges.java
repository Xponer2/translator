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
	public static Optional<?> get(Registry registry, ResourceKey key) {		return registry.getHolder(key);
	}

	/**
	 * 1.21 {@code HolderGetter.get(tag)}: the tag's named set. Like a registry lookup while data loads, a tag without
	 * members (yet) is a set too, so the data that names it still reads.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Optional<Object> get(Registry registry, TagKey tag) {
		return Optional.ofNullable(registry.getOrCreateTag(tag));
	}

	/**
	 * 1.21 {@code HolderGetter.getOrThrow(tag)}: the tag's entries as a 1.21 named holder set (a carried class, bound by
	 * its own {@code bind}); the vanilla tree features name block tags so (mangrove roots).
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Object getOrThrow(io.github.xponer2.translator.registry.Registry registry, io.github.xponer2.translator.util.TagKey tag) {
		var holders = new java.util.ArrayList<Object>();
		for (Object value : (Iterable<Object>) registry) {
			try {
				boolean in = value instanceof net.minecraft.block.Block block ? io.github.xponer2.translator.tags.TagManager.is(block, tag)
						: value instanceof net.minecraft.item.Item item && io.github.xponer2.translator.tags.TagManager.is(item, tag);
				if (in) {
					holders.add(io.github.xponer2.translator.registry.Holder.direct(value));
				}
			} catch (RuntimeException e) {
				// not of a kind tags are kept for
			}
		}
		try {
			Class<?> named = Class.forName("net.minecraft.core.HolderSet$Named", true, net.minecraft.launchwrapper.Launch.classLoader);
			for (java.lang.reflect.Constructor<?> constructor : named.getDeclaredConstructors()) {
				if (constructor.getParameterCount() == 2) {
					constructor.setAccessible(true);
					Object set = constructor.newInstance(null, tag);
					for (java.lang.reflect.Method method : named.getDeclaredMethods()) {
						if ("bind".equals(method.getName()) && method.getParameterCount() == 1) {
							method.setAccessible(true);
							method.invoke(set, holders);
						}
					}
					return set;
				}
			}
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("a holder set of the tag " + tag, e);
		}
		return null;
	}

	/** NeoForge {@code registry.getDataMap(type)}: the values the mods' data maps give the entries, by key. */
	public static java.util.Map<ResourceKey<?>, Object> getDataMap(Registry<?> registry, Object type) {
		return io.github.xponer2.translator.registry.DataMaps.byKey(registry, type);
	}

	/** NeoForge {@code registry.getData(type, key)}: the value the data map gives the entry, null when none. */
	@javax.annotation.Nullable
	public static Object getData(Registry<?> registry, Object type, ResourceKey<?> key) {
		return key == null ? null : io.github.xponer2.translator.registry.DataMaps.get(type, key.location());
	}

	/** 1.21 {@code registry.getHolder(id)}: the holder of the entry of that numeric id, empty when there is none. */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Optional<?> getHolder(Registry registry, int id) {
		Object value = registry.byId(id);
		if (value == null) {
			return Optional.empty();
		}
		Optional<ResourceKey> key = registry.getResourceKey(value);
		return key.isPresent() ? registry.getHolder(key.get()) : Optional.empty();
	}

	/** 1.21 {@code registry.getTags()}: each tag of the registry with its named set. */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static java.util.stream.Stream<?> getTags(Registry registry) {
		return ((java.util.stream.Stream<TagKey>) registry.getTagNames()).map(tag -> {
			Optional<Object> set = registry.getTag(tag);
			return set.isPresent() ? com.mojang.datafixers.util.Pair.of(tag, set.get()) : null;
		}).filter(java.util.Objects::nonNull);
	}
}
