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
package io.github.xponer2.translator.recipe;

import io.github.xponer2.translator.network.ByteBufCodecs;
import io.github.xponer2.translator.network.StreamCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;

import java.util.Locale;

/**
 * The recipe book tab of a crafting recipe (1.21 {@code CraftingBookCategory}); 1.12.2 has no recipe book tabs.
 */
public enum CraftingBookCategory {
    BUILDING,
    REDSTONE,
    EQUIPMENT,
    MISC;

    public static final Codec<CraftingBookCategory> CODEC = Codec.STRING.comapFlatMap(name -> {
        for (var category : values()) {
            if (category.getSerializedName().equals(name)) {
                return DataResult.success(category);
            }
        }
        return DataResult.error("Unknown crafting book category: " + name);
    }, CraftingBookCategory::getSerializedName);
    public static final StreamCodec<ByteBuf, CraftingBookCategory> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(i -> values()[i], Enum::ordinal);

    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
