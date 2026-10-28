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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.text.ITextComponent;

/**
 * 1.21 container screen on the 1.12.2 GuiContainer: the 1.12.2 draw calls go to the 1.21 render, renderBg and
 * renderLabels with a GuiGraphics, and the 1.21 fields (menu, title, font, minecraft, label positions) are kept.
 */
public abstract class AbstractContainerScreen<T extends Container> extends GuiContainer {
	protected final T menu;
	protected final ITextComponent title;
	protected final InventoryPlayer playerInventory;
	protected final ITextComponent playerInventoryTitle;
	protected FontRenderer font;
	protected Minecraft minecraft;
	protected int titleLabelX = 8;
	protected int titleLabelY = 6;
	protected int inventoryLabelX = 8;
	protected int inventoryLabelY;

	public AbstractContainerScreen(T menu, InventoryPlayer playerInventory, ITextComponent title) {
		super(menu);
		this.menu = menu;
		this.title = title;
		this.playerInventory = playerInventory;
		this.playerInventoryTitle = playerInventory.getDisplayName();
		this.inventoryLabelY = this.ySize - 94;
	}

	@Override
	public void initGui() {
		super.initGui();
		this.font = this.fontRenderer;
		this.minecraft = this.mc;
		this.inventoryLabelY = this.ySize - 94;
		this.init();
	}

	/** 1.21 init, after the 1.12.2 one has placed the screen. */
	protected void init() {
	}

	@Override
	public final void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.render(new GuiGraphics(this.mc), mouseX, mouseY, partialTicks);
	}

	/** Draws background, slots, widgets and labels; 1.21 subclasses call it as super.render. */
	public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
	}

	protected void renderTooltip(GuiGraphics gfx, int mouseX, int mouseY) {
		this.renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected final void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		this.renderBg(new GuiGraphics(this.mc), partialTicks, mouseX, mouseY);
	}

	protected abstract void renderBg(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY);

	@Override
	protected final void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		this.renderLabels(new GuiGraphics(this.mc), mouseX, mouseY);
	}

	protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
		gfx.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
		gfx.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
	}

	protected <W extends GuiButton> W addRenderableWidget(W widget) {
		this.buttonList.add(widget);
		return widget;
	}

	/** The white highlight of a hovered slot. */
	public static void renderSlotHighlight(GuiGraphics gfx, int x, int y, int z) {
		net.minecraft.client.renderer.GlStateManager.disableLighting();
		net.minecraft.client.renderer.GlStateManager.disableDepth();
		gfx.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
		net.minecraft.client.renderer.GlStateManager.enableDepth();
	}

	protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
		return this.isPointInRegion(x, y, width, height, (int) mouseX, (int) mouseY);
	}

	public T getMenu() {
		return this.menu;
	}

	public ITextComponent getTitle() {
		return this.title;
	}
}
