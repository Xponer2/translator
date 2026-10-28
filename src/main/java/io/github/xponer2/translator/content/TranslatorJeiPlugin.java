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
package io.github.xponer2.translator.content;

import io.github.xponer2.translator.recipe.CraftingRecipeWrapper;
import io.github.xponer2.translator.recipe.ShapedRecipe;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.wrapper.ICraftingRecipeWrapper;
import mezz.jei.api.recipe.wrapper.IShapedCraftingRecipeWrapper;

/**
 * JEI for the Translator: shows the 1.21 crafting recipes of the ported mods (registered as {@link
 * CraftingRecipeWrapper}, a class JEI doesn't know) in the crafting category, and hides the Translator's copper when
 * another mod adds copper (see {@link Copper}). Loaded by JEI only.
 */
@JEIPlugin
public final class TranslatorJeiPlugin implements IModPlugin {
	@Override
	public void register(IModRegistry registry) {
		var helpers = registry.getJeiHelpers();
		registry.handleRecipes(CraftingRecipeWrapper.class, recipe -> recipe.recipe() instanceof ShapedRecipe
				? new Shaped(helpers, recipe) : new Shapeless(helpers, recipe), VanillaRecipeCategoryUid.CRAFTING);

		if (!Copper.isActive()) {
			var blacklist = helpers.getIngredientBlacklist();
			blacklist.addIngredientToBlacklist(new ItemStack(Copper.ingot));
			blacklist.addIngredientToBlacklist(new ItemStack(Copper.ore));
		}
	}

	private static class Shapeless implements ICraftingRecipeWrapper {
		private final IJeiHelpers helpers;
		protected final CraftingRecipeWrapper recipe;

		Shapeless(IJeiHelpers helpers, CraftingRecipeWrapper recipe) {
			this.helpers = helpers;
			this.recipe = recipe;
		}

		@Override
		public void getIngredients(IIngredients ingredients) {
			ingredients.setInputLists(VanillaTypes.ITEM, this.helpers.getStackHelper().expandRecipeItemStackInputs(this.recipe.getIngredients()));
			ingredients.setOutput(VanillaTypes.ITEM, this.recipe.getRecipeOutput());
		}

		@Override
		public ResourceLocation getRegistryName() {
			return this.recipe.getRegistryName();
		}
	}

	private static final class Shaped extends Shapeless implements IShapedCraftingRecipeWrapper {
		Shaped(IJeiHelpers helpers, CraftingRecipeWrapper recipe) {
			super(helpers, recipe);
		}

		@Override
		public int getWidth() {
			return ((ShapedRecipe) this.recipe.recipe()).getWidth();
		}

		@Override
		public int getHeight() {
			return ((ShapedRecipe) this.recipe.recipe()).getHeight();
		}
	}
}
