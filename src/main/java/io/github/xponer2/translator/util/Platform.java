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
package io.github.xponer2.translator.util;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Small 1.21 helpers the converter's rules point to (ItemStack#copyWithCount, ItemStack#isSameItemSameComponents,
 * Vec3#atCenterOf).
 */
public final class Platform {
	private Platform() {
	}

	/**
	 * 1.21 {@code Level#addFreshEntity}: only the server adds entities (a client world adds nothing). 1.12.2
	 * {@code World#spawnEntity} adds them on the client too, as copies that can't be picked up or hit.
	 */
	public static boolean addFreshEntity(net.minecraft.world.World world, net.minecraft.entity.Entity entity) {
		return !world.isRemote && world.spawnEntity(entity);
	}

	public static ItemStack copyWithCount(ItemStack stack, int count) {
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		var copy = stack.copy();
		copy.setCount(count);
		return copy;
	}

	/** Same item, meta and NBT (the 1.21 components live in the NBT). */
	public static boolean isSameItemSameComponents(ItemStack first, ItemStack second) {
		return ItemStack.areItemsEqual(first, second) && ItemStack.areItemStackTagsEqual(first, second);
	}

	/** 1.21 Block.getDrops(state, level, pos, tile): the drops of the block's 1.12.2 getDrops (no fortune). */
	public static net.minecraft.util.NonNullList<ItemStack> getDrops(net.minecraft.block.state.IBlockState state, net.minecraft.world.World level, BlockPos pos) {
		var drops = net.minecraft.util.NonNullList.<ItemStack>create();
		state.getBlock().getDrops(drops, level, pos, state, 0);
		return drops;
	}

	/**
	 * 1.21 BlockState#getToolModifiedState for the tool actions 1.12.2 has (shovel path, hoe till); null when the
	 * block doesn't change.
	 */
	@javax.annotation.Nullable
	public static net.minecraft.block.state.IBlockState toolModifiedState(net.minecraft.block.state.IBlockState state, String action) {
		var block = state.getBlock();
		switch (action) {
			case "SHOVEL_FLATTEN":
				return block == net.minecraft.init.Blocks.GRASS ? net.minecraft.init.Blocks.GRASS_PATH.getDefaultState() : null;
			case "HOE_TILL":
				if (block == net.minecraft.init.Blocks.GRASS || block == net.minecraft.init.Blocks.GRASS_PATH) {
					return net.minecraft.init.Blocks.FARMLAND.getDefaultState();
				}
				if (block == net.minecraft.init.Blocks.DIRT) {
					var variant = state.getValue(net.minecraft.block.BlockDirt.VARIANT);
					if (variant == net.minecraft.block.BlockDirt.DirtType.DIRT) {
						return net.minecraft.init.Blocks.FARMLAND.getDefaultState();
					}
					if (variant == net.minecraft.block.BlockDirt.DirtType.COARSE_DIRT) {
						return state.withProperty(net.minecraft.block.BlockDirt.VARIANT, net.minecraft.block.BlockDirt.DirtType.DIRT);
					}
				}
				return null;
			default:
				return null;
		}
	}

	/** 1.21 ResourceLocation#withSuffix / withPrefix. */
	public static net.minecraft.util.ResourceLocation withSuffix(net.minecraft.util.ResourceLocation id, String suffix) {
		return new net.minecraft.util.ResourceLocation(id.getNamespace(), id.getPath() + suffix);
	}

	public static net.minecraft.util.ResourceLocation withPrefix(net.minecraft.util.ResourceLocation id, String prefix) {
		return new net.minecraft.util.ResourceLocation(id.getNamespace(), prefix + id.getPath());
	}

	/** 1.21 event.enqueueWork: 1.12.2 runs the lifecycle events on the main thread already. */
	public static void run(Runnable work) {
		work.run();
	}

	/** 1.21 Mth.lerp. */
	public static double lerp(double delta, double start, double end) {
		return start + delta * (end - start);
	}

	public static float lerp(float delta, float start, float end) {
		return start + delta * (end - start);
	}

	/** 1.21 AABB.ofSize(center, x, y, z). */
	public static net.minecraft.util.math.AxisAlignedBB boxOfSize(Vec3d center, double x, double y, double z) {
		return new net.minecraft.util.math.AxisAlignedBB(center.x - x / 2.0, center.y - y / 2.0, center.z - z / 2.0,
				center.x + x / 2.0, center.y + y / 2.0, center.z + z / 2.0);
	}

	public static Vec3d atCenterOf(BlockPos pos) {
		return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
	}
}
