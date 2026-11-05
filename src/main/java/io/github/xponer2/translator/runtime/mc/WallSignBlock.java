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

/** Facade of the 1.21 {@code WallSignBlock} (and wall hanging signs): a sign on a wall. */
public class WallSignBlock extends Block implements io.github.xponer2.translator.block.EntityBlock {
	public static final net.minecraft.block.properties.PropertyDirection FACING = BlockStateProperties.HORIZONTAL_FACING;
	private final Object type;

	public WallSignBlock(Object woodType, BlockProperties properties) {
		super(properties.noCollission().noOcclusion());
		this.type = woodType;
	}

	public WallSignBlock(BlockProperties properties, Object woodType) {
		this(woodType, properties);
	}

	public Object type() {
		return this.type;
	}

	/** 1.21 SignBlock.newBlockEntity: the sign's text (a mod's sign makes its own, a subclass). */
	@Override
	public net.minecraft.tileentity.TileEntity newBlockEntity(BlockPos pos, IBlockState state) {
		return new SignBlockEntity(pos, state);
	}

	@Override
	protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
		builder.add(FACING);
	}

	@Override
	public VoxelShape getShape(IBlockState state, @Nullable IBlockAccess level, @Nullable BlockPos pos, CollisionContext context) {
		switch (state.getValue(FACING)) {
		case NORTH:
			return Shapes.box16(0, 4.5, 14, 16, 12.5, 16);
		case SOUTH:
			return Shapes.box16(0, 4.5, 0, 16, 12.5, 2);
		case WEST:
			return Shapes.box16(14, 4.5, 0, 16, 12.5, 16);
		default:
			return Shapes.box16(0, 4.5, 0, 2, 12.5, 16);
		}
	}

	@Nullable
	@Override
	public IBlockState getStateForPlacement(BlockPlaceContext context) {
		EnumFacing face = context.getClickedFace();
		return face.getAxis().isHorizontal() ? this.defaultBlockState().withProperty(FACING, face) : null;
	}
}
