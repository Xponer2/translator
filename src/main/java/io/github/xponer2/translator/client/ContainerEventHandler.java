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

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

/** 1.21 listener with children: mouse events go to the child under the mouse, drags and keys to the focused one. */
public interface ContainerEventHandler extends GuiEventListener {
	List<? extends GuiEventListener> children();

	boolean isDragging();

	void setDragging(boolean dragging);

	@Nullable
	GuiEventListener getFocused();

	void setFocused(@Nullable GuiEventListener focused);

	default Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
		for (GuiEventListener child : this.children()) {
			if (child.isMouseOver(mouseX, mouseY)) {
				return Optional.of(child);
			}
		}
		return Optional.empty();
	}

	@Override
	default boolean mouseClicked(double mouseX, double mouseY, int button) {
		for (GuiEventListener child : this.children()) {
			if (child.mouseClicked(mouseX, mouseY, button)) {
				this.setFocused(child);
				if (button == 0) {
					this.setDragging(true);
				}
				return true;
			}
		}
		return false;
	}

	@Override
	default boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0 && this.isDragging()) {
			this.setDragging(false);
			if (this.getFocused() != null) {
				return this.getFocused().mouseReleased(mouseX, mouseY, button);
			}
		}
		return this.getChildAt(mouseX, mouseY).filter(child -> child.mouseReleased(mouseX, mouseY, button)).isPresent();
	}

	@Override
	default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		return this.getFocused() != null && this.isDragging() && button == 0
				&& this.getFocused().mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	default boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		return this.getChildAt(mouseX, mouseY).filter(child -> child.mouseScrolled(mouseX, mouseY, scrollX, scrollY)).isPresent();
	}

	@Override
	default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return this.getFocused() != null && this.getFocused().keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	default boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		return this.getFocused() != null && this.getFocused().keyReleased(keyCode, scanCode, modifiers);
	}

	@Override
	default boolean charTyped(char codePoint, int modifiers) {
		return this.getFocused() != null && this.getFocused().charTyped(codePoint, modifiers);
	}

	/** 1.21 Tab and arrow navigation between the children: not done here (see GuiEventListener.nextFocusPath). */
	@Nullable
	default <R> R handleTabNavigation(Object event) {
		return null;
	}

	@Nullable
	default <R> R handleArrowNavigation(Object event) {
		return null;
	}

	@Override
	default void setFocused(boolean focused) {
	}

	@Override
	default boolean isFocused() {
		return this.getFocused() != null;
	}
}
