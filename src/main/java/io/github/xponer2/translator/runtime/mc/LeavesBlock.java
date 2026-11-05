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

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Facade of the 1.21 {@code LeavesBlock}: a leaves block (see-through, drawn cut out, faces between leaves drawn as
 * fancy graphics do); it doesn't decay.
 */
public class LeavesBlock extends Block {
	public static final net.minecraft.block.properties.PropertyInteger DISTANCE = io.github.xponer2.translator.block.BlockStateProperties.DISTANCE;
	public static final net.minecraft.block.properties.PropertyBool PERSISTENT = io.github.xponer2.translator.block.BlockStateProperties.PERSISTENT;
	/** 1.21 leaves can be waterlogged: the property as 1.21 has it (a mod's leaves add it to their states). */
	public static final net.minecraft.block.properties.PropertyBool WATERLOGGED = io.github.xponer2.translator.block.BlockStateProperties.WATERLOGGED;

	public LeavesBlock(BlockProperties properties) {
		super(properties.noOcclusion());
		var state = this.getDefaultState();
		if (state.getPropertyKeys().contains(DISTANCE)) {
			state = state.withProperty(DISTANCE, 7);
		}
		if (state.getPropertyKeys().contains(PERSISTENT)) {
			state = state.withProperty(PERSISTENT, false);
		}
		if (state.getPropertyKeys().contains(WATERLOGGED)) {
			state = state.withProperty(WATERLOGGED, false);
		}
		this.setDefaultState(state);
	}

	/** The 1.21 leaves properties (a mod's leaves add theirs after super's). */
	@Override
	protected void createBlockStateDefinition(net.minecraft.block.state.BlockStateContainer.Builder builder) {
		builder.add(DISTANCE, PERSISTENT, WATERLOGGED);
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public BlockRenderLayer getRenderLayer() {
		return BlockRenderLayer.CUTOUT_MIPPED;
	}

	@Override
	@SideOnly(Side.CLIENT)
	@SuppressWarnings("deprecation")
	public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
		return true;
	}

	@Override
	public boolean isLeaves(IBlockState state, IBlockAccess world, BlockPos pos) {
		return true;
	}
}
