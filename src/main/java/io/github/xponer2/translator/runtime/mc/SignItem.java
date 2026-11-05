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

import io.github.xponer2.translator.item.ItemProperties;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Facade of the 1.21 {@code SignItem} (and hanging signs): it places the mod's standing sign on top of a block and its
 * wall sign on a side, turned to the player. A block item, as in 1.21 (a mod's code that makes its block items in one
 * method gives them as BlockItems: with signs among them that method didn't verify and the Aether had no block items).
 */
public class SignItem extends BlockItem {
	private final Block standing;
	private final Block wall;
	/** The face the standing sign hangs on: UP for signs (on top of a block), DOWN for hanging signs (under one). */
	private final EnumFacing attachment;

	public SignItem(ItemProperties properties, Block standing, Block wall) {
		this(properties, standing, wall, EnumFacing.UP);
	}

	/** 1.21 HangingSignItem's super call: the attachment face DOWN (the sign hangs under the block clicked). */
	public SignItem(ItemProperties properties, Block standing, Block wall, EnumFacing attachment) {
		super(standing != null ? standing : net.minecraft.init.Blocks.STANDING_SIGN, properties);
		this.standing = standing;
		this.wall = wall;
		this.attachment = attachment;
	}

	public SignItem(Block standing, Block wall, ItemProperties properties) {
		this(properties, standing, wall);
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (facing == this.attachment.getOpposite()) {
			return EnumActionResult.FAIL;
		}
		if (!world.getBlockState(pos).getBlock().isReplaceable(world, pos)) {
			pos = pos.offset(facing);
		}
		ItemStack stack = player.getHeldItem(hand);
		Block block = facing == this.attachment ? this.standing : this.wall;
		if (block == null || !player.canPlayerEdit(pos, facing, stack) || !block.canPlaceBlockAt(world, pos)) {
			return EnumActionResult.FAIL;
		}
		if (world.isRemote) {
			return EnumActionResult.SUCCESS;
		}
		IBlockState state = block.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, 0, player, hand);
		for (IProperty<?> property : state.getPropertyKeys()) {
			if (facing == this.attachment && property instanceof PropertyInteger rotation && "rotation".equals(property.getName())) {
				int angle = MathHelper.floor((180.0F + player.rotationYaw) * 16.0F / 360.0F + 0.5D) & 15;
				if (rotation.getAllowedValues().contains(angle)) {
					state = state.withProperty(rotation, angle);
				}
			} else if (facing != this.attachment && property instanceof PropertyDirection direction && "facing".equals(property.getName())
					&& direction.getAllowedValues().contains(facing)) {
				state = state.withProperty(direction, facing);
			}
		}
		world.setBlockState(pos, state, 11);
		if (!player.capabilities.isCreativeMode) {
			stack.shrink(1);
		}
		return EnumActionResult.SUCCESS;
	}
}
