/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.util.particle;

import net.minecraft.util.EnumParticleTypes;

/** A 1.21 particle that is a 1.12.2 particle: what {@link Particles} spawns for it. */
public interface VanillaParticle {
	EnumParticleTypes legacyType();

	int[] legacyArguments();
}
