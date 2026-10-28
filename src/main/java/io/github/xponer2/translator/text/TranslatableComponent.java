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
