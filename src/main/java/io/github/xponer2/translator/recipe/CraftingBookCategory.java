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
