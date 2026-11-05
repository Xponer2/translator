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
import net.minecraft.block.state.BlockStateContainer;
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

	/** 1.21 {@code DoorBlock.createBlockStateDefinition}: the door's properties; a mod's door adds its own to them. */
	protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
		builder.add(HALF, FACING, OPEN, HINGE, POWERED);
	}

	/** The state of a 1.21 door: what its createBlockStateDefinition adds (Create's sliding doors: visible). */
	@Override
	protected BlockStateContainer createBlockState() {
		var builder = new io.github.xponer2.translator.block.LazyStateContainer.Builder(this);
		this.createBlockStateDefinition(builder);
		return builder.build();
	}

	@Override
	public Item getItemDropped(IBlockState state, Random random, int fortune) {
		return state.getValue(HALF) == EnumDoorHalf.UPPER ? net.minecraft.init.Items.AIR : Item.getItemFromBlock(this);
	}

	@Override
	public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
		return new ItemStack(Item.getItemFromBlock(this));
	}

	/** The drops of the block's 1.21 loot table (when the mod has one), else 1.12.2's. */
	@Override
	public void getDrops(net.minecraft.util.NonNullList<net.minecraft.item.ItemStack> drops, net.minecraft.world.IBlockAccess world,
			net.minecraft.util.math.BlockPos pos, net.minecraft.block.state.IBlockState state, int fortune) {
		java.util.Random random = world instanceof net.minecraft.world.World level ? level.rand : RANDOM;
		if (!io.github.xponer2.translator.loot.BlockLoot.drop(io.github.xponer2.translator.loot.BlockLoot.tableOf(this, null), drops, world, pos, state,
				fortune, harvesters.get(), random)) {
			super.getDrops(drops, world, pos, state, fortune);
		}
	}
}
