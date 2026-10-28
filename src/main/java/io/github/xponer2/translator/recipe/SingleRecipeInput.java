package io.github.xponer2.translator.recipe;

import net.minecraft.item.ItemStack;

/**
 * One item (1.21 {@code SingleRecipeInput}): smelting and the like.
 */
public final class SingleRecipeInput implements RecipeInput {
    private final ItemStack item;

    public SingleRecipeInput(ItemStack item) {
        this.item = item;
    }

    public ItemStack item() {
        return this.item;
    }

    @Override
    public ItemStack getItem(int index) {
        if (index != 0) {
            throw new IllegalArgumentException("No item for index " + index);
        }
        return this.item;
    }

    @Override
    public int size() {
        return 1;
    }
}
