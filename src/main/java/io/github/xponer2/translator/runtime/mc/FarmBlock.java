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

import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/** Facade of the 1.21 {@code FarmBlock} (neoforge-1.21.1-runtime.tsv): tilled soil, its moisture as 1.12.2 farmland's. */
public class FarmBlock extends Block {
	public static final net.minecraft.block.properties.PropertyInteger MOISTURE = net.minecraft.block.BlockFarmland.MOISTURE;

	public FarmBlock(BlockProperties properties) {
		super(properties.noOcclusion());
	}

	@Override
	protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
		builder.add(MOISTURE);
	}

	@Override
	public VoxelShape getShape(IBlockState state, @Nullable IBlockAccess level, @Nullable BlockPos pos, CollisionContext context) {
		return Shapes.box16(0, 0, 0, 16, 15, 16);
	}

	@Override
	public boolean canSustainPlant(IBlockState state, IBlockAccess world, BlockPos pos, net.minecraft.util.EnumFacing direction,
			net.minecraftforge.common.IPlantable plantable) {
		return plantable.getPlantType(world, pos.offset(direction)) == net.minecraftforge.common.EnumPlantType.Crop;
	}
}
