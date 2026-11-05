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

import java.util.Random;

import javax.annotation.Nullable;

import io.github.xponer2.translator.block.BlockProperties;
import io.github.xponer2.translator.block.BlockStateProperties;
import io.github.xponer2.translator.shape.CollisionContext;
import io.github.xponer2.translator.shape.Shapes;
import io.github.xponer2.translator.shape.VoxelShape;
import io.github.xponer2.translator.util.BlockPlaceContext;

import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Facade of the 1.21 {@code BushBlock} (neoforge-1.21.1-runtime.tsv): a plant on dirt, grass or farmland, walked
 * through, drawn cut out; it pops off when its ground goes.
 */
public abstract class BushBlock extends Block {
	public BushBlock(BlockProperties properties) {
		super(properties.noCollission());
	}

	/** The ground the plant grows on (1.21: dirt and farmland). */
	protected boolean mayPlaceOn(IBlockState state, IBlockAccess level, BlockPos pos) {
		net.minecraft.block.Block block = state.getBlock();
		return block == net.minecraft.init.Blocks.GRASS || block == net.minecraft.init.Blocks.DIRT || block == net.minecraft.init.Blocks.FARMLAND
				|| block == net.minecraft.init.Blocks.MYCELIUM || io.github.xponer2.translator.tags.TagManager.is(state,
						io.github.xponer2.translator.util.TagKey.create(io.github.xponer2.translator.util.Registries.BLOCK,
								new net.minecraft.util.ResourceLocation("minecraft", "dirt")));
	}

	@Override
	public boolean canSurvive(IBlockState state, World level, BlockPos pos) {
		BlockPos below = pos.down();
		return this.mayPlaceOn(level.getBlockState(below), level, below);
	}

	/** 1.21: a plant that can't stay becomes air (and breaks with its drops). */
	@Override
	public IBlockState updateShape(IBlockState state, EnumFacing direction, IBlockState neighborState, World level, BlockPos pos, BlockPos neighborPos) {
		return !this.canSurvive(state, level, pos) ? net.minecraft.init.Blocks.AIR.getDefaultState()
				: super.updateShape(state, direction, neighborState, level, pos, neighborPos);
	}

	@Override
	public VoxelShape getShape(IBlockState state, @Nullable IBlockAccess level, @Nullable BlockPos pos, CollisionContext context) {
		return Shapes.box16(2, 0, 2, 14, 13, 14);
	}


	@Override
	@SuppressWarnings("deprecation")
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public BlockRenderLayer getRenderLayer() {
		return BlockRenderLayer.CUTOUT;
	}

	@Override
	@SuppressWarnings("deprecation")
	public net.minecraft.block.state.BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
		return net.minecraft.block.state.BlockFaceShape.UNDEFINED;
	}
}
