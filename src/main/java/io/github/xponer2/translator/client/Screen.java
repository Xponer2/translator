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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import io.github.xponer2.translator.runtime.neoforge.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;

/**
 * 1.21 screen on the 1.12.2 GuiScreen: the 1.12.2 calls go to the 1.21 init, render, tick, removed and input
 * methods. Widgets (GuiButtons) are drawn as renderables and clicked through the 1.12.2 button list; other
 * listeners (panels) get the mouse from the 1.21 input methods. Keys are GLFW codes, like in 1.21.
 */
public abstract class Screen extends GuiScreen implements ContainerEventHandler, Renderable {
	private static final int GLFW_KEY_ESCAPE = 256;

	protected final ITextComponent title;
	protected Minecraft minecraft;
	protected FontRenderer font;
	protected final List<Renderable> renderables = new ArrayList<>();
	private final List<GuiEventListener> children = new ArrayList<>();
	@Nullable
	private GuiEventListener focused;
	private boolean dragging;
	private int dragX;
	private int dragY;

	protected Screen(ITextComponent title) {
		this.title = title;
	}

	@Override
	public void initGui() {
		this.minecraft = this.mc;
		this.font = this.fontRenderer;
		this.clearWidgets();
		this.init();
	}

	/** 1.21 init: adds the widgets; runs again when the window is resized. */
	protected void init() {
	}

	@Override
	public final void drawScreen(int mouseX, int mouseY, float partialTicks) {
		try {
			this.render(new GuiGraphics(this.mc), mouseX, mouseY, partialTicks);
		} catch (RuntimeException | LinkageError e) {
			ScreenFailures.closeBroken(this, e);
		}
	}

