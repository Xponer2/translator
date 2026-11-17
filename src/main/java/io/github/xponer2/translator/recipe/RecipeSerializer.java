/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.recipe;

import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;
import io.github.xponer2.translator.network.StreamCodec;
import com.mojang.serialization.MapCodec;

/**
 * Reads a recipe type from JSON and the network (1.21 {@code RecipeSerializer}), registered under the recipe's
 * {@code "type"} id in {@code Registries.RECIPE_SERIALIZER}.
 */
public interface RecipeSerializer<T extends Recipe<?>> {
    /** The vanilla serializers (a mod's serializer wrapping the shaped one: Sophisticated Core's upgrade recipes). */
    RecipeSerializer<ShapedRecipe> SHAPED_RECIPE = VanillaRecipes.SHAPED;
    RecipeSerializer<ShapelessRecipe> SHAPELESS_RECIPE = VanillaRecipes.SHAPELESS;
    RecipeSerializer<VanillaRecipes.SmeltingRecipe> SMELTING_RECIPE = VanillaRecipes.SMELTING;
    RecipeSerializer<VanillaRecipes.StonecutterRecipe> STONECUTTER = VanillaRecipes.STONECUTTER;

    MapCodec<T> codec();

    StreamCodec<RegistryFriendlyByteBuf, T> streamCodec();
}
