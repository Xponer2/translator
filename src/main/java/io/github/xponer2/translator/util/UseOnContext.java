package io.github.xponer2.translator.util;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 1.21 item use on a block. The converter adds an Item#onItemUse override to items declaring useOn, built with
 * {@link #of}.
 */
public class UseOnContext {
	@Nullable
	private final EntityPlayer player;
	private final EnumHand hand;
	private final RayTraceResult hitResult;
	private final World level;
	private final ItemStack itemStack;

	public UseOnContext(EntityPlayer player, EnumHand hand, RayTraceResult hitResult) {
		this(player.world, player, hand, player.getHeldItem(hand), hitResult);
	}

	public UseOnContext(World level, @Nullable EntityPlayer player, EnumHand hand, ItemStack itemStack, RayTraceResult hitResult) {
		this.player = player;
		this.hand = hand;
		this.hitResult = hitResult;
		this.itemStack = itemStack;
		this.level = level;
	}

	/** From the arguments of the 1.12.2 Item#onItemUse (hit position relative to the block). */
	public static UseOnContext of(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		var hit = new RayTraceResult(new Vec3d(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ), facing, pos);
		return new UseOnContext(world, player, hand, player.getHeldItem(hand), hit);
	}

	protected final RayTraceResult getHitResult() {
		return this.hitResult;
	}

	public BlockPos getClickedPos() {
		return this.hitResult.getBlockPos();
	}

	public EnumFacing getClickedFace() {
		return this.hitResult.sideHit;
	}

	public Vec3d getClickLocation() {
		return this.hitResult.hitVec;
	}

	/** Hit position relative to the clicked block (the 1.12.2 onItemUse arguments). */
	public float hitX() {
		return (float) (this.hitResult.hitVec.x - this.getClickedPos().getX());
	}

	public float hitY() {
		return (float) (this.hitResult.hitVec.y - this.getClickedPos().getY());
	}

	public float hitZ() {
		return (float) (this.hitResult.hitVec.z - this.getClickedPos().getZ());
	}

	public boolean isInside() {
		return false;
	}

	public ItemStack getItemInHand() {
		return this.itemStack;
	}

	/** getItemInHand under the name the AE2 rules give every `.getItemInHand(` call. */
	public ItemStack getHeldItem() {
		return this.itemStack;
	}

	@Nullable
	public EntityPlayer getPlayer() {
		return this.player;
	}

	public EnumHand getHand() {
		return this.hand;
	}

	public World getLevel() {
		return this.level;
	}

	public EnumFacing getHorizontalDirection() {
		return this.player == null ? EnumFacing.NORTH : this.player.getHorizontalFacing();
	}

	public boolean isSecondaryUseActive() {
		return this.player != null && this.player.isSneaking();
	}

	public float getRotation() {
		return this.player == null ? 0.0F : this.player.rotationYaw;
	}
}
