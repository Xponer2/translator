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

import javax.annotation.Nullable;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 1.21 block placement: where a block is placed and by whom. Built from the arguments of the 1.12.2
 * Block#getStateForPlacement with {@link #of}.
 */
public class BlockPlaceContext extends UseOnContext {
	private final BlockPos placePos;

	public BlockPlaceContext(World level, @Nullable EntityPlayer player, EnumHand hand, ItemStack itemStack, RayTraceResult hitResult) {
		super(level, player, hand, itemStack, hitResult);
		this.placePos = hitResult.getBlockPos().offset(hitResult.sideHit);
	}

	public BlockPlaceContext(UseOnContext context) {
		this(context.getLevel(), context.getPlayer(), context.getHand(), context.getItemInHand(), context.getHitResult());
	}

	/**
	 * From Block#getStateForPlacement: pos is where the block goes, facing the clicked face of the block behind it.
	 */
	public static BlockPlaceContext of(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, EntityLivingBase placer, EnumHand hand) {
		var clicked = pos.offset(facing.getOpposite());
		var hit = new RayTraceResult(new Vec3d(clicked.getX() + hitX, clicked.getY() + hitY, clicked.getZ() + hitZ), facing, clicked);
		var player = placer instanceof EntityPlayer p ? p : null;
		var stack = placer.getHeldItem(hand);
		return new BlockPlaceContext(world, player, hand, stack, hit);
	}

	@Override
	public BlockPos getClickedPos() {
		return this.placePos;
	}

	public boolean canPlace() {
		return this.getLevel().getBlockState(this.placePos).getBlock().isReplaceable(this.getLevel(), this.placePos);
	}

	public boolean replacingClickedOnBlock() {
		return false;
	}

	public EnumFacing getNearestLookingDirection() {
		return this.getPlayer() == null ? EnumFacing.NORTH : EnumFacing.getDirectionFromEntityLiving(this.placePos, this.getPlayer());
	}
}
