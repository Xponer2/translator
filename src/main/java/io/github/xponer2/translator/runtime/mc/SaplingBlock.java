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

import net.minecraft.block.BlockBush;

/** Facade of the 1.21 {@code SaplingBlock}: a plant on soil, with properties; the 1.21 tree grower doesn't grow it. */
public class SaplingBlock extends BlockBush {
	public static final net.minecraft.block.properties.PropertyInteger STAGE = net.minecraft.block.properties.PropertyInteger.create("stage", 0, 1);

	public SaplingBlock(Object treeGrower, BlockProperties properties) {
		super(properties.material(), properties.mapColor());
		properties.applyTo(this, this::setSoundType);
		this.setDefaultState(this.blockState.getBaseState().withProperty(STAGE, 0));
	}

	@Override
	protected net.minecraft.block.state.BlockStateContainer createBlockState() {
		return new net.minecraft.block.state.BlockStateContainer(this, STAGE);
	}

	@Override
	@SuppressWarnings("deprecation")
	public net.minecraft.block.state.IBlockState getStateFromMeta(int meta) {
		return this.getDefaultState().withProperty(STAGE, meta & 1);
	}

	@Override
	public int getMetaFromState(net.minecraft.block.state.IBlockState state) {
		return state.getValue(STAGE);
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
