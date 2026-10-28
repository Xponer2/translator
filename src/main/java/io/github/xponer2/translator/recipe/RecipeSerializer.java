package io.github.xponer2.translator.recipe;

import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;
import io.github.xponer2.translator.network.StreamCodec;
import com.mojang.serialization.MapCodec;

/**
 * Reads a recipe type from JSON and the network (1.21 {@code RecipeSerializer}), registered under the recipe's
 * {@code "type"} id in {@code Registries.RECIPE_SERIALIZER}.
 */
public interface RecipeSerializer<T extends Recipe<?>> {
    MapCodec<T> codec();

    StreamCodec<RegistryFriendlyByteBuf, T> streamCodec();
}
