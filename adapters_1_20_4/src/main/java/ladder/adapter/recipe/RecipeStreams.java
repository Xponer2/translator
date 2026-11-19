/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.recipe;

import ladder.carried.net.minecraft.network.RegistryFriendlyByteBuf;
import ladder.carried.net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** 1.20.5's {@code serializer.streamCodec()} called on any serializer: a converted mod's own, or 1.20.4's (fromNetwork / toNetwork). */
public final class RecipeStreams {
    private RecipeStreams() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static StreamCodec<RegistryFriendlyByteBuf, ?> streamCodec(RecipeSerializer<?> serializer) {
        if (serializer instanceof RecipeStreamApi own) {
            return own.streamCodec();
        }
        RecipeSerializer raw = serializer;
        return StreamCodec.of((buffer, recipe) -> raw.toNetwork(buffer, (Recipe) recipe), buffer -> (Recipe<?>) raw.fromNetwork(buffer));
    }
}
