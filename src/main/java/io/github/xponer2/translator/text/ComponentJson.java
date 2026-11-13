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

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.HolderLookup;
import io.github.xponer2.translator.runtime.redirect.ApiBridges;

import net.minecraft.util.text.ITextComponent;

/**
 * 1.21 {@code Component.Serializer.fromJson / fromJsonLenient / toJson} with the registries (X lines): the 1.12.2 JSON
 * of a text component. The 1.21.1 code (the shadow) read it with its codec into a 1.12.2 component and cast that to a
 * MutableComponent, which threw (Supplementaries' speaker block lost its message when loaded).
 */
public final class ComponentJson {
	private ComponentJson() {
	}

	@Nullable
	public static MutableComponent fromJson(String json, @Nullable HolderLookup.Provider registries) {
		ITextComponent text = ITextComponent.Serializer.jsonToComponent(json);
		return text == null ? null : ApiBridges.copy(text);
	}

	@Nullable
	public static MutableComponent jsonToComponent(String json, @Nullable HolderLookup.Provider registries) {
		return fromJson(json, registries);
	}

	@Nullable
	public static MutableComponent fromJsonLenient(String json, @Nullable HolderLookup.Provider registries) {
		ITextComponent text = ITextComponent.Serializer.fromJsonLenient(json);
		return text == null ? null : ApiBridges.copy(text);
	}

	public static String toJson(ITextComponent text, @Nullable HolderLookup.Provider registries) {
		return ITextComponent.Serializer.componentToJson(text);
	}

	public static String componentToJson(ITextComponent text, @Nullable HolderLookup.Provider registries) {
		return toJson(text, registries);
	}
}
