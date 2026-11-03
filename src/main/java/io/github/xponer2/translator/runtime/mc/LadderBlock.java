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

/** Facade of the 1.21 {@code LadderBlock}: a climbable plate on a wall. */
public class LadderBlock extends Block {
	public static final net.minecraft.block.properties.PropertyDirection FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final VoxelShape LADDER_EAST_AABB = Shapes.box16(0, 0, 0, 3, 16, 16);
	public static final VoxelShape LADDER_WEST_AABB = Shapes.box16(13, 0, 0, 16, 16, 16);
	public static final VoxelShape LADDER_SOUTH_AABB = Shapes.box16(0, 0, 0, 16, 16, 3);
	public static final VoxelShape LADDER_NORTH_AABB = Shapes.box16(0, 0, 13, 16, 16, 16);

	public LadderBlock(BlockProperties properties) {
		super(properties.noOcclusion());
	}

	@Override
	protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
		builder.add(FACING);
	}

	@Override
	public VoxelShape getShape(IBlockState state, @Nullable IBlockAccess level, @Nullable BlockPos pos, CollisionContext context) {
		switch (state.getValue(FACING)) {
		case NORTH:
			return LADDER_NORTH_AABB;
		case SOUTH:
			return LADDER_SOUTH_AABB;
		case WEST:
			return LADDER_WEST_AABB;
		default:
			return LADDER_EAST_AABB;
		}
	}

	@Nullable
	@Override
	public IBlockState getStateForPlacement(BlockPlaceContext context) {
		EnumFacing face = context.getClickedFace();
		return face.getAxis().isHorizontal() ? this.defaultBlockState().withProperty(FACING, face) : null;
	}

	@Override
	public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos, net.minecraft.entity.EntityLivingBase entity) {
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public BlockRenderLayer getRenderLayer() {
		return BlockRenderLayer.CUTOUT;
	}
}
