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
 * renderWidget and react to clicks in onClick, drags in onDrag and the release in onRelease; `active` is copied to
 * GuiButton.enabled before every frame.
 */
public abstract class AbstractWidget extends GuiButton implements GuiEventListener, Renderable, NarratableEntry {
	/** NeoForge: no text color set, the widget picks one by its state. */
	public static final int UNSET_FG_COLOR = -1;
	// GuiScreen#actionPerformed gets the button id; the widgets handle their own clicks, so ids only need to differ
	private static int nextId = 0x10000;

	public boolean active = true;
	protected ITextComponent message;
	protected boolean isHovered;
	protected int packedFGColor = UNSET_FG_COLOR;
	private boolean focused;
	// 1.12.2 tells a button only the press and the release: the drag is seen in the frames between them
	private boolean dragging;
	private int dragX;
	private int dragY;
	@javax.annotation.Nullable
	private Tooltip tooltip;
	private long tooltipDelayMs;
	private long hoveredSince;

	public AbstractWidget(int x, int y, int width, int height, ITextComponent message) {
		super(nextId++, x, y, width, height, message.getFormattedText());
		this.message = message;
	}

	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		this.render(new GuiGraphics(mc), mouseX, mouseY, partialTicks);
	}

	@Override
	public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
		this.enabled = this.active;
		if (!this.visible) {
			return;
		}

		boolean wasHovered = this.hovered;
		this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
		this.isHovered = this.hovered;
		if (this.dragging && (mouseX != this.dragX || mouseY != this.dragY)) {
			this.onDrag(mouseX, mouseY, mouseX - this.dragX, mouseY - this.dragY);
			this.dragX = mouseX;
			this.dragY = mouseY;
		}
		this.renderWidget(gfx, mouseX, mouseY, partialTicks);
		if (this.hovered && !wasHovered) {
			this.hoveredSince = System.currentTimeMillis();
		}
		if (this.tooltip != null && this.hovered && System.currentTimeMillis() - this.hoveredSince >= this.tooltipDelayMs) {
			Tooltip.show(this.tooltip, mouseX, mouseY);
		}
	}

	/** 1.21 widget tooltip: shown while the mouse is over the widget (after the delay). */
	public void setTooltip(@javax.annotation.Nullable Tooltip tooltip) {
		this.tooltip = tooltip;
	}

	@javax.annotation.Nullable
	public Tooltip getTooltip() {
		return this.tooltip;
	}

	public void setTooltipDelay(java.time.Duration delay) {
		this.tooltipDelayMs = delay.toMillis();
	}

	protected abstract void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks);

	/** The vanilla 1.12.2 button look, for widgets that don't draw their own. */
	protected void renderDefaultButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		super.drawButton(mc, mouseX, mouseY, partialTicks);
	}

	/** 1.21 {@code playDownSound(soundManager)}: the click of a button. */
	public void playDownSound(net.minecraft.client.audio.SoundHandler sounds) {
		this.playPressSound(sounds);
	}

	@Override
	public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
		this.enabled = this.active;
		if (super.mousePressed(mc, mouseX, mouseY)) {
			this.onClick(mouseX, mouseY);
			this.dragging = true;
			this.dragX = mouseX;
			this.dragY = mouseY;
			return true;
		}

		return false;
	}

	@Override
	public void mouseReleased(int mouseX, int mouseY) {
		if (this.dragging) {
			this.dragging = false;
			this.onRelease(mouseX, mouseY);
		}
	}

	public void onClick(double mouseX, double mouseY) {
	}

	public void onRelease(double mouseX, double mouseY) {
	}

	protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
	}

	// 1.21 input, for a mod that calls it on a widget itself (screens click widgets through the 1.12.2 button list)

	protected boolean clicked(double mouseX, double mouseY) {
		return this.active && this.visible && mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width
				&& mouseY < this.y + this.height;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && this.clicked(mouseX, mouseY)) {
			this.playPressSound(Minecraft.getMinecraft().getSoundHandler());
			this.onClick(mouseX, mouseY);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0) {
			this.onRelease(mouseX, mouseY);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (button == 0) {
			this.onDrag(mouseX, mouseY, dragX, dragY);
			return true;
		}
		return false;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return this.clicked(mouseX, mouseY);
	}

	@Override
	public void setFocused(boolean focused) {
		this.focused = focused;
	}

	@Override
	public boolean isFocused() {
		return this.focused;
	}

	public int getFGColor() {
		return this.packedFGColor != UNSET_FG_COLOR ? this.packedFGColor : this.active ? 0xFFFFFF : 0xA0A0A0;
	}

	public void setFGColor(int color) {
		this.packedFGColor = color;
	}

	public void clearFGColor() {
		this.packedFGColor = UNSET_FG_COLOR;
	}

	@Override
	public NarrationPriority narrationPriority() {
		return this.focused ? NarrationPriority.FOCUSED : this.isHovered ? NarrationPriority.HOVERED : NarrationPriority.NONE;
	}

	@Override
	public final void updateNarration(NarrationElementOutput output) {
		this.updateWidgetNarration(output);
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
