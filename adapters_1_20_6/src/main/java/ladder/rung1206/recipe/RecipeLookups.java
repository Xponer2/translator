/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.recipe;

import java.util.Optional;

import javax.annotation.Nullable;

import ladder.carried_1_20_6.net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 1.21's recipe lookups on an input (1.20.6: a container), with the last recipe found as its holder (1.20.6: its id).
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

    /** {@code recipes.getRecipeFor(type, input, level, lastRecipeId)} (1.20.6 gives the holder too) */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Optional<RecipeHolder<?>> getRecipeFor(RecipeManager recipes, RecipeType<?> type, RecipeInput input, Level level, @Nullable ResourceLocation last) {
        Container container = RecipeInputs.container(input);
        if (container.isEmpty()) {
            // 1.21 finds no recipe for an empty input
            return Optional.empty();
        }
        return (Optional) recipes.getRecipeFor((RecipeType) type, container, level, last);
    }
}