	@Override
	public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(gfx, mouseX, mouseY, partialTick);
		for (Renderable renderable : new ArrayList<>(this.renderables)) {
			renderable.render(gfx, mouseX, mouseY, partialTick);
		}
	}

	public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
		this.drawDefaultBackground();
	}

	// --- widgets

	protected <T extends GuiEventListener> T addRenderableWidget(T widget) {
		if (widget instanceof Renderable) {
			this.renderables.add((Renderable) widget);
		}
		return this.addWidget(widget);
	}

	protected <T extends Renderable> T addRenderableOnly(T renderable) {
		this.renderables.add(renderable);
		return renderable;
	}

	protected <T extends GuiEventListener> T addWidget(T listener) {
		if (listener instanceof GuiButton) {
			this.buttonList.add((GuiButton) listener);
		}
		this.children.add(listener);
		return listener;
	}

	protected void removeWidget(GuiEventListener listener) {
		this.renderables.remove(listener);
		this.buttonList.remove(listener);
		this.children.remove(listener);
	}

	protected void clearWidgets() {
		this.renderables.clear();
		this.children.clear();
		this.buttonList.clear();
	}

	@Override
	public List<? extends GuiEventListener> children() {
		return this.children;
	}

	@Nullable
	@Override
	public GuiEventListener getFocused() {
		return this.focused;
	}

	@Override
	public void setFocused(@Nullable GuiEventListener focused) {
		if (this.focused != null) {
			this.focused.setFocused(false);
		}
		if (focused != null) {
			focused.setFocused(true);
		}
		this.focused = focused;
	}

	@Override
	public boolean isDragging() {
		return this.dragging;
	}

	@Override
	public void setDragging(boolean dragging) {
		this.dragging = dragging;
	}

	// --- life cycle

	public void tick() {
	}

	@Override
	public void updateScreen() {
		try {
			this.tick();
		} catch (RuntimeException | LinkageError e) {
			ScreenFailures.closeBroken(this, "tick", e);
		}
	}

	@Override
	public void handleInput() throws java.io.IOException {
		try {
			super.handleInput();
		} catch (RuntimeException | LinkageError e) {
			ScreenFailures.closeBroken(this, "input", e);
		}
	}

	/** The screen was closed or replaced. */
	public void removed() {
	}

	@Override
	public void onGuiClosed() {
		ScreenFailures.removed(this);
	}

	public void onClose() {
		this.mc.displayGuiScreen(null);
	}

	public boolean shouldCloseOnEsc() {
		return true;
	}

	public boolean isPauseScreen() {
		return true;
	}

	@Override
	public boolean doesGuiPauseGame() {
		return this.isPauseScreen();
	}

	public ITextComponent getTitle() {
		return this.title;
	}

	public Minecraft getMinecraft() {
		return this.mc;
	}

	/** 1.21 {@code renderTransparentBackground(gfx)}: the dark gradient behind a screen (JourneyMap's popups draw it). */
	public void renderTransparentBackground(GuiGraphics gfx) {
		this.drawDefaultBackground();
	}

	public static boolean hasShiftDown() {
		return isShiftKeyDown();
	}

	public static boolean hasControlDown() {
		return isCtrlKeyDown();
	}

	public static boolean hasAltDown() {
		return isAltKeyDown();
	}

	/** GLFW modifier bits of the keys held now (shift 1, control 2, alt 4). */
	static int modifiers() {
		return (isShiftKeyDown() ? 1 : 0) | (isCtrlKeyDown() ? 2 : 0) | (isAltKeyDown() ? 4 : 0);
	}

	// --- input: the 1.12.2 handlers call the 1.21 ones (which a subclass overrides); their defaults give the event
	// to the listeners that aren't buttons, then to the 1.12.2 handling (buttons).

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		for (GuiEventListener child : new ArrayList<>(this.children)) {
			if (!(child instanceof GuiButton) && child.mouseClicked(mouseX, mouseY, button)) {
				this.setFocused(child);
				if (button == 0) {
					this.setDragging(true);
				}
				return true;
			}
		}
		try {
			super.mouseClicked((int) mouseX, (int) mouseY, button);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		for (GuiButton widget : this.buttonList) {
			if (widget.visible && widget.enabled && mouseX >= widget.x && mouseY >= widget.y && mouseX < widget.x + widget.width
					&& mouseY < widget.y + widget.height) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
		this.dragX = mouseX;
		this.dragY = mouseY;
		this.mouseClicked((double) mouseX, (double) mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		boolean handled = false;
		if (button == 0 && this.dragging) {
			this.dragging = false;
			if (this.focused != null && !(this.focused instanceof GuiButton)) {
				handled = this.focused.mouseReleased(mouseX, mouseY, button);
			}
		}
		super.mouseReleased((int) mouseX, (int) mouseY, button);
		return handled;
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		this.mouseReleased((double) mouseX, (double) mouseY, state);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		return this.focused != null && !(this.focused instanceof GuiButton) && this.dragging && button == 0
				&& this.focused.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
		this.mouseDragged(mouseX, mouseY, button, mouseX - this.dragX, mouseY - this.dragY);
		this.dragX = mouseX;
		this.dragY = mouseY;
	}

	/** Wheel up is a positive scrollY (1.12.2 has no horizontal wheel). */
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		return ContainerEventHandler.super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		int wheel = org.lwjgl.input.Mouse.getEventDWheel();
		if (wheel != 0) {
			double mouseX = org.lwjgl.input.Mouse.getEventX() * this.width / (double) this.mc.displayWidth;
			double mouseY = this.height - org.lwjgl.input.Mouse.getEventY() * this.height / (double) this.mc.displayHeight - 1;
			this.mouseScrolled(mouseX, mouseY, 0.0D, wheel > 0 ? 1.0D : -1.0D);
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW_KEY_ESCAPE && this.shouldCloseOnEsc()) {
			this.onClose();
			return true;
		}
		return ContainerEventHandler.super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		// Escape closes the screen in keyPressed (1.21), not in the 1.12.2 keyTyped
		if (!this.keyPressed(KeyMapping.toGlfw(keyCode), keyCode, modifiers()) && typedChar >= ' ') {
			this.charTyped(typedChar, modifiers());
		}
	}
}
