/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package io.github.xponer2.translator.recipe.ingredient;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.util.RecipeItemHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

import javax.annotation.Nullable;

/**
 * A 1.12.2 {@link Ingredient} backed by a custom ingredient ({@link ICustomIngredient#toVanilla()}; NeoForge keeps it
 * inside {@code Ingredient}). The matching stacks are computed on first use.
 */
public class CustomIngredient extends Ingredient {
    private final ICustomIngredient custom;
    @Nullable
    private ItemStack[] stacks;
    @Nullable
    private IntList packed;

    public CustomIngredient(ICustomIngredient custom) {
        super(0);
        this.custom = custom;
    }

    public ICustomIngredient getCustomIngredient() {
        return this.custom;
    }

    @Override
    public ItemStack[] getMatchingStacks() {
        if (this.stacks == null) {
            this.stacks = this.custom.getItems().toArray(ItemStack[]::new);
        }
        return this.stacks;
    }

    @Override
    public boolean apply(@Nullable ItemStack stack) {
        return stack != null && !stack.isEmpty() && this.custom.test(stack);
    }

    @Override
    public IntList getValidItemStacksPacked() {
        if (this.packed == null) {
            var packed = new IntArrayList();
            for (var stack : this.getMatchingStacks()) {
                packed.add(RecipeItemHelper.pack(stack));
            }
            packed.sort(null);
            this.packed = packed;
        }
        return this.packed;
    }

    @Override
    public boolean isSimple() {
        return this.custom.isSimple();
    }

    @Override
    protected void invalidate() {
        this.stacks = null;
        this.packed = null;
    }
}
