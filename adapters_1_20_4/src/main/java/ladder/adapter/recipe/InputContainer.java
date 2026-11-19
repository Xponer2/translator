/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.recipe;

import java.util.ArrayList;
import java.util.List;

import ladder.carried.net.minecraft.world.item.crafting.CraftingInput;
import ladder.carried.net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

/**
 * A 1.21 recipe input (1.20.4's recipes take a Container) handed to 1.20.4's recipe code: a read-only crafting
 * container over it (a crafting input's width and height; any other input one row). A converted recipe's override
 * gets the input itself back ({@link RecipeInputs#recipeInput}): the mod's own input types go through 1.20.4's
 * recipe manager unchanged.
 */
public final class InputContainer implements CraftingContainer {
    final RecipeInput input;

    InputContainer(RecipeInput input) {
        this.input = input;
    }

    public RecipeInput input() {
        return this.input;
    }

    @Override
    public int getWidth() {
        return this.input instanceof CraftingInput crafting ? crafting.width() : this.input.size();
    }

    @Override
    public int getHeight() {
        return this.input instanceof CraftingInput crafting ? crafting.height() : 1;
    }

    @Override
    public List<ItemStack> getItems() {
        if (this.input instanceof CraftingInput crafting) {
            return crafting.items();
        }
        List<ItemStack> items = new ArrayList<>(this.input.size());
        for (int i = 0; i < this.input.size(); i++) {
            items.add(this.input.getItem(i));
        }
        return items;
    }

    @Override
    public int getContainerSize() {
        return this.input.size();
    }

    @Override
    public boolean isEmpty() {
        return this.input.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < this.input.size() ? this.input.getItem(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
    }

    @Override
    public void fillStackedContents(StackedContents contents) {
        for (int i = 0; i < this.input.size(); i++) {
            contents.accountSimpleStack(this.input.getItem(i));
        }
    }
}
