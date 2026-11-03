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
 * Facade of the 1.21 {@code DoublePlantBlock}: a plant two blocks high (lower and upper half); the upper half is
 * placed with the lower one and goes with it.
 */
public class DoublePlantBlock extends BushBlock {
	public static final net.minecraft.block.properties.PropertyEnum<io.github.xponer2.translator.block.DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

	public DoublePlantBlock(BlockProperties properties) {
		super(properties);
	}

	@Override
	protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
		builder.add(HALF);
	}

	@Override
	public VoxelShape getShape(IBlockState state, @Nullable IBlockAccess level, @Nullable BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	@Nullable
	@Override
	public IBlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		World level = context.getLevel();
		return pos.getY() < 255 && level.getBlockState(pos.up()).getBlock().isReplaceable(level, pos.up())
				? this.defaultBlockState().withProperty(HALF, io.github.xponer2.translator.block.DoubleBlockHalf.LOWER) : null;
	}

	@Override
	public boolean canSurvive(IBlockState state, World level, BlockPos pos) {
		if (state.getPropertyKeys().contains(HALF) && state.getValue(HALF) == io.github.xponer2.translator.block.DoubleBlockHalf.UPPER) {
			IBlockState below = level.getBlockState(pos.down());
			return below.getBlock() == this && below.getValue(HALF) == io.github.xponer2.translator.block.DoubleBlockHalf.LOWER;
		}
		return super.canSurvive(state, level, pos);
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, net.minecraft.entity.EntityLivingBase placer,
			net.minecraft.item.ItemStack stack) {
		super.onBlockPlacedBy(world, pos, state, placer, stack);
		world.setBlockState(pos.up(), state.withProperty(HALF, io.github.xponer2.translator.block.DoubleBlockHalf.UPPER), 3);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void neighborChanged(IBlockState state, World world, BlockPos pos, net.minecraft.block.Block block, BlockPos from) {
		if (!world.isRemote && !this.canSurvive(state, world, pos)) {
			boolean lower = state.getValue(HALF) == io.github.xponer2.translator.block.DoubleBlockHalf.LOWER;
			if (lower) {
				this.dropBlockAsItem(world, pos, state, 0);
			}
			world.setBlockToAir(pos);
			return;
		}
		if (state.getValue(HALF) == io.github.xponer2.translator.block.DoubleBlockHalf.LOWER && world.getBlockState(pos.up()).getBlock() != this) {
			world.setBlockToAir(pos);
		}
	}

	@Override
	public net.minecraft.item.Item getItemDropped(IBlockState state, Random random, int fortune) {
		return state.getValue(HALF) == io.github.xponer2.translator.block.DoubleBlockHalf.UPPER ? net.minecraft.init.Items.AIR
				: super.getItemDropped(state, random, fortune);
	}
}
