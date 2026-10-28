package io.github.xponer2.translator.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/**
 * Helpers of the 1.12.2 overrides the converter adds for the 1.21 use methods.
 */
public final class Interactions {
	private Interactions() {
	}

	/** The block hit of Block#onBlockActivated (hit position relative to the block). */
	public static RayTraceResult blockHit(BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ) {
		return new RayTraceResult(new Vec3d(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ), facing, pos);
	}

	/** True when the 1.12.2 block activation should stop (the item or block interaction did something). */
	public static boolean activated(ItemInteractionResult itemResult, net.minecraft.util.EnumActionResult blockResult) {
		if (itemResult == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
			return blockResult == net.minecraft.util.EnumActionResult.SUCCESS;
		}

		return itemResult.consumesAction() || itemResult == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
	}

	/** useWithoutItem of a block that doesn't declare one. */
	public static net.minecraft.util.EnumActionResult pass(Object state, Object world, BlockPos pos, EntityPlayer player, RayTraceResult hit) {
		return net.minecraft.util.EnumActionResult.PASS;
	}
}
