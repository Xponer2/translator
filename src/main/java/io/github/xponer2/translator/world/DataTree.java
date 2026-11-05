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
package io.github.xponer2.translator.world;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;

/**
 * A tree of a 1.21.1 mod's biome on 1.12.2: the log and leaves its tree feature names (Biomes O' Plenty's flowering
 * oak, maple, fir...), its height range, and a shape from its feature type (round, cone for the taiga-like ones, a
 * bush, a twiglet, a big round one). The 1.21 feature code itself doesn't run on 1.12.2; this draws its likeness.
 */
final class DataTree extends WorldGenAbstractTree {
	enum Shape {
		ROUND, CONE, BUSH, TWIGLET, BIG
	}

	private final IBlockState log;
	private final IBlockState leaves;
	private final int minHeight;
	private final int maxHeight;
	private final Shape shape;

	/** Grown from a sapling: the blocks are set with updates and the sapling's ground isn't checked (it was placed). */
	private final boolean sapling;

	DataTree(IBlockState log, IBlockState leaves, int minHeight, int maxHeight, Shape shape) {
		this(log, leaves, minHeight, maxHeight, shape, false);
	}

	private DataTree(IBlockState log, IBlockState leaves, int minHeight, int maxHeight, Shape shape, boolean sapling) {
		super(sapling);
		this.sapling = sapling;
		this.log = log;
		this.leaves = leaves;
		this.minHeight = Math.max(1, minHeight);
		this.maxHeight = Math.max(this.minHeight, maxHeight);
		this.shape = shape;
	}

	@Override
	public boolean generate(World world, Random random, BlockPos pos) {
		int height = this.minHeight + random.nextInt(this.maxHeight - this.minHeight + 1);
		if (pos.getY() < 1 || pos.getY() + height + 2 > world.getHeight()) {
			return false;
		}
		BlockPos below = pos.down();
		IBlockState ground = world.getBlockState(below);
		if (!this.sapling && !ground.getBlock().canSustainPlant(ground, world, below, EnumFacing.UP, (net.minecraft.block.BlockSapling) Blocks.SAPLING)) {
			return false;
		}
		for (int y = 0; y < height; y++) {
			if (!this.isReplaceable(world, pos.up(y))) {
				return false;
			}
		}
		ground.getBlock().onPlantGrow(ground, world, below, pos);
		switch (this.shape) {
			case CONE -> this.cone(world, random, pos, height);
			case BUSH -> this.blob(world, random, pos, 0, 1, 2);
			case TWIGLET -> this.blob(world, random, pos.up(height - 1), 0, 1, 1);
			case BIG -> this.blob(world, random, pos.up(height - 4), 0, 4, 3);
			default -> this.blob(world, random, pos.up(height - 3), 0, 3, 2);
		}
		for (int y = 0; y < height; y++) {
			this.setBlockAndNotifyAdequately(world, pos.up(y), this.log);
		}
		return true;
	}

	/** Leaves in layers from {@code base}: {@code layers} high, the top layer one narrower, corners left out at random. */
	private void blob(World world, Random random, BlockPos base, int from, int layers, int radius) {
		for (int dy = from; dy <= layers; dy++) {
			int r = dy >= layers - 1 ? Math.max(1, radius - 1) : radius;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.abs(dx) == r && Math.abs(dz) == r && (dy == layers || random.nextInt(2) == 0)) {
						continue;
					}
					this.leaf(world, base.add(dx, dy, dz));
				}
			}
		}
	}

	/** A cone: wide at the bottom of the crown, a point on top (fir, pine, redwood, the taiga trees). */
	private void cone(World world, Random random, BlockPos pos, int height) {
		int crownStart = Math.max(1, height / 4);
		int radius = 0;
		for (int y = height; y >= crownStart; y--) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (Math.abs(dx) + Math.abs(dz) <= radius + 1 && !(Math.abs(dx) == radius && Math.abs(dz) == radius && radius > 0)) {
						this.leaf(world, pos.add(dx, y, dz));
					}
				}
			}
			radius = (height - y) % 2 == 1 ? Math.min(3, radius + 1) : Math.max(1, radius);
		}
		this.leaf(world, pos.up(height + 1));
	}

	/** The same tree grown from a sapling. */
	DataTree grown() {
		return new DataTree(this.log, this.leaves, this.minHeight, this.maxHeight, this.shape, true);
	}

	private void leaf(World world, BlockPos pos) {
		IBlockState state = world.getBlockState(pos);
		if (state.getBlock().isAir(state, world, pos) || state.getMaterial() == Material.LEAVES || state.getMaterial() == Material.PLANTS
				|| state.getMaterial() == Material.VINE) {
			this.setBlockAndNotifyAdequately(world, pos, this.leaves);
		}
	}
}
