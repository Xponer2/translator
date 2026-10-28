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
package io.github.xponer2.translator.jei;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Where a recipe's slots go and what they show (JEI 1.21 {@code IRecipeLayoutBuilder}). The positions are those of
 * the 16x16 ingredient, like on 1.21.
 */
public final class IRecipeLayoutBuilder {
    private final List<Slot> slots = new ArrayList<>();

    public Slot addSlot(RecipeIngredientRole role, int x, int y) {
        var slot = new Slot(role, x, y);
        this.slots.add(slot);
        return slot;
    }

    public List<Slot> slots() {
        return Collections.unmodifiableList(this.slots);
    }

    /**
     * A slot (JEI 1.21 {@code IRecipeSlotBuilder}).
     */
    public static final class Slot {
        private final RecipeIngredientRole role;
        private final int x;
        private final int y;
        private final List<ItemStack> stacks = new ArrayList<>();
        @Nullable
        private IRecipeSlotRichTooltipCallback tooltip;

        Slot(RecipeIngredientRole role, int x, int y) {
            this.role = role;
            this.x = x;
            this.y = y;
        }

        public Slot addIngredients(Ingredient ingredient) {
            this.stacks.addAll(Arrays.asList(ingredient.getMatchingStacks()));
            return this;
        }

        public Slot addItemStack(ItemStack stack) {
            this.stacks.add(stack);
            return this;
        }

        public Slot addItemStacks(List<ItemStack> stacks) {
            this.stacks.addAll(stacks);
            return this;
        }

        public Slot addRichTooltipCallback(IRecipeSlotRichTooltipCallback callback) {
            this.tooltip = callback;
            return this;
        }

        public RecipeIngredientRole role() {
            return this.role;
        }

        public int x() {
            return this.x;
        }

        public int y() {
            return this.y;
        }

        public List<ItemStack> stacks() {
            return this.stacks;
        }

        @Nullable
        public IRecipeSlotRichTooltipCallback tooltip() {
            return this.tooltip;
        }
    }
}
