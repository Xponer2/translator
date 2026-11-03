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

/** Facade of the 1.21 {@code FallingBlock}: falls when nothing holds it (the 1.12.2 falling block entity). */
public abstract class FallingBlock extends Block {
	public FallingBlock(BlockProperties properties) {
		super(properties);
	}

	/** 1.21 isFree (renamed by the table): air, fire, a liquid or a replaceable block. */
	public static boolean canFallThrough(IBlockState state) {
		return net.minecraft.block.BlockFalling.canFallThrough(state);
	}

	/** 1.21 falling (renamed by the table). */
	protected void onStartFalling(net.minecraft.entity.item.EntityFallingBlock entity) {
	}

	public int getDustColor(IBlockState state, IBlockAccess level, BlockPos pos) {
		return -16777216;
	}

	@Override
	public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
		world.scheduleUpdate(pos, this, 2);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void neighborChanged(IBlockState state, World world, BlockPos pos, net.minecraft.block.Block block, BlockPos from) {
		world.scheduleUpdate(pos, this, 2);
	}

	@Override
	public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
		if (!world.isRemote && pos.getY() > 0 && canFallThrough(world.getBlockState(pos.down()))
				&& world.isAreaLoaded(pos.add(-32, -32, -32), pos.add(32, 32, 32))) {
			var entity = new net.minecraft.entity.item.EntityFallingBlock(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, state);
			this.onStartFalling(entity);
			world.spawnEntity(entity);
		}
	}
}
