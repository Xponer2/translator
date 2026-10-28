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
package io.github.xponer2.translator.util;

/**
 * 1.21 feature flags (experimental content toggles). 1.12.2 has none: every feature is enabled.
 */
public final class FeatureFlagSet {
	private static final FeatureFlagSet ALL = new FeatureFlagSet();

	private FeatureFlagSet() {
	}

	public static FeatureFlagSet of(Object... flags) {
		return ALL;
	}

	public boolean contains(Object flag) {
		return true;
	}
}
