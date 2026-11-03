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
package io.github.xponer2.translator.runtime.redirect;

import javax.annotation.Nullable;

import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

/**
 * 1.21 {@code Style} is immutable: {@code Style.EMPTY} and its with* methods give new styles. 1.12.2's setters change
 * the style itself, so {@code Style.EMPTY.withColor(GRAY)} as setColor would color every later use of EMPTY (all of
 * AE2's tooltip styles were one object). The with* methods here copy first (neoforge-1.21.1-runtime.tsv keeps their
 * names; MiscBridges and ApiBridges have the other with* methods).
 */
public final class StyleStatics {
	public static final Style EMPTY = new Style();

	private StyleStatics() {
	}

	public static Style withColor(Style style, @Nullable TextFormatting color) {
		return style.createShallowCopy().setColor(color);
	}

	public static Style withBold(Style style, @Nullable Boolean value) {
		return style.createShallowCopy().setBold(value);
	}

	public static Style withItalic(Style style, @Nullable Boolean value) {
		return style.createShallowCopy().setItalic(value);
	}

	public static Style withClickEvent(Style style, @Nullable ClickEvent event) {
		return style.createShallowCopy().setClickEvent(event);
	}

	public static Style withHoverEvent(Style style, @Nullable HoverEvent event) {
		return style.createShallowCopy().setHoverEvent(event);
	}
}
