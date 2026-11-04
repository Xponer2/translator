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

	/**
	 * 1.21 {@code getNearestLookingDirections}: the six directions in the order the player looks along them, the one
	 * into the clicked face first (buttons, levers and other blocks that attach to a face).
	 */
	public EnumFacing[] getNearestLookingDirections() {
		EnumFacing[] directions = this.getPlayer() == null
				? new EnumFacing[] {EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.UP}
				: orderedByNearest(this.getPlayer());
		EnumFacing into = this.getClickedFace().getOpposite();
		int i = 0;
		while (i < directions.length && directions[i] != into) {
			i++;
		}
		if (i > 0 && i < directions.length) {
			System.arraycopy(directions, 0, directions, 1, i);
			directions[0] = into;
		}
		return directions;
	}

	/** 1.21 {@code Direction.orderedByNearest}: the directions by how close they are to where the entity looks. */
	private static EnumFacing[] orderedByNearest(net.minecraft.entity.Entity entity) {
		float pitch = entity.rotationPitch * ((float) Math.PI / 180F);
		float yaw = -entity.rotationYaw * ((float) Math.PI / 180F);
		float sinPitch = net.minecraft.util.math.MathHelper.sin(pitch);
		float cosPitch = net.minecraft.util.math.MathHelper.cos(pitch);
		float sinYaw = net.minecraft.util.math.MathHelper.sin(yaw);
		float cosYaw = net.minecraft.util.math.MathHelper.cos(yaw);
		boolean east = sinYaw > 0.0F;
		boolean up = sinPitch < 0.0F;
		boolean south = cosYaw > 0.0F;
		float x = east ? sinYaw : -sinYaw;
		float y = up ? -sinPitch : sinPitch;
		float z = south ? cosYaw : -cosYaw;
		float xLook = x * cosPitch;
		float zLook = z * cosPitch;
		EnumFacing ew = east ? EnumFacing.EAST : EnumFacing.WEST;
		EnumFacing ud = up ? EnumFacing.UP : EnumFacing.DOWN;
		EnumFacing ns = south ? EnumFacing.SOUTH : EnumFacing.NORTH;
		if (x > z) {
			if (y > xLook) {
				return directions(ud, ew, ns);
			}
			return zLook > y ? directions(ew, ns, ud) : directions(ew, ud, ns);
		}
		if (y > zLook) {
			return directions(ud, ns, ew);
		}
		return xLook > y ? directions(ns, ew, ud) : directions(ns, ud, ew);
	}

	private static EnumFacing[] directions(EnumFacing a, EnumFacing b, EnumFacing c) {
		return new EnumFacing[] {a, b, c, c.getOpposite(), b.getOpposite(), a.getOpposite()};
	}
}
