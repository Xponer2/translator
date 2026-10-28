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
package io.github.xponer2.translator.recipe.ingredient;

import io.github.xponer2.translator.network.ByteBufCodecs;
import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;
import io.github.xponer2.translator.network.StreamCodec;
import io.github.xponer2.translator.registry.NeoForgeRegistries;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * The type of a custom ingredient (NeoForge {@code IngredientType}): its codec under {@code {"type": id, ...}}.
 * Registered in {@link NeoForgeRegistries#INGREDIENT_TYPES}; {@code Codecs.INGREDIENT} finds it there by id.
 */
public final class IngredientType<T extends ICustomIngredient> {
    private final MapCodec<T> codec;
    private final StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec;

    public IngredientType(MapCodec<T> codec) {
        this(codec, ByteBufCodecs.fromCodecWithRegistries(codec.codec()));
    }

    public IngredientType(MapCodec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        this.codec = codec;
        this.streamCodec = streamCodec;
    }

    public MapCodec<T> codec() {
        return this.codec;
    }

    public StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() {
        return this.streamCodec;
    }

    /**
     * The registry id, null while the type isn't registered.
     */
    @Nullable
    public ResourceLocation id() {
        NeoForgeIngredientTypes.register();
        return NeoForgeRegistries.INGREDIENT_TYPES.getKey(this);
    }

    /**
     * The registered type of an id (the NeoForge types are registered on first use), null if there is none.
     */
    @Nullable
    public static IngredientType<?> byId(ResourceLocation id) {
        NeoForgeIngredientTypes.register();
        return NeoForgeRegistries.INGREDIENT_TYPES.get(id);
    }

    @Override
    public String toString() {
        var id = this.id();
        return id != null ? id.toString() : "IngredientType@" + Integer.toHexString(this.hashCode());
    }
}
