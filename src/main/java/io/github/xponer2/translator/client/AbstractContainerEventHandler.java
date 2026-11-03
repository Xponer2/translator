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

/** 1.21 base of a listener with children: keeps the focused child and the dragging flag. */
public abstract class AbstractContainerEventHandler implements ContainerEventHandler {
	@Nullable
	private GuiEventListener focused;
	private boolean dragging;

	@Override
	public final boolean isDragging() {
		return this.dragging;
	}

	@Override
	public final void setDragging(boolean dragging) {
		this.dragging = dragging;
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
}
