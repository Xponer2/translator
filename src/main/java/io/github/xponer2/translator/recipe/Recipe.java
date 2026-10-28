package io.github.xponer2.translator.recipe;

import io.github.xponer2.translator.registry.HolderLookup;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

/**
 * A recipe of the 1.21 recipe system (1.21 {@code net.minecraft.world.item.crafting.Recipe}). Loaded from
 * {@code data/<namespace>/recipe} by {@link RecipeManager}; crafting recipes also work in the 1.12.2 crafting table.
 */
public interface Recipe<T extends RecipeInput> {
    boolean matches(T input, World level);

    ItemStack assemble(T input, HolderLookup.Provider registries);

    boolean canCraftInDimensions(int width, int height);

    ItemStack getResultItem(HolderLookup.Provider registries);

    default NonNullList<ItemStack> getRemainingItems(T input) {
        var remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < remaining.size(); i++) {
            var stack = input.getItem(i);
            if (stack.getItem().hasContainerItem(stack)) {
                remaining.set(i, stack.getItem().getContainerItem(stack));
            }
        }
        return remaining;
    }

    default NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    default boolean isSpecial() {
        return false;
    }

    default boolean showNotification() {
        return true;
    }

    default String getGroup() {
        return "";
    }

    default ItemStack getToastSymbol() {
        return new ItemStack(Blocks.CRAFTING_TABLE);
    }

    RecipeSerializer<?> getSerializer();

    RecipeType<?> getType();

    default boolean isIncomplete() {
        var ingredients = this.getIngredients();
        return ingredients.isEmpty() || ingredients.stream().anyMatch(i -> i != Ingredient.EMPTY && i.getMatchingStacks().length == 0);
    }
}
