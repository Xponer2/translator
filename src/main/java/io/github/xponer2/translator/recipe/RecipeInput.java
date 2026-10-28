package io.github.xponer2.translator.recipe;

import net.minecraft.item.ItemStack;

/**
 * The items a recipe is matched against (1.21 {@code RecipeInput}).
 */
public interface RecipeInput {
    ItemStack getItem(int index);

    int size();

    default boolean isEmpty() {
        for (int i = 0; i < this.size(); i++) {
            if (!this.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
