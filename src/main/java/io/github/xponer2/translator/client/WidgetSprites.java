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

import net.minecraft.util.ResourceLocation;

/** 1.21 sprites of a widget by state (drawn with GuiGraphics.blitSprite). */
public final class WidgetSprites {
	private final ResourceLocation enabled;
	private final ResourceLocation disabled;
	private final ResourceLocation enabledFocused;
	private final ResourceLocation disabledFocused;

	public WidgetSprites(ResourceLocation enabled, ResourceLocation disabled, ResourceLocation enabledFocused,
			ResourceLocation disabledFocused) {
		this.enabled = enabled;
		this.disabled = disabled;
		this.enabledFocused = enabledFocused;
		this.disabledFocused = disabledFocused;
	}

	public WidgetSprites(ResourceLocation enabled, ResourceLocation enabledFocused) {
		this(enabled, enabled, enabledFocused, enabledFocused);
	}

	public WidgetSprites(ResourceLocation enabled, ResourceLocation disabled, ResourceLocation enabledFocused) {
		this(enabled, disabled, enabledFocused, disabled);
	}

	public ResourceLocation get(boolean enabled, boolean focused) {
		if (enabled) {
			return focused ? this.enabledFocused : this.enabled;
		}
		return focused ? this.disabledFocused : this.disabled;
	}

	public ResourceLocation enabled() {
		return this.enabled;
	}

	public ResourceLocation disabled() {
		return this.disabled;
	}

	public ResourceLocation enabledFocused() {
		return this.enabledFocused;
	}

	public ResourceLocation disabledFocused() {
		return this.disabledFocused;
	}
}
