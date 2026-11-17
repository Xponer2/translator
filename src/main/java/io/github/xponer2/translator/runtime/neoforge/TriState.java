/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.runtime.neoforge;

/** NeoForge {@code TriState}: an event answer that can also be "not decided". */
public enum TriState {
	TRUE,
	DEFAULT,
	FALSE;

	public boolean isTrue() {
		return this == TRUE;
	}

	public boolean isFalse() {
		return this == FALSE;
	}

	public boolean isDefault() {
		return this == DEFAULT;
	}
}
