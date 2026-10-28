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
package io.github.xponer2.translator.recipe;

import io.github.xponer2.translator.registry.RegistryAccess;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * A 1.21 crafting recipe in the 1.12.2 recipe registry: the crafting table (and every 1.12.2 crafting grid) uses
 * the recipe through this.
 */
public final class CraftingRecipeWrapper extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    private final CraftingRecipe recipe;

    public CraftingRecipeWrapper(CraftingRecipe recipe) {
        this.recipe = recipe;
    }

    public CraftingRecipe recipe() {
        return this.recipe;
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        return this.recipe.matches(CraftingInput.of(inventory), world);
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        return this.recipe.assemble(CraftingInput.of(inventory), RegistryAccess.INSTANCE);
    }

    @Override
    public boolean canFit(int width, int height) {
        return this.recipe.canCraftInDimensions(width, height);
    }

    @Override
    public ItemStack getRecipeOutput() {
        return this.recipe.getResultItem(RegistryAccess.INSTANCE);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inventory) {
        var positioned = CraftingInput.ofPositioned(inventory);
        var input = positioned.input();
        var result = ForgeHooks.defaultRecipeGetRemainingItems(inventory);
        if (input.width() == 0) {
            return result;
        }
        var remaining = this.recipe.getRemainingItems(input);
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                var slot = x + positioned.left() + (y + positioned.top()) * inventory.getWidth();
                result.set(slot, remaining.get(x + y * input.width()));
            }
        }
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.recipe.getIngredients();
    }

    @Override
    public boolean isDynamic() {
        return this.recipe.isSpecial();
    }

    @Override
    public String getGroup() {
        return this.recipe.getGroup();
    }
}
