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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.JsonOps;

import io.github.xponer2.translator.recipe.ICondition;
import io.github.xponer2.translator.tags.TagManager;
import io.github.xponer2.translator.util.BuiltInRegistries;
import net.minecraft.util.ResourceLocation;

/**
 * NeoForge's data maps ({@code data/<namespace>/data_maps/<registry>/<name>.json}): values the mods' data give registry
 * entries (Powah's coolants and heat sources, the Aether's fuels, Mekanism's). 1.12.2 has no data maps and no data pack
 * reload: the files of all mods are read once, a map's values when a mod first asks for it. A {@code DataMapType} is a
 * carried NeoForge class, asked for its id, registry and codec by reflection.
 */
public final class DataMaps {
	private static final Logger LOGGER = LogManager.getLogger("Translator Data Maps");
	private static final String FOLDER = "/data_maps/";
	// "<namespace>/data_maps/<registry>/<name>.json" -> the files of it, in mod order
	private static Map<String, List<String>> files;
	private static final Map<Object, Map<ResourceLocation, Object>> VALUES = new java.util.IdentityHashMap<>();

	private DataMaps() {
	}

	/** The map's values by entry id (empty when no mod gives it values or the type isn't one). */
	public static synchronized Map<ResourceLocation, Object> values(@Nullable Object type) {
		if (type == null) {
			return Collections.emptyMap();
		}
		var values = VALUES.get(type);
		if (values == null) {
			values = load(type);
			VALUES.put(type, values);
		}
		return values;
	}

	/** 1.21 {@code registry.getDataMap(type)}: the values by the entries' keys of the registry. */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Map<ResourceKey<?>, Object> byKey(Registry<?> registry, @Nullable Object type) {
		var result = new LinkedHashMap<ResourceKey<?>, Object>();
		ResourceKey registryKey = registry.key();
		values(type).forEach((id, value) -> result.put(ResourceKey.create(registryKey, id), value));
		return Collections.unmodifiableMap(result);
	}

	/** The value the map gives the entry of that id, null when none. */
	@Nullable
	public static Object get(@Nullable Object type, @Nullable ResourceLocation id) {
		return id == null ? null : values(type).get(id);
	}

	/** The value the map gives the holder's entry: by its key, or for a direct holder by the id of its value. */
	@Nullable
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Object get(@Nullable Object type, @Nullable Holder<?> holder) {
		if (holder == null || type == null) {
			return null;
		}
		Optional<? extends ResourceKey<?>> key = holder.unwrapKey();
		if (key.isPresent()) {
			return get(type, key.get().location());
		}
		Object registryKey = call(type, "registryKey");
		Registry registry = registryKey instanceof ResourceKey<?> k ? BuiltInRegistries.REGISTRY.get(k.location()) : null;
		return registry == null ? null : get(type, registry.getKey(holder.value()));
	}

	private static Map<ResourceLocation, Object> load(Object type) {
		Object id = call(type, "id");
		Object registryKey = call(type, "registryKey");
		Object codec = call(type, "codec");
		if (!(id instanceof ResourceLocation name) || !(registryKey instanceof ResourceKey<?> registry) || !(codec instanceof Decoder<?> decoder)) {
			return Collections.emptyMap();
		}
		ResourceLocation registryName = registry.location();
		String registryFolder = "minecraft".equals(registryName.getNamespace()) ? registryName.getPath()
				: registryName.getNamespace() + "/" + registryName.getPath();
		String path = name.getNamespace() + FOLDER + registryFolder + "/" + name.getPath() + ".json";
		var values = new LinkedHashMap<ResourceLocation, Object>();
		for (String text : files().getOrDefault(path, Collections.emptyList())) {
			try {
				read(new JsonParser().parse(text).getAsJsonObject(), decoder, registryName.getPath(), values);
			} catch (RuntimeException e) {
				LOGGER.error("Couldn't read the data map {}: {}", path, e.toString());
			}
		}
		if (!values.isEmpty()) {
			LOGGER.info("Data map {} of {}: {} values", name, registryName, values.size());
		}
		return values;
	}

	private static void read(JsonObject json, Decoder<?> decoder, String tagFolder, Map<ResourceLocation, Object> values) {
		if (json.has("neoforge:conditions")) {
			var conditions = ICondition.LIST_CODEC.parse(JsonOps.INSTANCE, json.get("neoforge:conditions")).result();
			for (var condition : conditions.orElse(Collections.emptyList())) {
				if (!condition.test(TagManager.conditionContext())) {
					return;
				}
			}
		}
		if (json.has("replace") && json.get("replace").getAsBoolean()) {
			values.clear();
		}
		if (json.has("values") && json.get("values").isJsonObject()) {
			for (var entry : json.getAsJsonObject("values").entrySet()) {
				Object value = decode(decoder, entry.getValue());
				if (value == null) {
					LOGGER.warn("A data map value for {} doesn't read: {}", entry.getKey(), entry.getValue());
					continue;
				}
				for (ResourceLocation target : targets(entry.getKey(), tagFolder)) {
					values.put(target, value);
				}
			}
		}
		if (json.has("remove") && json.get("remove").isJsonArray()) {
			for (JsonElement removed : json.getAsJsonArray("remove")) {
				if (removed.isJsonPrimitive()) {
					for (ResourceLocation target : targets(removed.getAsString(), tagFolder)) {
						values.remove(target);
					}
				}
			}
		}
	}

	/** An entry id, or the members of a tag ({@code #c:ingots}). */
	private static List<ResourceLocation> targets(String key, String tagFolder) {
		if (key.startsWith("#")) {
			return new ArrayList<>(TagManager.ids(tagFolder, new ResourceLocation(key.substring(1))));
		}
		return Collections.singletonList(new ResourceLocation(key));
	}

	/**
	 * A value as NeoForge reads one: {@code {"value": ..., "replace": true}} first, then the value itself.
	 */
	@Nullable
	private static Object decode(Decoder<?> decoder, JsonElement element) {
		try {
			if (element.isJsonObject() && element.getAsJsonObject().has("value")) {
				Optional<?> wrapped = decoder.parse(JsonOps.INSTANCE, element.getAsJsonObject().get("value")).result();
				if (wrapped.isPresent()) {
					return wrapped.get();
				}
			}
			return decoder.parse(JsonOps.INSTANCE, element).result().orElse(null);
		} catch (RuntimeException | LinkageError e) {
			return null;
		}
	}

	/** The data map files of the 1.21.1 mods (only they have data maps), from their jars: FML lists them late. */
	private static Map<String, List<String>> files() {
		if (files != null) {
			return files;
		}
		var found = new HashMap<String, List<String>>();
		io.github.xponer2.translator.runtime.RuntimeMods.forEachData((path, bytes) -> {
			if (path.contains(FOLDER) && path.endsWith(".json")) {
				try {
					found.computeIfAbsent(path, k -> new ArrayList<>()).add(new String(bytes.get(), StandardCharsets.UTF_8));
				} catch (RuntimeException e) {
					LOGGER.error("Couldn't read the data map {}: {}", path, e.toString());
				}
			}
		});
		LOGGER.info("Found {} data map files", found.size());
		files = found;
		return files;
	}

	@Nullable
	private static Object call(Object target, String method) {
		try {
			var m = target.getClass().getMethod(method);
			m.setAccessible(true);
			return m.invoke(target);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return null;
		}
	}
}
