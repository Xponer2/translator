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
package io.github.xponer2.translator.client;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.recipebook.GuiRecipeBook;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Facade of the 1.21 {@code RecipeBookComponent} (neoforge-1.21.1-supers.tsv) for a 1.21.1 mod's recipe book (Farmer's
 * Delight's cooking pot). The 1.12.2 book is made for a crafting grid; a 1.21 menu's book isn't set up, so it stays
 * closed: the screen, its slots and its clicks work (clicking a slot asked the never-initialized 1.12.2 book whether it
 * was open and crashed the game).
 */
@SideOnly(Side.CLIENT)
public class RecipeBookComponent extends GuiRecipeBook {
	/** The 1.12.2 book was set up (a crafting grid): only then does it do anything. */
	private boolean initialized;

	// --- 1.21 ---------------------------------------------------------------------------------------------------------

	/** 1.21 {@code init(width, height, minecraft, widthTooNarrow, menu)}: the book of a 1.21 menu stays closed. */
	public void init(int width, int height, Minecraft minecraft, boolean widthTooNarrow, Object menu) {
	}

	public void initVisuals() {
	}

	/** 1.21 filter button look (a subclass sets its own textures): the book stays closed, nothing to set. */
	protected void initFilterButtonTextures() {
	}

	/** 1.21 {@code getFuelItems()} of a furnace book: none, the book is closed. */
	protected java.util.Set<net.minecraft.item.Item> getFuelItems() {
		return java.util.Collections.emptySet();
	}

	/** 1.21 {@code setupGhostRecipe(recipe, slots)}: the ghost items of a recipe; none in a closed book. */
	public void setupGhostRecipe(Object recipe, java.util.List<?> slots) {
	}

	/** The left edge of the screen next to a closed book: centered. */
	public int updateScreenPosition(int width, int imageWidth) {
		return (width - imageWidth) / 2;
	}

	public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
	}

	public void renderTooltip(GuiGraphics gfx, int left, int top, int mouseX, int mouseY) {
	}

	public void renderGhostRecipe(GuiGraphics gfx, int left, int top, boolean square, float partialTick) {
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return false;
	}

	public boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int width, int height, int button) {
		return true;
	}

	public boolean keyPressed(int key, int scanCode, int modifiers) {
		return false;
	}

	public boolean charTyped(char c, int modifiers) {
		return false;
	}

	public void setFocused(boolean focused) {
	}

	public boolean isFocused() {
		return false;
	}

	// --- 1.12.2, safe before setup ------------------------------------------------------------------------------------

	@Override
	public void func_194303_a(int width, int height, Minecraft minecraft, boolean wideEnough, InventoryCrafting grid) {
		super.func_194303_a(width, height, minecraft, wideEnough, grid);
		this.initialized = true;
	}

	@Override
	public boolean isVisible() {
		return this.initialized && super.isVisible();
	}

	@Override
	public void toggleVisibility() {
		if (this.initialized) {
			super.toggleVisibility();
		}
	}

	@Override
	public void slotClicked(@Nullable Slot slot) {
		if (this.initialized) {
			super.slotClicked(slot);
		}
	}

	@Override
	public void tick() {
		if (this.initialized) {
			super.tick();
		}
	}

	@Override
	public void recipesUpdated() {
		if (this.initialized) {
			super.recipesUpdated();
		}
	}

	@Override
	public void removed() {
		if (this.initialized) {
			super.removed();
		}
	}
}
