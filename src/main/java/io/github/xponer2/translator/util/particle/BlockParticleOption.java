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

import io.github.xponer2.translator.runtime.mc.VanillaOptionType;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumParticleTypes;

/**
 * Particles of a block state (1.21 {@code BlockParticleOption}, neoforge-1.21.1-runtime.tsv): the 1.12.2 block crack
 * (or dust, falling dust) particle of the state.
 */
public class BlockParticleOption extends LegacyParticleOptions {
	private final IBlockState state;

	public BlockParticleOption(io.github.xponer2.translator.runtime.mc.ParticleType<?> type, IBlockState state) {
		super(VanillaOptionType.legacyOf(type, EnumParticleTypes.BLOCK_CRACK), Block.getStateId(state));
		this.state = state;
	}

	public IBlockState getState() {
		return this.state;
	}
}
