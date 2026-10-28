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
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.text.ITextComponent;

/**
 * 1.21 widget on a 1.12.2 GuiButton, so any screen draws and clicks it through its button list. Subclasses draw in
 * renderWidget and react to clicks in onClick; `active` is copied to GuiButton.enabled before every frame.
 */
public abstract class AbstractWidget extends GuiButton {
	// GuiScreen#actionPerformed gets the button id; the widgets handle their own clicks, so ids only need to differ
	private static int nextId = 0x10000;

	public boolean active = true;
	protected ITextComponent message;

	public AbstractWidget(int x, int y, int width, int height, ITextComponent message) {
		super(nextId++, x, y, width, height, message.getFormattedText());
		this.message = message;
	}

	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		this.enabled = this.active;
		if (!this.visible) {
			return;
		}

		this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
		this.renderWidget(new GuiGraphics(mc), mouseX, mouseY, partialTicks);
	}

	protected abstract void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks);

	/** The vanilla 1.12.2 button look, for widgets that don't draw their own. */
	protected void renderDefaultButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		super.drawButton(mc, mouseX, mouseY, partialTicks);
	}

	@Override
	public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
		this.enabled = this.active;
		if (super.mousePressed(mc, mouseX, mouseY)) {
			this.onClick(mouseX, mouseY);
			return true;
		}

		return false;
	}

	public void onClick(double mouseX, double mouseY) {
	}

	protected void updateWidgetNarration(NarrationElementOutput output) {
	}

	public int getX() {
		return this.x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return this.y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	public boolean isHovered() {
		return this.hovered;
	}

	public boolean isHoveredOrFocused() {
		return this.hovered;
	}

	public boolean isActive() {
		return this.visible && this.active;
	}

	public ITextComponent getMessage() {
		return this.message;
	}

	public void setMessage(ITextComponent message) {
		this.message = message;
		this.displayString = message.getFormattedText();
	}
}
