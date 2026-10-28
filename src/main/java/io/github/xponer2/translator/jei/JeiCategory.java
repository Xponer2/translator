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

import io.github.xponer2.translator.client.GuiGraphics;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Shows a JEI 1.21 style category ({@link IRecipeCategory}) in JEI 4.16: each recipe is wrapped in a
 * {@link Wrapper}, its slots come from {@link IRecipeCategory#setRecipe} (positions of the 16x16 ingredient, JEI 4.16
 * places the 18x18 slot one pixel up and left).
 */
public final class JeiCategory<T> implements mezz.jei.api.recipe.IRecipeCategory<JeiCategory.Wrapper<T>> {
    private final IRecipeCategory<T> category;
    private final String modName;

    public JeiCategory(IRecipeCategory<T> category, String modName) {
        this.category = category;
        this.modName = modName;
    }

    /**
     * Adds the recipes of a category to JEI 4.16.
     */
    public static <T> void addRecipes(IModRegistry registry, IRecipeCategory<T> category, Collection<? extends T> recipes) {
        var wrappers = new ArrayList<Wrapper<T>>();
        for (var recipe : recipes) {
            wrappers.add(new Wrapper<>(category, recipe));
        }
        registry.addRecipes(wrappers, category.getRecipeType().getUid());
    }

    @Override
    public String getUid() {
        return this.category.getRecipeType().getUid();
    }

    @Override
    public String getTitle() {
        return this.category.getTitle().getFormattedText();
    }

    @Override
    public String getModName() {
        return this.modName;
    }

    @Override
    public IDrawable getBackground() {
        return this.category.getBackground();
    }

    @Nullable
    @Override
    public IDrawable getIcon() {
        return this.category.getIcon();
    }

    @Override
    public void setRecipe(IRecipeLayout layout, Wrapper<T> wrapper, IIngredients ingredients) {
        var stacks = layout.getItemStacks();
        var slots = wrapper.slots();
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            stacks.init(i, slot.role() != RecipeIngredientRole.OUTPUT, slot.x() - 1, slot.y() - 1);
            stacks.set(i, slot.stacks());
        }
        stacks.addTooltipCallback((index, input, stack, tooltip) -> {
            if (index < 0 || index >= slots.size() || slots.get(index).tooltip() == null) {
                return;
            }
            var slot = slots.get(index);
            slot.tooltip().onRichTooltip(view(slot, stack), line -> tooltip.add(line.getFormattedText()));
        });
    }

    private static IRecipeSlotView view(IRecipeLayoutBuilder.Slot slot, @Nullable ItemStack shown) {
        return new IRecipeSlotView() {
            @Override
            public Optional<ItemStack> getDisplayedItemStack() {
                return shown == null || shown.isEmpty() ? Optional.empty() : Optional.of(shown);
            }

            @Override
            public RecipeIngredientRole getRole() {
                return slot.role();
            }
        };
    }

    /**
     * One recipe for JEI 4.16.
     */
    public static final class Wrapper<T> implements IRecipeWrapper {
        private final IRecipeCategory<T> category;
        private final T recipe;
        private List<IRecipeLayoutBuilder.Slot> slots;

        Wrapper(IRecipeCategory<T> category, T recipe) {
            this.category = category;
            this.recipe = recipe;
        }

        public T recipe() {
            return this.recipe;
        }

        List<IRecipeLayoutBuilder.Slot> slots() {
            if (this.slots == null) {
                var builder = new IRecipeLayoutBuilder();
                this.category.setRecipe(builder, this.recipe, IFocusGroup.EMPTY);
                this.slots = builder.slots();
            }
            return this.slots;
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            var inputs = new ArrayList<List<ItemStack>>();
            var outputs = new ArrayList<List<ItemStack>>();
            for (var slot : this.slots()) {
                if (slot.role() == RecipeIngredientRole.OUTPUT) {
                    outputs.add(slot.stacks());
                } else if (slot.role() != RecipeIngredientRole.RENDER_ONLY) {
                    inputs.add(slot.stacks());
                }
            }
            ingredients.setInputLists(VanillaTypes.ITEM, inputs);
            ingredients.setOutputLists(VanillaTypes.ITEM, outputs);
        }

        @Override
        public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
            this.category.draw(this.recipe, IRecipeSlotsView.EMPTY, new GuiGraphics(minecraft), mouseX, mouseY);
        }

        @Override
        public List<String> getTooltipStrings(int mouseX, int mouseY) {
            var lines = new ArrayList<String>();
            for (var line : this.category.getTooltipStrings(this.recipe, IRecipeSlotsView.EMPTY, mouseX, mouseY)) {
                lines.add(line.getFormattedText());
            }
            return lines;
        }
    }
}
