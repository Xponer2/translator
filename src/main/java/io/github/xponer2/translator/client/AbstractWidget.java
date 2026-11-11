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
		// the bounds themselves, as 1.21: a mod's clicked may ask isMouseOver (Create's AbstractSimiWidget), and through
		// clicked the two called each other until the stack overflowed (Create's toolbox screen)
		return this.active && this.visible && mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width
				&& mouseY < this.y + this.height;
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

	public void setWidth(int width) {
		this.width = width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
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

	// --- the rest of the 1.21 widget API (JourneyMap's screens, layouts and scrolling labels use it)

	public static final double PERIOD_PER_SCROLLED_PIXEL = 0.5D;
	public static final double MIN_SCROLL_PERIOD = 3.0D;

	/** 1.21 widget opacity (faded in title screens); the 1.12.2 drawing keeps the widget opaque. */
	protected float alpha = 1.0F;
	private int tabOrderGroup;

	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}

	public int getRight() {
		return this.x + this.width;
	}

	public int getBottom() {
		return this.y + this.height;
	}

	/** 1.21 {@code setRectangle(width, height, x, y)}. */
	public void setRectangle(int width, int height, int x, int y) {
		this.setSize(width, height);
		this.setPosition(x, y);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> R getRectangle() {
		return (R) ScreenRectangles.of(this.x, this.y, this.width, this.height);
	}

	public int getTabOrderGroup() {
		return this.tabOrderGroup;
	}

	public void setTabOrderGroup(int group) {
		this.tabOrderGroup = group;
	}

	protected boolean isValidClickButton(int button) {
		return button == 0;
	}

	public void visitWidgets(java.util.function.Consumer<AbstractWidget> visitor) {
		visitor.accept(this);
	}

	protected ITextComponent createNarrationMessage() {
		return wrapDefaultNarrationMessage(this.getMessage());
	}

	public static ITextComponent wrapDefaultNarrationMessage(ITextComponent message) {
		return new net.minecraft.util.text.TextComponentTranslation("gui.narrate.button", message);
	}

	protected void defaultButtonNarrationText(NarrationElementOutput output) {
	}

	/** The message, scrolling back and forth when it is wider than the widget (1.21 long button labels). */
	protected void renderScrollingString(GuiGraphics gfx, net.minecraft.client.gui.FontRenderer font, int margin, int color) {
		renderScrollingString(gfx, font, this.getMessage(), this.x + margin, this.y, this.x + this.width - margin, this.y + this.height, color);
	}

	public static void renderScrollingString(GuiGraphics gfx, net.minecraft.client.gui.FontRenderer font, ITextComponent text, int minX, int minY,
			int maxX, int maxY, int color) {
		renderScrollingString(gfx, font, text, (minX + maxX) / 2, minX, minY, maxX, maxY, color);
	}

	public static void renderScrollingString(GuiGraphics gfx, net.minecraft.client.gui.FontRenderer font, ITextComponent text, int centerX, int minX,
			int minY, int maxX, int maxY, int color) {
		String string = text.getFormattedText();
		int textWidth = font.getStringWidth(string);
		int y = (minY + maxY - 9) / 2 + 1;
		int room = maxX - minX;
		if (textWidth > room) {
			int overflow = textWidth - room;
			double seconds = System.currentTimeMillis() / 1000.0D;
			double period = Math.max(overflow * PERIOD_PER_SCROLLED_PIXEL, MIN_SCROLL_PERIOD);
			double phase = Math.sin(Math.PI / 2 * Math.cos(Math.PI * 2 * seconds / period)) / 2.0D + 0.5D;
			gfx.enableScissor(minX, minY, maxX, maxY);
			gfx.drawString(font, string, minX - (int) (phase * overflow), y, color);
			gfx.disableScissor();
		} else {
			int center = Math.max(minX + textWidth / 2, Math.min(centerX, maxX - textWidth / 2));
			gfx.drawCenteredString(font, string, center, y, color);
		}
	}

	public void setMessage(ITextComponent message) {
		this.message = message;
		this.displayString = message.getFormattedText();
	}
}
