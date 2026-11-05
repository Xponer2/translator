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

import javax.annotation.Nullable;

import io.github.xponer2.translator.block.BlockProperties;
import io.github.xponer2.translator.shape.CollisionContext;
import io.github.xponer2.translator.shape.Shapes;
import io.github.xponer2.translator.shape.VoxelShape;
import io.github.xponer2.translator.util.BlockPlaceContext;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Facade of the 1.21 {@code WallHangingSignBlock}: a sign hanging from a plank that sticks out of a wall, its faces across
 * the wall (facing along it). It was a wall sign, flat on the wall.
 */
public class WallHangingSignBlock extends WallSignBlock {
	public WallHangingSignBlock(Object woodType, BlockProperties properties) {
		super(woodType, properties);
	}

	public WallHangingSignBlock(BlockProperties properties, Object woodType) {
		this(woodType, properties);
	}

	/** The plank at the top and the board under it, across the facing. */
	@Override
	public VoxelShape getShape(IBlockState state, @Nullable IBlockAccess level, @Nullable BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == EnumFacing.Axis.Z
				? Shapes.or(Shapes.box16(0, 14, 6, 16, 16, 10), Shapes.box16(1, 0, 7, 15, 10, 9))
				: Shapes.or(Shapes.box16(6, 14, 0, 10, 16, 16), Shapes.box16(7, 0, 1, 9, 10, 15));
	}

	/** Across the clicked wall, the side the player looks along. */
	@Nullable
	@Override
	public IBlockState getStateForPlacement(BlockPlaceContext context) {
		EnumFacing wall = context.getClickedFace();
		if (!wall.getAxis().isHorizontal()) {
			return null;
		}
		EnumFacing look = context.getPlayer() == null ? wall.rotateY() : context.getPlayer().getHorizontalFacing();
		if (look.getAxis() == wall.getAxis()) {
			look = look.rotateY();
		}
		return this.defaultBlockState().withProperty(FACING, look.getOpposite());
	}
}
