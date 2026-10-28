package io.github.xponer2.translator.util;

import net.minecraft.entity.Entity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 1.21 Containers: drops the contents of an inventory or a stack list into the world.
 */
public final class Containers {
	private Containers() {
	}

	public static void dropContents(World level, BlockPos pos, NonNullList<ItemStack> stacks) {
		for (var stack : stacks) {
			if (!stack.isEmpty()) {
				InventoryHelper.spawnItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
			}
		}
	}

	public static void dropContents(World level, BlockPos pos, IInventory inventory) {
		InventoryHelper.dropInventoryItems(level, pos, inventory);
	}

	public static void dropContents(World level, Entity entity, IInventory inventory) {
		InventoryHelper.dropInventoryItems(level, entity, inventory);
	}

	public static void dropItemStack(World level, double x, double y, double z, ItemStack stack) {
		InventoryHelper.spawnItemStack(level, x, y, z, stack);
	}
}
