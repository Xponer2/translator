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

/** Facade of the 1.21 {@code HugeMushroomBlock}: a block of a big mushroom, a face per side. */
public class HugeMushroomBlock extends Block {
	public static final net.minecraft.block.properties.PropertyBool NORTH = BlockStateProperties.NORTH;
	public static final net.minecraft.block.properties.PropertyBool EAST = BlockStateProperties.EAST;
	public static final net.minecraft.block.properties.PropertyBool SOUTH = BlockStateProperties.SOUTH;
	public static final net.minecraft.block.properties.PropertyBool WEST = BlockStateProperties.WEST;
	public static final net.minecraft.block.properties.PropertyBool UP = BlockStateProperties.UP;
	public static final net.minecraft.block.properties.PropertyBool DOWN = BlockStateProperties.DOWN;

	public HugeMushroomBlock(BlockProperties properties) {
		super(properties);
		this.setDefaultState(this.getDefaultState().withProperty(NORTH, true).withProperty(EAST, true).withProperty(SOUTH, true)
				.withProperty(WEST, true).withProperty(UP, true).withProperty(DOWN, true));
	}

	@Override
	protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
		builder.add(UP, DOWN, NORTH, EAST, SOUTH, WEST);
	}
}
