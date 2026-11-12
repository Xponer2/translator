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

import com.mojang.serialization.Codec;

import io.github.xponer2.translator.network.ByteBufCodecs;
import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;
import io.github.xponer2.translator.network.StreamCodec;
import io.github.xponer2.translator.recipe.Recipe;
import io.github.xponer2.translator.recipe.RecipeSerializer;
import io.github.xponer2.translator.util.BuiltInRegistries;
import io.github.xponer2.translator.util.Registries;

/**
 * 1.21 {@code Recipe.CODEC} and {@code Recipe.STREAM_CODEC}, which the Translator's Recipe interface hasn't: a recipe
 * by its serializer (its "type" id) and that serializer's codec, as 1.21 dispatches them. Create's sequenced assembly
 * steps (SequencedRecipe's static initializer was left empty), JEI's and Moonlight's recipe readers.
 */
public final class RecipeCodecs {
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static final Codec<Recipe<?>> CODEC = BuiltInRegistries.RECIPE_SERIALIZER.byNameCodec()
			.<Recipe<?>>dispatch(recipe -> recipe.getSerializer(), serializer -> (Codec) ((RecipeSerializer) serializer).codec().codec());
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static final StreamCodec<RegistryFriendlyByteBuf, Recipe<?>> STREAM_CODEC = (StreamCodec) ByteBufCodecs.registry(Registries.RECIPE_SERIALIZER)
			.dispatch(recipe -> ((Recipe<?>) recipe).getSerializer(), serializer -> (StreamCodec) ((RecipeSerializer) serializer).streamCodec());

	private RecipeCodecs() {
	}
}
