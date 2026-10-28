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
package io.github.xponer2.translator.util.particle;

import net.minecraft.util.EnumParticleTypes;

/**
 * 1.21 particle (type + options) as a 1.12.2 particle type with its int arguments. Sent with {@link Particles}.
 */
public class ParticleOptions {
	private final EnumParticleTypes type;
	private final int[] arguments;

	public ParticleOptions(EnumParticleTypes type, int... arguments) {
		this.type = type;
		this.arguments = arguments;
	}

	public EnumParticleTypes getType() {
		return this.type;
	}

	public int[] getArguments() {
		return this.arguments;
	}
}
