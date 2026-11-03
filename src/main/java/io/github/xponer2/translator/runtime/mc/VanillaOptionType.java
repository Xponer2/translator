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

import net.minecraft.util.EnumParticleTypes;

/**
 * A vanilla particle type with options (1.21 {@code ParticleTypes.BLOCK}, {@code ITEM}, ...): the 1.12.2 particle the
 * options are shown as (block crack, item crack, ...).
 */
public final class VanillaOptionType extends ParticleType<ParticleOptions> {
	private final EnumParticleTypes legacy;

	public VanillaOptionType(EnumParticleTypes legacy) {
		super(false);
		this.legacy = legacy;
	}

	public EnumParticleTypes legacyType() {
		return this.legacy;
	}

	/** The 1.12.2 particle of a type (an option type's own, ITEM_CRACK for one that isn't vanilla). */
	public static EnumParticleTypes legacyOf(Object type, EnumParticleTypes fallback) {
		return type instanceof VanillaOptionType vanilla ? vanilla.legacy : fallback;
	}
}
