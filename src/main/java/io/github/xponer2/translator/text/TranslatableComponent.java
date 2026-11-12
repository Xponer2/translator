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

import net.minecraft.util.text.TextComponentTranslation;

/**
 * 1.21 {@code Component.translatable(key, args)}.
 */
public class TranslatableComponent extends TextComponentTranslation implements MutableComponent {
	/**
	 * A copy of the style: 1.21 styles are values, a 1.12.2 one is mutable and appendSibling sets its parent. A shared
	 * one (Style.EMPTY, a mod's style constant) held by two components appended to each other became its own ancestor,
	 * and every getBold/getColor of the text recursed until the stack overflowed (Iron's Spells' scroll names).
	 */
	@Override
	public TranslatableComponent setStyle(net.minecraft.util.text.Style style) {
		super.setStyle(style == null ? null : style.createShallowCopy());
		return this;
	}

	public TranslatableComponent(String key, Object... args) {
		super(key, args);
	}

	@Override
	public TranslatableComponent createCopy() {
		var args = this.getFormatArgs();
		var copiedArgs = new Object[args.length];
		for (int i = 0; i < args.length; i++) {
			copiedArgs[i] = args[i] instanceof net.minecraft.util.text.ITextComponent component ? component.createCopy() : args[i];
		}

		var copy = new TranslatableComponent(this.getKey(), copiedArgs);
		copy.setStyle(this.getStyle().createShallowCopy());
		for (var sibling : this.getSiblings()) {
			copy.appendSibling(sibling.createCopy());
		}

		return copy;
	}
}
