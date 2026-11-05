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

import net.minecraft.block.BlockFence;

/** Facade of the 1.21 {@code FenceBlock}: the 1.12.2 fence, with properties. */
public class FenceBlock extends BlockFence {
	public FenceBlock(BlockProperties properties) {
		super(properties.material(), properties.mapColor());
		properties.applyTo(this, this::setSoundType);
	}

	/** The drops of the block's 1.21 loot table (when the mod has one), else 1.12.2's. */
	@Override
	public void getDrops(net.minecraft.util.NonNullList<net.minecraft.item.ItemStack> drops, net.minecraft.world.IBlockAccess world,
			net.minecraft.util.math.BlockPos pos, net.minecraft.block.state.IBlockState state, int fortune) {
		java.util.Random random = world instanceof net.minecraft.world.World level ? level.rand : RANDOM;
		if (!io.github.xponer2.translator.loot.BlockLoot.drop(io.github.xponer2.translator.loot.BlockLoot.tableOf(this, null), drops, world, pos, state,
				fortune, harvesters.get(), random)) {
			super.getDrops(drops, world, pos, state, fortune);
		}
	}
}
