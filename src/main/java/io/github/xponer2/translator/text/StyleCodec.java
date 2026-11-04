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
package io.github.xponer2.translator.text;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;

import net.minecraft.util.EnumTypeAdapterFactory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;

/**
 * 1.21 {@code Style.Serializer.CODEC} (a style as a JSON object: color, bold, italic, ...) over the 1.12.2 style JSON:
 * Jade keeps its mod name style in its config with it; without it its config codec was null, the config never loaded
 * and its tooltip never showed. A static field the converter reads instead of the missing one (redirect table).
 */
public final class StyleCodec {
	private static final Gson GSON = new GsonBuilder().registerTypeHierarchyAdapter(ITextComponent.class, new ITextComponent.Serializer())
			.registerTypeHierarchyAdapter(Style.class, new Style.Serializer()).registerTypeAdapterFactory(new EnumTypeAdapterFactory()).create();

	public static final Codec<Style> CODEC = Codec.PASSTHROUGH.comapFlatMap(StyleCodec::decode, StyleCodec::encode);

	private StyleCodec() {
	}

	private static DataResult<Style> decode(Dynamic<?> dynamic) {
		try {
			JsonElement json = dynamic.convert(JsonOps.INSTANCE).getValue();
			Style style = GSON.fromJson(json, Style.class);
			return DataResult.success(style != null ? style : new Style());
		} catch (RuntimeException e) {
			return DataResult.error("Not a style: " + e.getMessage());
		}
	}

	private static Dynamic<?> encode(Style style) {
		return new Dynamic<>(JsonOps.INSTANCE, GSON.toJsonTree(style, Style.class));
	}
}
