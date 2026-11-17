/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.util.particle;

import net.minecraft.util.EnumParticleTypes;

/**
 * A 1.21 particle that 1.12.2 has: its 1.12.2 particle type with the int arguments. Sent with {@link Particles}.
 */
public class LegacyParticleOptions implements ParticleOptions, VanillaParticle {
	private final EnumParticleTypes type;
	private final int[] arguments;

	public LegacyParticleOptions(EnumParticleTypes type, int... arguments) {
		this.type = type;
		this.arguments = arguments;
	}

	public EnumParticleTypes getType() {
		return this.type;
	}

	public int[] getArguments() {
		return this.arguments;
	}

	@Override
	public EnumParticleTypes legacyType() {
		return this.type;
	}

	@Override
	public int[] legacyArguments() {
		return this.arguments;
	}
}
