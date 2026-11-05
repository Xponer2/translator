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
import io.github.xponer2.translator.world.TreeGrowing;

import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Facade of the 1.21 {@code SaplingBlock}: a plant that grows with light (random ticks) and with bone meal, first to
 * its second stage, then into the tree of its 1.21 tree grower (the mod's tree from its worldgen data).
 */
public class SaplingBlock extends BushBlock implements IGrowable {
	public static final net.minecraft.block.properties.PropertyInteger STAGE = BlockStateProperties.STAGE;

	/** The 1.21 TreeGrower (a carried class: the trees it names are grown by {@link TreeGrowing}). */
	@Nullable
	protected final Object treeGrower;

	public SaplingBlock(Object treeGrower, BlockProperties properties) {
		super(properties.randomTicks());
		this.treeGrower = treeGrower;
		var state = this.getDefaultState();
		if (state.getPropertyKeys().contains(STAGE)) {
			this.setDefaultState(state.withProperty(STAGE, 0));
		}
	}

	/** The 1.21 sapling's stage (a mod overriding this adds its own, STAGE among them). */
	@Override
	protected void createBlockStateDefinition(net.minecraft.block.state.BlockStateContainer.Builder builder) {
		builder.add(STAGE);
	}

	@Override
	public VoxelShape getShape(IBlockState state, @Nullable IBlockAccess level, @Nullable BlockPos pos, CollisionContext context) {
		return Shapes.box16(2, 0, 2, 14, 12, 14);
	}

	/** 1.21 random tick: with light, now and then one step of growth. */
	@Override
	public void randomTick(IBlockState state, WorldServer level, BlockPos pos, Random random) {
		if (level.isAreaLoaded(pos, 1) && level.getLightFromNeighbors(pos.up()) >= 9 && random.nextInt(7) == 0) {
			this.grow(level, pos, state, random);
		}
	}

	/** 1.21 {@code advanceTree} (renamed by the table): the second stage, then the tree. */
	public void grow(WorldServer level, BlockPos pos, IBlockState state, Random random) {
		this.advance(level, pos, state, random);
	}

	private void advance(WorldServer level, BlockPos pos, IBlockState state, Random random) {
		if (state.getPropertyKeys().contains(STAGE) && state.getValue(STAGE) == 0) {
			level.setBlockState(pos, state.cycleProperty(STAGE), 4);
		} else {
			TreeGrowing.growTree(this.treeGrower, level, null, pos, state, random);
		}
	}

	/**
	 * Inside performBonemeal on this thread: a mod's advanceTree calls its performBonemeal (BOP), and when that one
	 * ends in this one (its own couldn't run), the loop is broken here (it was a StackOverflowError at every tick).
	 */
	private static final ThreadLocal<Boolean> GROWING = ThreadLocal.withInitial(() -> Boolean.FALSE);

	// --- bone meal: the 1.21 methods (a mod overrides them) and 1.12.2's IGrowable calling them ----------------------

	public boolean isValidBonemealTarget(World level, BlockPos pos, IBlockState state) {
		return true;
	}

	public void performBonemeal(WorldServer level, Random random, BlockPos pos, IBlockState state) {
		if (GROWING.get()) {
			this.advance(level, pos, state, random);
			return;
		}
		GROWING.set(Boolean.TRUE);
		try {
			this.grow(level, pos, state, random);
		} finally {
			GROWING.set(Boolean.FALSE);
		}
	}

	@Override
	public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
		return this.isValidBonemealTarget(world, pos, state);
	}

	/** 1.21 {@code isBonemealSuccess} (the table gives it this name): 45 %. */
	@Override
	public boolean canUseBonemeal(World world, Random random, BlockPos pos, IBlockState state) {
		return world.rand.nextFloat() < 0.45F;
	}

	@Override
	public void grow(World world, Random random, BlockPos pos, IBlockState state) {
		if (world instanceof WorldServer server) {
			this.performBonemeal(server, random, pos, state);
		}
	}
}
