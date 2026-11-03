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
package io.github.xponer2.translator.runtime.mc;

import io.github.xponer2.translator.util.particle.ParticleOptions;

/**
 * 1.21 {@code ParticleType} for original 1.21.1 mods (runtime): registered in the 1.21 particle type registry (a
 * Translator registry). The codecs stay the mod's own methods; drawing a mod's own particles isn't bridged yet.
 */
public abstract class ParticleType<T extends ParticleOptions> {
	private final boolean overrideLimiter;

	protected ParticleType(boolean overrideLimiter) {
		this.overrideLimiter = overrideLimiter;
	}

	public boolean getOverrideLimiter() {
		return this.overrideLimiter;
	}
}
