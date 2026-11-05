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

import io.github.xponer2.translator.block.BlockProperties;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Facade of the 1.21 {@code LeavesBlock}: a leaves block (see-through, drawn cut out, faces between leaves drawn as
 * fancy graphics do); it doesn't decay.
 */
public class LeavesBlock extends Block {
	public static final net.minecraft.block.properties.PropertyInteger DISTANCE = io.github.xponer2.translator.block.BlockStateProperties.DISTANCE;
	public static final net.minecraft.block.properties.PropertyBool PERSISTENT = io.github.xponer2.translator.block.BlockStateProperties.PERSISTENT;
	/** 1.21 leaves can be waterlogged: the property as 1.21 has it (a mod's leaves add it to their states). */
	public static final net.minecraft.block.properties.PropertyBool WATERLOGGED = io.github.xponer2.translator.block.BlockStateProperties.WATERLOGGED;

	public LeavesBlock(BlockProperties properties) {
		super(properties.noOcclusion());
		var state = this.getDefaultState();
		if (state.getPropertyKeys().contains(DISTANCE)) {
			state = state.withProperty(DISTANCE, 7);
		}
		if (state.getPropertyKeys().contains(PERSISTENT)) {
			state = state.withProperty(PERSISTENT, false);
		}
		if (state.getPropertyKeys().contains(WATERLOGGED)) {
			state = state.withProperty(WATERLOGGED, false);
		}
		this.setDefaultState(state);
	}

	/** The 1.21 leaves properties (a mod's leaves add theirs after super's). */
	@Override
	protected void createBlockStateDefinition(net.minecraft.block.state.BlockStateContainer.Builder builder) {
		builder.add(DISTANCE, PERSISTENT, WATERLOGGED);
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public BlockRenderLayer getRenderLayer() {
		return BlockRenderLayer.CUTOUT_MIPPED;
	}

	@Override
	@SideOnly(Side.CLIENT)
	@SuppressWarnings("deprecation")
	public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
		return true;
	}

	@Override
	public boolean isLeaves(IBlockState state, IBlockAccess world, BlockPos pos) {
		return true;
	}

	/**
	 * Leaves shade each other as 1.12.2's do (they are full cubes there: the ambient occlusion of a solid block). A mod's
	 * leaves had none: their insides were as bright as their outsides.
	 */
	@Override
	@SideOnly(Side.CLIENT)
	@SuppressWarnings("deprecation")
	public float getAmbientOcclusionLightValue(IBlockState state) {
		return 0.2F;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isFullCube(IBlockState state) {
		return true;
	}

	// --- decay as in 1.21: the distance to a log through leaves (7: none within 6), leaves at 7 fall -------------------

	/** A player's leaves are persistent (they don't decay). */
	@javax.annotation.Nullable
	@Override
	public IBlockState getStateForPlacement(io.github.xponer2.translator.util.BlockPlaceContext context) {
		IBlockState state = this.getDefaultState();
		if (state.getPropertyKeys().contains(PERSISTENT)) {
			state = state.withProperty(PERSISTENT, true);
		}
		return withDistance(state, context.getLevel(), context.getClickedPos());
	}

	/** Leaves that can decay tick (the 1.21 leaves tick only at distance 7: here the distance is checked then). */
	@Override
	public boolean isRandomlyTicking(IBlockState state) {
		return !state.getPropertyKeys().contains(PERSISTENT) || !state.getValue(PERSISTENT);
	}

	@Override
	public boolean getTickRandomly() {
		return true;
	}

	@Override
	public void randomTick(IBlockState state, net.minecraft.world.WorldServer level, BlockPos pos, java.util.Random random) {
		IBlockState updated = withDistance(state, level, pos);
		if (decaying(updated)) {
			this.dropBlockAsItem(level, pos, state, 0);
			level.setBlockToAir(pos);
		} else if (updated != state) {
			level.setBlockState(pos, updated, 4);
		}
	}

	/** A scheduled tick after a neighbour changed: the new distance (1.21 tick). */
	@Override
	public void tick(IBlockState state, net.minecraft.world.WorldServer level, BlockPos pos, java.util.Random random) {
		IBlockState updated = withDistance(state, level, pos);
		if (updated != state) {
			level.setBlockState(pos, updated, 3);
		}
	}

	@Override
	public IBlockState updateShape(IBlockState state, EnumFacing direction, IBlockState neighborState, net.minecraft.world.World level, BlockPos pos,
			BlockPos neighborPos) {
		level.scheduleUpdate(pos, this, 1);
		return state;
	}

	private static boolean decaying(IBlockState state) {
		return state.getPropertyKeys().contains(DISTANCE) && state.getValue(DISTANCE) == 7
				&& !(state.getPropertyKeys().contains(PERSISTENT) && state.getValue(PERSISTENT));
	}

	/** The state with its distance to the nearest log (through leaves, 1 next to a log, 7 when none within 6). */
	static IBlockState withDistance(IBlockState state, net.minecraft.world.World level, BlockPos pos) {
		if (!state.getPropertyKeys().contains(DISTANCE)) {
			return state;
		}
		int distance = distanceToLog(level, pos);
		return state.getValue(DISTANCE) == distance ? state : state.withProperty(DISTANCE, distance);
	}

	/**
	 * Steps through leaves to the nearest log (breadth first, 6 at most). 1.21 keeps the distance up to date as trees
	 * grow; the trees of the worldgen here place theirs as the data says (7), so it's counted when it matters.
	 */
	static int distanceToLog(net.minecraft.world.World level, BlockPos start) {
		var seen = new java.util.HashSet<BlockPos>();
		var current = new java.util.ArrayList<BlockPos>();
		current.add(start);
		seen.add(start);
		for (int distance = 1; distance <= 6; distance++) {
			var next = new java.util.ArrayList<BlockPos>();
			for (BlockPos pos : current) {
				for (EnumFacing facing : EnumFacing.values()) {
					BlockPos side = pos.offset(facing);
					if (!seen.add(side) || !level.isBlockLoaded(side)) {
						continue;
					}
					IBlockState state = level.getBlockState(side);
					if (isLog(state, level, side)) {
						return distance;
					}
					if (state.getBlock().isLeaves(state, level, side)) {
						next.add(side);
					}
				}
			}
			current = next;
		}
		return 7;
	}

	private static final io.github.xponer2.translator.util.TagKey<net.minecraft.block.Block> LOGS = io.github.xponer2.translator.util.TagKey.create(
			io.github.xponer2.translator.util.Registries.BLOCK, new net.minecraft.util.ResourceLocation("minecraft", "logs"));

	private static boolean isLog(IBlockState state, net.minecraft.world.World level, BlockPos pos) {
		net.minecraft.block.Block block = state.getBlock();
		if (block instanceof net.minecraft.block.BlockLog || block.canSustainLeaves(state, level, pos)) {
			return true;
		}
		if (block instanceof RotatedPillarBlock && state.getMaterial() == net.minecraft.block.material.Material.WOOD) {
			return true;
		}
		try {
			return io.github.xponer2.translator.tags.TagManager.is(state, LOGS);
		} catch (RuntimeException e) {
			return false;
		}
	}
}
