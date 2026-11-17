/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.client;

/** 1.21 narrated screen element. 1.12.2 has no screen narration: the Translator never asks for it. */
public interface NarratableEntry {
	NarrationPriority narrationPriority();

	void updateNarration(NarrationElementOutput output);

	default boolean isActive() {
		return true;
	}

	enum NarrationPriority {
		NONE,
		HOVERED,
		FOCUSED;

		public boolean isTerminal() {
			return this == FOCUSED;
		}
	}
}
