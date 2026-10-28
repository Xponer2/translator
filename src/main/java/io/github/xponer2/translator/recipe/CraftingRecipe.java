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
