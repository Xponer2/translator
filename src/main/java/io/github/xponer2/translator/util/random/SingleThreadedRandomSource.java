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
package io.github.xponer2.translator.util.random;

/** 1.21 {@code SingleThreadedRandomSource}: the same generator as the legacy one (see {@link LegacyRandomSource}). */
public class SingleThreadedRandomSource extends LegacyRandomSource {
	public SingleThreadedRandomSource(long seed) {
		super(seed);
	}

	@Override
	public java.util.Random fork() {
		return new SingleThreadedRandomSource(this.nextLong());
	}
}
