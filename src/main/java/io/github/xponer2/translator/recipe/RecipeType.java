package io.github.xponer2.translator.recipe;

import net.minecraft.util.ResourceLocation;

/**
 * The kind of a recipe (1.21 {@code RecipeType}), registered in {@code Registries.RECIPE_TYPE}. The vanilla types
 * are constants; the recipes of a type are found with {@link RecipeManager#getAllRecipesFor(RecipeType)}.
 */
public interface RecipeType<T extends Recipe<?>> {
    RecipeType<CraftingRecipe> CRAFTING = simple(new ResourceLocation("minecraft", "crafting"));
    RecipeType<VanillaRecipes.SmeltingRecipe> SMELTING = simple(new ResourceLocation("minecraft", "smelting"));
    RecipeType<Recipe<SingleRecipeInput>> STONECUTTING = simple(new ResourceLocation("minecraft", "stonecutting"));

    static <T extends Recipe<?>> RecipeType<T> simple(ResourceLocation id) {
        return new RecipeType<T>() {
            @Override
            public String toString() {
                return id.toString();
            }
        };
    }
}
