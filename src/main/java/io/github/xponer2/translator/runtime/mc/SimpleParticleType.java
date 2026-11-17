/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.runtime.mc;

import io.github.xponer2.translator.util.particle.ParticleOptions;

/** 1.21 {@code SimpleParticleType}: a particle type that is its own options (runtime). */
public class SimpleParticleType extends ParticleType<SimpleParticleType> implements ParticleOptions {
	public SimpleParticleType(boolean overrideLimiter) {
		super(overrideLimiter);
	}

	public SimpleParticleType getType() {
		return this;
	}
}
