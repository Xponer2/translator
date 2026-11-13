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

	/**
	 * 1.21 {@code getDelayAfterPlace()}: the ticks before a placed or moved block falls (2; Supplementaries' ash, sacks
	 * and urns override it).
	 */
	protected int getDelayAfterPlace() {
		return 2;
	}

	public int getDustColor(IBlockState state, IBlockAccess level, BlockPos pos) {
		return -16777216;
	}

	@Override
	public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
		super.onBlockAdded(world, pos, state);
		world.scheduleUpdate(pos, this, this.getDelayAfterPlace());
	}

	@Override
	@SuppressWarnings("deprecation")
	public void neighborChanged(IBlockState state, World world, BlockPos pos, net.minecraft.block.Block block, BlockPos from) {
		world.scheduleUpdate(pos, this, this.getDelayAfterPlace());
	}

	/** 1.21 {@code Fallable.onLand}: the block landed and was placed ({@code replaced} was there before). */
	public void onLand(World level, BlockPos pos, IBlockState state, IBlockState replaced, net.minecraft.entity.item.EntityFallingBlock entity) {
	}

	/** 1.21 {@code Fallable.onBrokenAfterFall}: the block landed where it couldn't be placed. */
	public void onBrokenAfterFall(World level, BlockPos pos, net.minecraft.entity.item.EntityFallingBlock entity) {
	}

	/** A 1.21 Fallable's own onLand / onBrokenAfterFall (a mod block that implements Fallable without this class). */
	private static final ClassValue<java.lang.reflect.Method[]> FALLABLE = new ClassValue<java.lang.reflect.Method[]>() {
		@Override
		protected java.lang.reflect.Method[] computeValue(Class<?> type) {
			java.lang.reflect.Method land = null;
			java.lang.reflect.Method broken = null;
			for (java.lang.reflect.Method method : type.getMethods()) {
				if ("onLand".equals(method.getName()) && method.getParameterCount() == 5) {
					land = method;
				} else if ("onBrokenAfterFall".equals(method.getName()) && method.getParameterCount() == 3) {
					broken = method;
				}
			}
			return land == null && broken == null ? null : new java.lang.reflect.Method[] {land, broken};
		}
	};

	/**
	 * 1.12.2's falling block entity asks this in place of {@code block instanceof BlockFalling} (core hook): 1.12.2's
	 * falling blocks, this class's and any other 1.21 Fallable get told where they landed.
	 */
	public static boolean landsAsFallable(@Nullable Object block) {
		return block instanceof net.minecraft.block.BlockFalling || block instanceof FallingBlock || block != null && FALLABLE.get(block.getClass()) != null;
	}

	/** In place of 1.12.2's {@code ((BlockFalling) block).onEndFalling(world, pos, state, replaced)} (core hook). */
	public static void landed(net.minecraft.block.Block block, World world, BlockPos pos, IBlockState state, IBlockState replaced,
			net.minecraft.entity.item.EntityFallingBlock entity) {
		try {
			if (block instanceof net.minecraft.block.BlockFalling falling) {
				falling.onEndFalling(world, pos, state, replaced);
			} else if (block instanceof FallingBlock falling) {
				falling.onLand(world, pos, state, replaced, entity);
			} else {
				java.lang.reflect.Method[] own = FALLABLE.get(block.getClass());
				if (own != null && own[0] != null) {
					own[0].invoke(block, world, pos, state, replaced, entity);
				}
			}
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(block.getClass().getName() + ".onLand", e);
		}
	}

	/** In place of 1.12.2's {@code ((BlockFalling) block).onBroken(world, pos)} (core hook). */
	public static void broken(net.minecraft.block.Block block, World world, BlockPos pos, net.minecraft.entity.item.EntityFallingBlock entity) {
		try {
			if (block instanceof net.minecraft.block.BlockFalling falling) {
				falling.onBroken(world, pos);
			} else if (block instanceof FallingBlock falling) {
				falling.onBrokenAfterFall(world, pos, entity);
			} else {
				java.lang.reflect.Method[] own = FALLABLE.get(block.getClass());
				if (own != null && own[1] != null) {
					own[1].invoke(block, world, pos, entity);
				}
			}
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(block.getClass().getName() + ".onBrokenAfterFall", e);
		}
	}

	/** 1.21 FallingBlock.tick: falls when the block below lets it (a mod's tick override calls this through super). */
	@Override
	public void tick(IBlockState state, net.minecraft.world.WorldServer world, BlockPos pos, Random random) {
		if (pos.getY() > 0 && canFallThrough(world.getBlockState(pos.down()))
				&& world.isAreaLoaded(pos.add(-32, -32, -32), pos.add(32, 32, 32))) {
			var entity = new net.minecraft.entity.item.EntityFallingBlock(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, state);
			this.onStartFalling(entity);
			world.spawnEntity(entity);
		}
	}
}
