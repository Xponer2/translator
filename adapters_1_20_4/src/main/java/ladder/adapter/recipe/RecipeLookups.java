/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.recipe;

import java.util.Optional;

import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import ladder.carried.net.minecraft.network.RegistryFriendlyByteBuf;
import ladder.carried.net.minecraft.network.codec.StreamCodec;
import ladder.carried.net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

/**
 * 1.21's recipe lookups and stream codecs that 1.20.4 has in another form: getRecipeFor with the last recipe found
 * (1.20.4 gives the id and the holder as a pair), the shaped pattern's and the book category's stream codecs (1.20.4:
 * toNetwork / fromNetwork, the enum by its ordinal).
 */
public final class RecipeLookups {
    private RecipeLookups() {
    }

    /** {@code recipes.getRecipeFor(type, input, level, lastRecipe)} (1.21: the last recipe's holder) */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Optional<RecipeHolder<?>> getRecipeFor(RecipeManager recipes, RecipeType<?> type, RecipeInput input, Level level, @Nullable RecipeHolder<?> last) {
        return getRecipeFor(recipes, type, input, level, last == null ? null : last.id());
    }

    /** {@code recipes.getRecipeFor(type, input, level)} (1.21: none for an empty input) */
    public static Optional<RecipeHolder<?>> getRecipeFor(RecipeManager recipes, RecipeType<?> type, RecipeInput input, Level level) {
        return getRecipeFor(recipes, type, input, level, (ResourceLocation) null);
    }

    /** {@code recipes.getRecipeFor(type, input, level, lastRecipeId)} (1.21 gives the holder, 1.20.4 a pair of id and holder) */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Optional<RecipeHolder<?>> getRecipeFor(RecipeManager recipes, RecipeType<?> type, RecipeInput input, Level level, @Nullable ResourceLocation last) {
        Container container = RecipeInputs.container(input);
        if (container.isEmpty()) {
            // 1.21 finds no recipe for an empty input
            return Optional.empty();
        }
        Optional found = recipes.getRecipeFor((RecipeType) type, container, level, last);
        return found.map(pair -> ((com.mojang.datafixers.util.Pair<ResourceLocation, RecipeHolder<?>>) pair).getSecond());
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, ShapedRecipePattern> PATTERN = StreamCodec.of(
            (buffer, pattern) -> pattern.toNetwork(buffer), ShapedRecipePattern::fromNetwork);

    /** 1.20.5's {@code ShapedRecipePattern.STREAM_CODEC} */
    public static StreamCodec<RegistryFriendlyByteBuf, ShapedRecipePattern> shapedPattern() {
        return PATTERN;
    }

    private static final StreamCodec<ByteBuf, CraftingBookCategory> CATEGORY = StreamCodec.of(
            (buffer, category) -> new FriendlyByteBuf(buffer).writeVarInt(category.ordinal()),
            buffer -> CraftingBookCategory.values()[new FriendlyByteBuf(buffer).readVarInt()]);

    /** 1.20.5's {@code CraftingBookCategory.STREAM_CODEC} */
    public static StreamCodec<ByteBuf, CraftingBookCategory> craftingBookCategory() {
        return CATEGORY;
    }
}
