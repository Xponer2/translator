/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.util.random;

/** 1.21 {@code ThreadSafeLegacyRandomSource}: java.util.Random is thread-safe already (see {@link LegacyRandomSource}). */
public class ThreadSafeLegacyRandomSource extends LegacyRandomSource {
	public ThreadSafeLegacyRandomSource(long seed) {
		super(seed);
	}

	@Override
	public java.util.Random fork() {
		return new ThreadSafeLegacyRandomSource(this.nextLong());
	}
}
