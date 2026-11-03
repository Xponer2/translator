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

/** Facade of the 1.21 {@code StandingSignBlock} (and ceiling hanging signs): a sign turned in sixteenths. */
public class StandingSignBlock extends Block {
	public static final net.minecraft.block.properties.PropertyInteger ROTATION = BlockStateProperties.ROTATION_16;
	private final Object type;

	public StandingSignBlock(Object woodType, BlockProperties properties) {
		super(properties.noCollission().noOcclusion());
		this.type = woodType;
	}

	public StandingSignBlock(BlockProperties properties, Object woodType) {
		this(woodType, properties);
	}

	public Object type() {
		return this.type;
	}

	@Override
	protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
		builder.add(ROTATION);
	}

	@Override
	public VoxelShape getShape(IBlockState state, @Nullable IBlockAccess level, @Nullable BlockPos pos, CollisionContext context) {
		return Shapes.box16(4, 0, 4, 12, 16, 12);
	}

	@Nullable
	@Override
	public IBlockState getStateForPlacement(BlockPlaceContext context) {
		net.minecraft.entity.player.EntityPlayer player = context.getPlayer();
		float yaw = player == null ? 0.0F : player.rotationYaw;
		return this.defaultBlockState().withProperty(ROTATION, net.minecraft.util.math.MathHelper.floor((180.0F + yaw) * 16.0F / 360.0F + 0.5D) & 15);
	}
}
