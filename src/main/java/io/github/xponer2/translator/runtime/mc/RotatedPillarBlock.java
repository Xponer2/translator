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
import io.github.xponer2.translator.block.BlockStates;

import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;

/**
 * Facade of the 1.21 {@code RotatedPillarBlock} (logs, pillars): the 1.12.2 one, with properties; a subclass adds its
 * own block state properties in createBlockStateDefinition after the axis (super's), as in 1.21.
 */
public class RotatedPillarBlock extends BlockRotatedPillar {
	public RotatedPillarBlock(BlockProperties properties) {
		super(properties.material(), properties.mapColor());
		properties.applyTo(this, this::setSoundType);
	}

	protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
		builder.add(AXIS);
	}

	@Override
	protected BlockStateContainer createBlockState() {
		var builder = new io.github.xponer2.translator.block.LazyStateContainer.Builder(this);
		this.createBlockStateDefinition(builder);
		return builder.build();
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return BlockStates.toMeta(state);
	}

	@Override
	@SuppressWarnings("deprecation")
	public IBlockState getStateFromMeta(int meta) {
		return BlockStates.fromMeta(this, meta);
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
