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
