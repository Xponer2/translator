/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.recipe;

import ladder.adapter.network.Payloads;
import ladder.carried.net.minecraft.network.RegistryFriendlyByteBuf;
import ladder.carried.net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * 1.20.5's recipe serializer: a stream codec ({@code streamCodec()}) where 1.20.4's has fromNetwork and toNetwork. A
 * converted mod serializer gets this interface (an O row): 1.20.4's recipe packet reads and writes its recipes with
 * the mod's stream codec, on a registry buffer over the packet's.
 */
public interface RecipeStreamApi<T extends Recipe<?>> extends RecipeSerializer<T> {
    StreamCodec<RegistryFriendlyByteBuf, T> streamCodec();

    @Override
    default T fromNetwork(FriendlyByteBuf buffer) {
        return this.streamCodec().decode(Payloads.registryBuffer(buffer));
    }

    @Override
    default void toNetwork(FriendlyByteBuf buffer, T recipe) {
        this.streamCodec().encode(Payloads.registryBuffer(buffer), recipe);
    }

    /** a super call of 1.20.5's abstract streamCodec(): none can be made */
    static StreamCodec<RegistryFriendlyByteBuf, ?> streamCodec(RecipeSerializer<?> self) {
        throw new AbstractMethodError("RecipeSerializer.streamCodec");
    }
}
