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

import java.lang.reflect.Type;

import javax.annotation.Nullable;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import io.github.xponer2.translator.registry.HolderLookup;

import net.minecraft.util.text.ITextComponent;

/**
 * 1.21 {@code Component.SerializerAdapter} (neoforge-1.21.1-runtime.tsv): text in a mod's own Gson files (AE2's screen
 * styles: every label of its screens). The JSON text format of 1.21 is 1.12.2's (text, translate, with, extra, color,
 * bold, ...), so 1.12.2's reader does it; the registries 1.21 passes are not needed.
 */
public class ComponentSerializerAdapter implements JsonDeserializer<ITextComponent>, JsonSerializer<ITextComponent> {
	public ComponentSerializerAdapter(@Nullable HolderLookup.Provider registries) {
	}

	@Override
	public ITextComponent deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
		return ITextComponent.Serializer.jsonToComponent(json.toString());
	}

	@Override
	public JsonElement serialize(ITextComponent text, Type type, JsonSerializationContext context) {
		return new JsonParser().parse(ITextComponent.Serializer.componentToJson(text));
	}
}
