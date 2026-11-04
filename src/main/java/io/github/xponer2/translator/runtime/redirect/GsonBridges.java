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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/**
 * Gson 2.10 members the game's Gson 2.8.0 lacks (the 1.21.1 mods are built against 2.10): {@code new JsonArray(capacity)}
 * (JEI's config writer got null for its lists and JEI didn't start), {@code JsonObject.keySet / asMap / isEmpty},
 * {@code JsonArray.asList} (live views, as in 2.10), {@code Gson.fromJson(..., TypeToken)}.
 */
public final class GsonBridges {
	@Nullable
	private static final Field OBJECT_MEMBERS = field(JsonObject.class, "members");
	@Nullable
	private static final Field ARRAY_ELEMENTS = field(JsonArray.class, "elements");

	private GsonBridges() {
	}

	@Nullable
	private static Field field(Class<?> owner, String name) {
		try {
			Field field = owner.getDeclaredField(name);
			field.setAccessible(true);
			return field;
		} catch (NoSuchFieldException | RuntimeException e) {
			return null;
		}
	}

	/** 1.21's Gson {@code new JsonArray(capacity)}. */
	public static JsonArray newJsonArray(int capacity) {
		return new JsonArray();
	}

	/** {@code object.keySet()}: the member names (removing one removes the member). */
	public static Set<String> keySet(JsonObject object) {
		Map<String, JsonElement> members = asMap(object);
		return members.keySet();
	}

	public static boolean isEmpty(JsonObject object) {
		return object.entrySet().isEmpty();
	}

	/** {@code object.asMap()}: the object's members as a live map. */
	@SuppressWarnings("unchecked")
	public static Map<String, JsonElement> asMap(JsonObject object) {
		if (OBJECT_MEMBERS != null) {
			try {
				return (Map<String, JsonElement>) OBJECT_MEMBERS.get(object);
			} catch (IllegalAccessException e) {
				// a copy below
			}
		}
		var copy = new java.util.LinkedHashMap<String, JsonElement>();
		for (var entry : object.entrySet()) {
			copy.put(entry.getKey(), entry.getValue());
		}
		return copy;
	}

	/** {@code array.asList()}: the array's elements as a live list. */
	@SuppressWarnings("unchecked")
	public static List<JsonElement> asList(JsonArray array) {
		if (ARRAY_ELEMENTS != null) {
			try {
				return (List<JsonElement>) ARRAY_ELEMENTS.get(array);
			} catch (IllegalAccessException e) {
				// a copy below
			}
		}
		var copy = new java.util.ArrayList<JsonElement>(array.size());
		for (JsonElement element : array) {
			copy.add(element);
		}
		return copy;
	}

	// Gson 2.10's fromJson with a TypeToken (2.8.0 takes its Type)

	public static <T> T fromJson(Gson gson, JsonElement json, TypeToken<T> type) {
		return gson.fromJson(json, type.getType());
	}

	public static <T> T fromJson(Gson gson, String json, TypeToken<T> type) {
		return gson.fromJson(json, type.getType());
	}

	public static <T> T fromJson(Gson gson, java.io.Reader json, TypeToken<T> type) {
		return gson.fromJson(json, type.getType());
	}

	public static <T> T fromJson(Gson gson, com.google.gson.stream.JsonReader json, TypeToken<T> type) {
		return gson.fromJson(json, type.getType());
	}
}
