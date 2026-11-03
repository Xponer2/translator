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

import net.minecraft.block.BlockRailBase;

/**
 * Facade of the 1.21 {@code BaseRailBlock}: the 1.12.2 rail base (its own material), with properties. The mod's rail
 * gives the shape property (1.21 RailShape = 1.12.2 EnumRailDirection).
 */
public abstract class BaseRailBlock extends BlockRailBase {
	protected BaseRailBlock(boolean isStraight, BlockProperties properties) {
		super(isStraight);
		properties.applyTo(this, this::setSoundType);
	}
}
