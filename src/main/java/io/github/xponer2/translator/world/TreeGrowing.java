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

import java.lang.reflect.Method;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenBigTree;
import net.minecraft.world.gen.feature.WorldGenBirchTree;
import net.minecraft.world.gen.feature.WorldGenCanopyTree;
import net.minecraft.world.gen.feature.WorldGenMegaJungle;
import net.minecraft.world.gen.feature.WorldGenMegaPineTree;
import net.minecraft.world.gen.feature.WorldGenSavannaTree;
import net.minecraft.world.gen.feature.WorldGenTaiga2;
import net.minecraft.world.gen.feature.WorldGenTrees;

/**
 * The 1.21 {@code TreeGrower.growTree} of a sapling: the tree its grower names (a 2x2 one for four saplings when it has
 * one, one with flowers near flowers), grown from the mod's worldgen data ({@link DataBiomes#treeFor}) or, for a vanilla
 * feature, by 1.12.2's own tree. The carried TreeGrower's growTree calls this (its features don't run here).
 */
public final class TreeGrowing {
	private TreeGrowing() {
	}

	/** {@code grower.growTree(level, generator, pos, state, random)}: whether a tree grew. */
	public static boolean growTree(@Nullable Object grower, Object level, @Nullable Object generator, Object pos, Object state, Object random) {
		if (!(level instanceof World world) || grower == null) {
			return false;
		}
		BlockPos at = (BlockPos) pos;
		IBlockState sapling = (IBlockState) state;
		Random rand = (Random) random;
		ResourceLocation mega = key(call(grower, "getConfiguredMegaFeature", rand));
		if (mega != null) {
			for (int i = 0; i >= -1; i--) {
				for (int j = 0; j >= -1; j--) {
					if (twoByTwo(world, at, sapling, i, j)) {
						WorldGenAbstractTree tree = tree(mega, rand, true);
						if (tree == null) {
							break;
						}
						BlockPos corner = at.add(i, 0, j);
						IBlockState air = Blocks.AIR.getDefaultState();
						BlockPos[] four = {corner, corner.east(), corner.south(), corner.south().east()};
						for (BlockPos one : four) {
							world.setBlockState(one, air, 4);
						}
						if (tree.generate(world, rand, corner)) {
							return true;
						}
						for (BlockPos one : four) {
							world.setBlockState(one, sapling, 4);
						}
						return false;
					}
				}
			}
		}
		ResourceLocation feature = key(call(grower, "getConfiguredFeature", rand, hasFlowers(world, at)));
		if (feature == null) {
			return false;
		}
		WorldGenAbstractTree tree = tree(feature, rand, false);
		if (tree == null) {
			return false;
		}
		world.setBlockState(at, Blocks.AIR.getDefaultState(), 4);
		if (tree.generate(world, rand, at)) {
			return true;
		}
		world.setBlockState(at, sapling, 4);
		return false;
	}

	/** The (not 2x2) tree a grower names, without growing it (the self-test). */
	@Nullable
	static WorldGenAbstractTree treeOf(@Nullable Object grower, Random random) {
		if (grower == null) {
			return null;
		}
		ResourceLocation feature = key(call(grower, "getConfiguredFeature", random, false));
		return feature == null ? null : tree(feature, random, false);
	}

	/** The tree of a configured feature: the mod's data, or 1.12.2's tree for a vanilla one. */
	@Nullable
	static WorldGenAbstractTree tree(ResourceLocation feature, Random random, boolean mega) {
		if (!"minecraft".equals(feature.getNamespace())) {
			return DataBiomes.treeFor(feature, random);
		}
		String path = feature.getPath();
		IBlockState jungleLog = Blocks.LOG.getDefaultState().withProperty(net.minecraft.block.BlockOldLog.VARIANT, net.minecraft.block.BlockPlanks.EnumType.JUNGLE);
		IBlockState jungleLeaves = Blocks.LEAVES.getDefaultState().withProperty(net.minecraft.block.BlockOldLeaf.VARIANT, net.minecraft.block.BlockPlanks.EnumType.JUNGLE)
				.withProperty(net.minecraft.block.BlockLeaves.CHECK_DECAY, false);
		if (path.startsWith("mega_jungle")) {
			return new WorldGenMegaJungle(true, 10, 20, jungleLog, jungleLeaves);
		}
		if (path.startsWith("mega_spruce")) {
			return new WorldGenMegaPineTree(true, false);
		}
		if (path.startsWith("mega_pine")) {
			return new WorldGenMegaPineTree(true, true);
		}
		if (path.startsWith("dark_oak")) {
			return new WorldGenCanopyTree(true);
		}
		if (path.startsWith("fancy_oak")) {
			return new WorldGenBigTree(true);
		}
		if (path.startsWith("super_birch")) {
			return new WorldGenBirchTree(true, true);
		}
		if (path.startsWith("birch")) {
			return new WorldGenBirchTree(true, false);
		}
		if (path.startsWith("spruce") || path.startsWith("pine")) {
			return new WorldGenTaiga2(true);
		}
		if (path.startsWith("acacia")) {
			return new WorldGenSavannaTree(true);
		}
		if (path.startsWith("jungle")) {
			return new WorldGenTrees(true, 4 + random.nextInt(7), jungleLog, jungleLeaves, false);
		}
		if (path.startsWith("oak")) {
			return new WorldGenTrees(true);
		}
		return DataBiomes.vanillaTree(path);
	}

	private static boolean twoByTwo(World world, BlockPos pos, IBlockState sapling, int i, int j) {
		Block block = sapling.getBlock();
		return world.getBlockState(pos.add(i, 0, j)).getBlock() == block && world.getBlockState(pos.add(i + 1, 0, j)).getBlock() == block
				&& world.getBlockState(pos.add(i, 0, j + 1)).getBlock() == block && world.getBlockState(pos.add(i + 1, 0, j + 1)).getBlock() == block;
	}

	/** 1.21: flowers within 2 blocks around and one up or down (the trees with bees). */
	private static boolean hasFlowers(World world, BlockPos pos) {
		for (BlockPos near : BlockPos.getAllInBoxMutable(pos.down().north(2).west(2), pos.up().south(2).east(2))) {
			Block block = world.getBlockState(near).getBlock();
			if (block instanceof BlockFlower || block instanceof io.github.xponer2.translator.runtime.mc.FlowerBlock) {
				return true;
			}
		}
		return false;
	}

	/** A private method of the carried TreeGrower, by its name (its parameters are 1.12.2 types after conversion). */
	@Nullable
	private static Object call(Object grower, String name, Object... args) {
		for (Class<?> type = grower.getClass(); type != null; type = type.getSuperclass()) {
			for (Method method : type.getDeclaredMethods()) {
				if (method.getName().equals(name) && method.getParameterCount() == args.length) {
					try {
						method.setAccessible(true);
						return method.invoke(grower, args);
					} catch (ReflectiveOperationException | RuntimeException e) {
						io.github.xponer2.translator.runtime.RuntimeStubs.failed("TreeGrower." + name, e);
						return null;
					}
				}
			}
		}
		return null;
	}

	@Nullable
	private static ResourceLocation key(@Nullable Object key) {
		if (key instanceof java.util.Optional<?> optional) {
			key = optional.orElse(null);
		}
		if (key instanceof io.github.xponer2.translator.registry.ResourceKey<?> resourceKey) {
			return resourceKey.location();
		}
		return key instanceof ResourceLocation location ? location : null;
	}
}
