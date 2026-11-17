/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.recipe;

/**
 * A crafting table recipe (1.21 {@code CraftingRecipe}).
 */
public interface CraftingRecipe extends Recipe<CraftingInput> {
    CraftingBookCategory category();

    @Override
    default RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }
}
