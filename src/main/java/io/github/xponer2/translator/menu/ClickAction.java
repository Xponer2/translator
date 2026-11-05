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
package io.github.xponer2.translator.menu;

/**
 * 1.21 {@code ClickAction}: the mouse button of a click in a menu (left: the whole stack, right: one item or half).
 * The Translator's own, so that the 1.21 {@code Item} methods taking it ({@code overrideStackedOnOther},
 * {@code overrideOtherStackedOnMe}) are on the item facade and a mod's item calling super finds them.
 */
public enum ClickAction {
	PRIMARY,
	SECONDARY;

	/** The action of 1.12.2's mouse button (0 left, 1 right). */
	public static ClickAction of(int button) {
		return button == 0 ? PRIMARY : SECONDARY;
	}
}
