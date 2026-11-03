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

/**
 * 1.21 input target of a screen (widgets, panels, the screen itself). The Translator screens hand 1.12.2 mouse and
 * key events to it with 1.21 values: doubles for the mouse, GLFW key codes.
 */
public interface GuiEventListener {
	default void mouseMoved(double mouseX, double mouseY) {
	}

	default boolean mouseClicked(double mouseX, double mouseY, int button) {
		return false;
	}

	default boolean mouseReleased(double mouseX, double mouseY, int button) {
		return false;
	}

	default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		return false;
	}

	default boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		return false;
	}

	default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return false;
	}

	default boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		return false;
	}

	default boolean charTyped(char codePoint, int modifiers) {
		return false;
	}

	default boolean isMouseOver(double mouseX, double mouseY) {
		return false;
	}

	void setFocused(boolean focused);

	boolean isFocused();
}
