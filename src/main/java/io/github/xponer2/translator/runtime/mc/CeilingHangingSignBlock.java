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
import io.github.xponer2.translator.block.BlockStateProperties;
import io.github.xponer2.translator.shape.CollisionContext;
import io.github.xponer2.translator.shape.Shapes;
import io.github.xponer2.translator.shape.VoxelShape;
import io.github.xponer2.translator.util.BlockPlaceContext;

import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Facade of the 1.21 {@code CeilingHangingSignBlock}: a sign hanging under a block on two chains (one chain, "attached",
 * when the block above isn't a full face or the player sneaks), turned in sixteenths. It was a standing sign.
 */
public class CeilingHangingSignBlock extends StandingSignBlock {
	public static final net.minecraft.block.properties.PropertyBool ATTACHED = BlockStateProperties.ATTACHED;

	public CeilingHangingSignBlock(Object woodType, BlockProperties properties) {
		super(woodType, properties);
		var state = this.getDefaultState();
		if (state.getPropertyKeys().contains(ATTACHED)) {
			this.setDefaultState(state.withProperty(ATTACHED, false));
		}
	}

	public CeilingHangingSignBlock(BlockProperties properties, Object woodType) {
		this(woodType, properties);
	}

	@Override
	protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
		builder.add(ROTATION, ATTACHED);
	}

	@Override
	public VoxelShape getShape(IBlockState state, @Nullable IBlockAccess level, @Nullable BlockPos pos, CollisionContext context) {
		return Shapes.box16(3, 0, 3, 13, 16, 13);
	}

	/** Under a block. */
	@Override
	public boolean canSurvive(IBlockState state, World level, BlockPos pos) {
		IBlockState above = level.getBlockState(pos.up());
		return !above.getBlock().isAir(above, level, pos.up());
	}

	@Nullable
	@Override
	public IBlockState getStateForPlacement(BlockPlaceContext context) {
		World level = context.getLevel();
		BlockPos above = context.getClickedPos().up();
		boolean attached = context.getPlayer() != null && context.getPlayer().isSneaking()
				|| !level.getBlockState(above).isSideSolid(level, above, EnumFacing.DOWN);
		float yaw = context.getPlayer() == null ? 0.0F : context.getPlayer().rotationYaw;
		int rotation = net.minecraft.util.math.MathHelper.floor((180.0F + yaw) * 16.0F / 360.0F + 0.5D) & 15;
		if (!attached) {
			// on two chains: one of the four sides
			rotation = (rotation + 2) / 4 * 4 & 15;
		}
		return this.defaultBlockState().withProperty(ROTATION, rotation).withProperty(ATTACHED, attached);
	}
}
