package io.github.xponer2.translator.jei;

import net.minecraft.item.ItemStack;

import java.util.Optional;

/**
 * One slot of a shown recipe (JEI 1.21 {@code IRecipeSlotView}).
 */
public interface IRecipeSlotView {
    Optional<ItemStack> getDisplayedItemStack();

    RecipeIngredientRole getRole();
}
