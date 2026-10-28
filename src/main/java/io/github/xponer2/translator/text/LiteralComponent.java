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
