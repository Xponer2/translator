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

import io.github.xponer2.translator.util.particle.VanillaParticle;

import net.minecraft.util.EnumParticleTypes;

/** A vanilla 1.21 {@code SimpleParticleType} (ParticleTypes.FLAME, ...): spawned as its 1.12.2 particle. */
public final class VanillaParticleType extends SimpleParticleType implements VanillaParticle {
	private final EnumParticleTypes legacy;

	public VanillaParticleType(EnumParticleTypes legacy) {
		super(false);
		this.legacy = legacy;
	}

	@Override
	public EnumParticleTypes legacyType() {
		return this.legacy;
	}

	@Override
	public int[] legacyArguments() {
		return new int[0];
	}
}
