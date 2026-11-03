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

/** Facade of the 1.21 {@code MushroomBlock}: a small mushroom on a solid block in the dark (it doesn't grow big here). */
public class MushroomBlock extends BushBlock {
	public MushroomBlock(Object feature, BlockProperties properties) {
		super(properties);
	}

	@Override
	protected boolean mayPlaceOn(IBlockState state, IBlockAccess level, BlockPos pos) {
		return state.isFullBlock();
	}

	@Override
	public VoxelShape getShape(IBlockState state, @Nullable IBlockAccess level, @Nullable BlockPos pos, CollisionContext context) {
		return Shapes.box16(5, 0, 5, 11, 6, 11);
	}

	/** 1.21 growMushroom (renamed by the table): no huge mushroom feature here. */
	public boolean generateBigMushroom(net.minecraft.world.WorldServer level, BlockPos pos, IBlockState state, Random random) {
		return false;
	}
}
