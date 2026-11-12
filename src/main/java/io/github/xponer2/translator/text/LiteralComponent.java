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
package io.github.xponer2.translator.text;

import net.minecraft.util.text.TextComponentString;

/**
 * 1.21 {@code Component.literal(text)}.
 */
public class LiteralComponent extends TextComponentString implements MutableComponent {
	/**
	 * A copy of the style: 1.21 styles are values, a 1.12.2 one is mutable and appendSibling sets its parent. A shared
	 * one (Style.EMPTY, a mod's style constant) held by two components appended to each other became its own ancestor,
	 * and every getBold/getColor of the text recursed until the stack overflowed (Iron's Spells' scroll names).
	 */
	@Override
	public LiteralComponent setStyle(net.minecraft.util.text.Style style) {
		super.setStyle(style == null ? null : style.createShallowCopy());
		return this;
	}

	public LiteralComponent(String text) {
		super(text);
	}

	@Override
	public LiteralComponent createCopy() {
		var copy = new LiteralComponent(this.getText());
		copy.setStyle(this.getStyle().createShallowCopy());
		for (var sibling : this.getSiblings()) {
			copy.appendSibling(sibling.createCopy());
		}

		return copy;
	}
}
