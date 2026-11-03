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

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import io.github.xponer2.translator.network.ByteBufCodecs;
import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;
import io.github.xponer2.translator.network.StreamCodec;
import io.github.xponer2.translator.util.Codecs;

import net.minecraft.item.crafting.Ingredient;

/** The static fields 1.21 {@code Ingredient} has and 1.12.2's doesn't (runtime): its codecs, in the 1.21 formats. */
public final class IngredientStatics {
	public static final Codec<Ingredient> CODEC = Codecs.INGREDIENT;
	public static final Codec<Ingredient> CODEC_NONEMPTY = Codecs.INGREDIENT_NONEMPTY;
	public static final Codec<List<Ingredient>> LIST_CODEC = CODEC.listOf();
	public static final Codec<List<Ingredient>> LIST_CODEC_NONEMPTY = LIST_CODEC.flatXmap(IngredientStatics::nonEmpty, IngredientStatics::nonEmpty);
	public static final StreamCodec<RegistryFriendlyByteBuf, Ingredient> CONTENTS_STREAM_CODEC = ByteBufCodecs.INGREDIENT;

	private IngredientStatics() {
	}

	private static DataResult<List<Ingredient>> nonEmpty(List<Ingredient> list) {
		return list.isEmpty() ? DataResult.error("Recipe has no ingredients") : DataResult.success(list);
	}
}
