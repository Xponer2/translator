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
