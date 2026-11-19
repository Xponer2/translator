/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.recipe;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import ladder.carried.net.minecraft.world.item.crafting.CraftingInput;
import ladder.carried.net.minecraft.world.item.crafting.RecipeInput;
import ladder.carried.net.minecraft.world.item.crafting.SingleRecipeInput;
import ladder.carried.net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

/**
 * 1.21's recipe inputs where 1.20.4 passes a Container (the generated bridges' conversion): an input becomes a
 * container over it; a container becomes the input the recipe takes (the same input when it was one, else made of the
 * container's slots as 1.21's menus make them: the crafting grid, a furnace's or stonecutter's first slot, the
 * smithing table's three).
 */
public final class RecipeInputs {
    private RecipeInputs() {
    }

    /** an input (a RecipeInput) -> a container 1.20.4's recipes read */
    @Nullable
    public static CraftingContainer container(@Nullable Object input) {
        if (input == null) {
            return null;
        }
        if (input instanceof CraftingContainer container) {
            return container;
        }
        return new InputContainer((RecipeInput) input);
    }

    /** a container 1.20.4 passes -> the input of the type the recipe takes */
    @Nullable
    public static Object recipeInput(@Nullable Container container, Class<?> wanted) {
        if (container == null) {
            return null;
        }
        if (container instanceof InputContainer own && wanted.isInstance(own.input)) {
            return own.input;
        }
        if (wanted.isAssignableFrom(CraftingInput.class) && (container instanceof CraftingContainer || wanted == CraftingInput.class)) {
            return craftingInput(container);
        }
        if (wanted == SingleRecipeInput.class || wanted.isAssignableFrom(SingleRecipeInput.class) && container.getContainerSize() == 1) {
            return new SingleRecipeInput(container.getItem(0));
        }
        if (wanted == SmithingRecipeInput.class) {
            return new SmithingRecipeInput(container.getItem(0), container.getItem(1), container.getItem(2));
        }
        if (wanted.isAssignableFrom(ContainerInput.class)) {
            return new ContainerInput(container);
        }
        // a mod's own input type made of a container it never saw: no input of it can be made
        throw new IllegalArgumentException("no " + wanted.getName() + " of " + container.getClass().getName());
    }

    /** 1.21's {@code CraftingContainer.asCraftInput()} */
    public static CraftingInput craftingInput(Container container) {
        if (container instanceof CraftingContainer crafting) {
            return CraftingInput.of(crafting.getWidth(), crafting.getHeight(), crafting.getItems());
        }
        List<ItemStack> items = new ArrayList<>(container.getContainerSize());
        for (int i = 0; i < container.getContainerSize(); i++) {
            items.add(container.getItem(i));
        }
        return CraftingInput.of(items.size(), 1, items);
    }

    /** 1.21's {@code CraftingContainer.asPositionedCraftInput()} */
    public static CraftingInput.Positioned positionedCraftingInput(CraftingContainer container) {
        return CraftingInput.ofPositioned(container.getWidth(), container.getHeight(), container.getItems());
    }

    /** a container read as an input, slot by slot */
    public record ContainerInput(Container container) implements RecipeInput {
        @Override
        public ItemStack getItem(int slot) {
            return this.container.getItem(slot);
        }

        @Override
        public int size() {
            return this.container.getContainerSize();
        }
    }
}
