/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.client;

import net.minecraft.util.text.ITextComponent;

/**
 * Where a widget tells the screen reader what it is (1.21 narration): 1.12.2 has no narration of widgets, what is
 * added goes nowhere. The element type is NarratedElementType (carried with the API).
 */
public interface NarrationElementOutput {
	NarrationElementOutput NONE = new NarrationElementOutput() {
	};

	default void add(Object type, ITextComponent text) {
	}

	default void add(Object type, ITextComponent... texts) {
	}

	default void add(Object type, String text) {
	}

	default NarrationElementOutput nest() {
		return this;
	}
}
