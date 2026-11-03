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

import io.github.xponer2.translator.block.BlockProperties;

import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;

/** Facade of the 1.21 {@code StairBlock} (neoforge-1.21.1-supers.tsv): 1.12.2 stairs of a base state, with properties. */
public class StairBlock extends BlockStairs {
	public StairBlock(IBlockState base, BlockProperties properties) {
		super(base);
		properties.applyTo(this, this::setSoundType);
	}
}
