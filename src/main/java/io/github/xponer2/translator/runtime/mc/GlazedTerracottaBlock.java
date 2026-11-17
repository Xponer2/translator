/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
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

/** Facade of the 1.21 {@code GlazedTerracottaBlock}: facing the way the player looks when placed. */
public class GlazedTerracottaBlock extends HorizontalDirectionalBlock {
	public GlazedTerracottaBlock(BlockProperties properties) {
		super(properties);
	}

	@Override
	protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
		builder.add(FACING);
	}

	@Override
	public IBlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().withProperty(FACING, context.getHorizontalDirection().getOpposite());
	}
}
