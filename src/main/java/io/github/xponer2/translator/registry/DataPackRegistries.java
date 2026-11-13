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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import io.github.xponer2.translator.recipe.ICondition;
import io.github.xponer2.translator.runtime.RuntimeMods;
import io.github.xponer2.translator.tags.TagManager;
import io.github.xponer2.translator.util.BuiltInRegistries;

import net.minecraft.util.ResourceLocation;

/**
 * The registries the 1.21.1 mods named in {@link DataPackRegistryEvent.NewRegistry}, filled from the files of the
 * mods' jars as 1.21's RegistryDataLoader does: {@code data/<namespace>/<dir>/<path>.json} is {@code namespace:path},
 * where dir is the registry's path for a minecraft registry and {@code <its namespace>/<its path>} otherwise. Read once,
 * after the mods' items and blocks are registered (the files name them). There are no world data packs on 1.12.2, so
 * the mods' own files are all there is.
 */
public final class DataPackRegistries {
	private static final Logger LOGGER = LogManager.getLogger("Translator Runtime");
	private static final Map<ResourceKey<?>, Codec<?>> REGISTRIES = new LinkedHashMap<>();
	private static boolean loaded;

	private DataPackRegistries() {
	}

	static synchronized <T> void add(ResourceKey<Registry<T>> key, Codec<T> codec) {
		REGISTRIES.putIfAbsent(key, codec);
		// the registry exists from now on, empty until the files are read
		BuiltInRegistries.byKey(key);
		if (loaded) {
			load(key, codec);
		}
	}

	/** NeoForge posts the event once every mod is constructed, after NewRegistryEvent. */
	public static void fire() {
		try {
			net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new DataPackRegistryEvent.NewRegistry());
		} catch (Throwable t) {
			LOGGER.error("DataPackRegistryEvent.NewRegistry failed", t);
		}
	}

	/** Reads the files of every registry named so far. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static synchronized void loadAll() {
		if (loaded) {
			return;
		}
		loaded = true;
		for (var entry : new ArrayList<>(REGISTRIES.entrySet())) {
			load((ResourceKey) entry.getKey(), (Codec) entry.getValue());
		}
	}

	private static String dir(ResourceLocation registry) {
		return "minecraft".equals(registry.getNamespace()) ? registry.getPath() : registry.getNamespace() + "/" + registry.getPath();
	}

	@SuppressWarnings("unchecked")
	private static <T> void load(ResourceKey<Registry<T>> key, Codec<T> codec) {
		String dir = "/" + dir(key.location()) + "/";
		// the files by id, in a stable order (a registry's ids keep the same order from game to game)
		Map<ResourceLocation, JsonElement> files = new TreeMap<>(java.util.Comparator.comparing(ResourceLocation::toString));
		RuntimeMods.forEachData((path, bytes) -> {
			int slash = path.indexOf('/');
			if (slash <= 0 || !path.endsWith(".json") || !path.startsWith(dir, slash)) {
				return;
			}
			String name = path.substring(slash + dir.length(), path.length() - ".json".length());
			ResourceLocation id;
			try {
				id = new ResourceLocation(path.substring(0, slash), name);
			} catch (RuntimeException e) {
				return;
			}
			try {
				files.put(id, new JsonParser().parse(new String(bytes.get(), StandardCharsets.UTF_8)));
			} catch (RuntimeException e) {
				LOGGER.warn("Couldn't read data/{}: {}", path, e.toString());
			}
		});
		Registry<T> registry = BuiltInRegistries.byKey(key);
		if (!(registry instanceof MappedRegistry)) {
			LOGGER.warn("The data pack registry {} isn't one the Translator fills ({})", key.location(), registry);
			return;
		}
		MappedRegistry<T> mapped = (MappedRegistry<T>) registry;
		var ops = RegistryOps.create(JsonOps.INSTANCE, RegistryAccess.INSTANCE);
		List<String> failed = new ArrayList<>();
		int read = 0;
		for (var file : files.entrySet()) {
			ResourceLocation id = file.getKey();
			JsonElement json = file.getValue();
			if (mapped.hasEntry(id) || !conditionsMet(json)) {
				continue;
			}
			try {
				DataResult<T> result = codec.parse(ops, json);
				var value = result.result();
				if (value.isPresent()) {
					mapped.register(id, value.get());
					read++;
				} else {
					failed.add(id + ": " + result.error().map(DataResult.PartialResult::message).orElse("no value"));
				}
			} catch (RuntimeException | LinkageError e) {
				failed.add(id + ": " + e);
			}
		}
		LOGGER.info("Data pack registry {}: {} entries from the mods' files", key.location(), read);
		for (String why : failed.subList(0, Math.min(failed.size(), 20))) {
			LOGGER.warn("Data pack registry {}: couldn't read {}", key.location(), why);
		}
	}

	private static boolean conditionsMet(JsonElement json) {
		if (!json.isJsonObject() || !((JsonObject) json).has("neoforge:conditions")) {
			return true;
		}
		try {
			var conditions = ICondition.LIST_CODEC.parse(JsonOps.INSTANCE, ((JsonObject) json).get("neoforge:conditions")).result();
			for (var condition : conditions.orElse(Collections.emptyList())) {
				if (!condition.test(TagManager.conditionContext())) {
					return false;
				}
			}
		} catch (RuntimeException | LinkageError e) {
			// a condition the Translator can't read: the file is kept
		}
		return true;
	}
}
