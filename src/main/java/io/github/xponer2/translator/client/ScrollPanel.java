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

import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;

/**
 * NeoForge scrolling panel: the subclass draws its content in drawPanel from a scrolled y; the panel clips it,
 * draws the background and the scroll bar and handles wheel, bar drags and clicks (clickPanel).
 */
public abstract class ScrollPanel extends AbstractContainerEventHandler implements Renderable, NarratableEntry {
	private final Minecraft client;
	protected final int width;
	protected final int height;
	protected final int top;
	protected final int bottom;
	protected final int right;
	protected final int left;
	private boolean scrolling;
	protected float scrollDistance;
	protected boolean captureMouse = true;
	protected final int border;

	private final int barWidth;
	private final int barLeft;
	private final int bgColorFrom;
	private final int bgColorTo;
	private final int barBgColor;
	private final int barColor;
	private final int barBorderColor;

	public ScrollPanel(Minecraft client, int width, int height, int top, int left) {
		this(client, width, height, top, left, 4);
	}

	public ScrollPanel(Minecraft client, int width, int height, int top, int left, int border) {
		this(client, width, height, top, left, border, 6);
	}

	public ScrollPanel(Minecraft client, int width, int height, int top, int left, int border, int barWidth) {
		this(client, width, height, top, left, border, barWidth, 0xC0101010, 0xD0101010);
	}

	public ScrollPanel(Minecraft client, int width, int height, int top, int left, int border, int barWidth, int bgColor) {
		this(client, width, height, top, left, border, barWidth, bgColor, bgColor);
	}

	public ScrollPanel(Minecraft client, int width, int height, int top, int left, int border, int barWidth, int bgColorFrom,
			int bgColorTo) {
		this(client, width, height, top, left, border, barWidth, bgColorFrom, bgColorTo, 0xFF000000, 0xFF808080, 0xFFC0C0C0);
	}

	public ScrollPanel(Minecraft client, int width, int height, int top, int left, int border, int barWidth, int bgColorFrom,
			int bgColorTo, int barBgColor, int barColor, int barBorderColor) {
		this.client = client;
		this.width = width;
		this.height = height;
		this.top = top;
		this.left = left;
		this.bottom = height + this.top;
		this.right = width + this.left;
		this.barLeft = this.left + this.width - barWidth;
		this.border = border;
		this.barWidth = barWidth;
		this.bgColorFrom = bgColorFrom;
		this.bgColorTo = bgColorTo;
		this.barBgColor = barBgColor;
		this.barColor = barColor;
		this.barBorderColor = barBorderColor;
	}

	protected abstract int getContentHeight();

	protected void drawBackground(GuiGraphics gfx, Tessellator tess, float partialTick) {
	}

	/** Draws the content; relativeY is the top of the content after scrolling. */
	protected abstract void drawPanel(GuiGraphics gfx, int entryRight, int relativeY, Tessellator tess, int mouseX, int mouseY);

	protected boolean clickPanel(double mouseX, double mouseY, int button) {
		return false;
	}

	private int getMaxScroll() {
		return this.getContentHeight() - (this.height - this.border);
	}

	private void applyScrollLimits() {
		int max = this.getMaxScroll();
		if (max < 0) {
			max /= 2;
		}
		if (this.scrollDistance < 0.0F) {
			this.scrollDistance = 0.0F;
		}
		if (this.scrollDistance > max) {
			this.scrollDistance = max;
		}
	}

	protected int getScrollAmount() {
		return 20;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0) {
			this.scrollDistance += (float) (-scrollY * this.getScrollAmount());
			this.applyScrollLimits();
			return true;
		}
		return false;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= this.left && mouseX <= this.left + this.width && mouseY >= this.top && mouseY <= this.bottom;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		this.scrolling = button == 0 && mouseX >= this.barLeft && mouseX < this.barLeft + this.barWidth;
		if (this.scrolling) {
			return true;
		}
		int mouseListY = (int) mouseY - this.top - this.getContentHeight() + (int) this.scrollDistance - this.border;
		if (mouseX >= this.left && mouseX <= this.right && mouseListY < 0) {
			return this.clickPanel(mouseX - this.left, mouseY - this.top + (int) this.scrollDistance - this.border, button);
		}
		return false;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (super.mouseReleased(mouseX, mouseY, button)) {
			return true;
		}
		boolean wasScrolling = this.scrolling;
		this.scrolling = false;
		return wasScrolling;
	}

	private int getBarHeight() {
		int barHeight = this.height * this.height / this.getContentHeight();
		if (barHeight < 32) {
			barHeight = 32;
		}
		if (barHeight > this.height - this.border * 2) {
			barHeight = this.height - this.border * 2;
		}
		return barHeight;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (this.scrolling) {
			int maxScroll = this.height - this.getBarHeight();
			double moved = dragY / maxScroll;
			this.scrollDistance += (float) (this.getMaxScroll() * moved);
			this.applyScrollLimits();
			return true;
		}
		return false;
	}

	@Override
	public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
		Tessellator tess = Tessellator.getInstance();
		gfx.enableScissor(this.left, this.top, this.right, this.bottom);
		this.drawGradientRect(gfx, this.left, this.top, this.right, this.bottom, this.bgColorFrom, this.bgColorTo);
		this.drawBackground(gfx, tess, partialTick);
		int baseY = this.top + this.border - (int) this.scrollDistance;
		this.drawPanel(gfx, this.right, baseY, tess, mouseX, mouseY);
		GlStateManager.disableDepth();
		int extraHeight = this.getContentHeight() + this.border - this.height;
		if (extraHeight > 0) {
			int barHeight = this.getBarHeight();
			int barTop = (int) this.scrollDistance * (this.height - barHeight) / extraHeight + this.top;
			if (barTop < this.top) {
				barTop = this.top;
			}
			gfx.fill(this.barLeft, this.top, this.barLeft + this.barWidth, this.bottom, this.barBgColor);
			gfx.fill(this.barLeft, barTop, this.barLeft + this.barWidth, barTop + barHeight, this.barColor);
			gfx.fill(this.barLeft, barTop, this.barLeft + this.barWidth - 1, barTop + barHeight - 1, this.barBorderColor);
		}
		GlStateManager.enableDepth();
		GlStateManager.disableBlend();
		gfx.disableScissor();
	}

	protected void drawGradientRect(GuiGraphics gfx, int left, int top, int right, int bottom, int colorFrom, int colorTo) {
		gfx.fillGradient(left, top, right, bottom, colorFrom, colorTo);
	}

	@Override
	public List<? extends GuiEventListener> children() {
		return Collections.emptyList();
	}

	@Override
	public NarrationPriority narrationPriority() {
		return NarrationPriority.NONE;
	}

	@Override
	public void updateNarration(NarrationElementOutput output) {
	}
}
