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

import io.github.xponer2.translator.block.BlockProperties;

import net.minecraft.block.BlockDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Facade of the 1.21 {@code DoorBlock}: the 1.12.2 door, with properties; it drops its own item (not the oak door). */
public class DoorBlock extends BlockDoor {
	public DoorBlock(Object blockSetType, BlockProperties properties) {
		super(properties.material());
		properties.applyTo(this, this::setSoundType);
	}

	@Override
	public Item getItemDropped(IBlockState state, Random random, int fortune) {
		return state.getValue(HALF) == EnumDoorHalf.UPPER ? net.minecraft.init.Items.AIR : Item.getItemFromBlock(this);
	}

	@Override
	public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
		return new ItemStack(Item.getItemFromBlock(this));
	}
}
