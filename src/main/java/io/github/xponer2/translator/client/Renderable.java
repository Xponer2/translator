/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.client;

/** 1.21 thing a screen draws every frame (its renderables). */
public interface Renderable {
	void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick);
}
