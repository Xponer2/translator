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

import java.util.function.Supplier;

import io.github.xponer2.translator.block.BlockProperties;

import net.minecraft.block.BlockChest;

/**
 * Facade of the 1.21 {@code ChestBlock} (and the carried TrappedChestBlock on it): the 1.12.2 chest with
 * properties. 1.12.2 chests know two kinds (basic, trapped) instead of a block entity type; a mod's chest that gives
 * its own block entity keeps it through its newBlockEntity override.
 */
public class ChestBlock extends BlockChest {
	public ChestBlock(BlockProperties properties, Supplier<?> blockEntityType) {
		super(BlockChest.Type.BASIC);
		properties.applyTo(this, this::setSoundType);
	}
}
