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
