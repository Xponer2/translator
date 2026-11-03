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

import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockPlanks;

/**
 * Facade of the 1.21 {@code FenceGateBlock}: the 1.12.2 gate, with properties. The 1.21 wood type and sounds (types
 * 1.12.2 doesn't have) are taken as they are; the gate opens with the 1.12.2 sounds.
 */
public class FenceGateBlock extends BlockFenceGate {
	public FenceGateBlock(Object woodType, BlockProperties properties) {
		super(BlockPlanks.EnumType.OAK);
		properties.applyTo(this, this::setSoundType);
	}

	public FenceGateBlock(BlockProperties properties, Object openSound, Object closeSound) {
		this(null, properties);
	}

	public FenceGateBlock(Object woodType, BlockProperties properties, Object openSound, Object closeSound) {
		this(woodType, properties);
	}
}
