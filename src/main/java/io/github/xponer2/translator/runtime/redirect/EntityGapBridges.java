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
package io.github.xponer2.translator.runtime.redirect;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Entity calls of 1.21.1 mods that 1.12.2 entities lack (getters and small state; what 1.12.2 doesn't have, like
 * freezing or spin attacks, answers as an entity without it).
 */
public final class EntityGapBridges {
	private EntityGapBridges() {
	}

	// --- Entity ---------------------------------------------------------------------------------------------------------

	public static boolean canBeHitByProjectile(Entity entity) {
		return entity.isEntityAlive() && entity.canBeCollidedWith();
	}

	/** Where a lead is tied (1.21: a bit below the eyes). */
	public static Vec3d getRopeHoldPosition(Entity entity, float partialTicks) {
		double x = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
		double y = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
		double z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
		return new Vec3d(x, y + entity.getEyeHeight() * 0.7D, z);
	}

	public static boolean isInWaterRainOrBubble(Entity entity) {
		return entity.isWet();
	}

	/** 1.21 calls it when the entity leaves the world; 1.12.2 entities have nothing to do then. */
	public static void onRemovedFromLevel(Entity entity) {
	}

	/** 1.21 {@code setRemoved(reason)}: the entity is gone (killed, discarded, unloaded, changed dimension). */
	public static void setRemoved(Entity entity, Object reason) {
		entity.setDead();
	}

	/** 1.21 portals start a dimension change by this; 1.12.2 portals are blocks that do it themselves. */
	public static void setAsInsidePortal(Entity entity, Object portal, BlockPos pos) {
	}

	public static IBlockState getBlockStateOn(Entity entity) {
		return entity.world.getBlockState(new BlockPos(entity.posX, entity.posY - 0.2D, entity.posZ));
	}

	public static Vec3d getKnownMovement(Entity entity) {
		return new Vec3d(entity.motionX, entity.motionY, entity.motionZ);
	}

	public static double getRandomX(Entity entity, double scale) {
		return entity.posX + entity.width * (2.0D * entity.world.rand.nextDouble() - 1.0D) * scale;
	}

	public static double getRandomY(Entity entity) {
		return entity.posY + entity.height * entity.world.rand.nextDouble();
	}

	public static double getRandomZ(Entity entity, double scale) {
		return entity.posZ + entity.width * (2.0D * entity.world.rand.nextDouble() - 1.0D) * scale;
	}

	/** The up direction of the entity's view (its look direction turned up by 90 degrees). */
	public static Vec3d getUpVector(Entity entity, float partialTicks) {
		float pitch = (entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks) - 90.0F;
		float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
		float yawCos = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
		float yawSin = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
		float pitchCos = -MathHelper.cos(-pitch * 0.017453292F);
		float pitchSin = MathHelper.sin(-pitch * 0.017453292F);
		return new Vec3d(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
	}

	// --- LivingEntity ---------------------------------------------------------------------------------------------------

	/** 1.21: alive, not invulnerable, not a spectator or creative player. */
	public static boolean canBeSeenAsEnemy(EntityLivingBase living) {
		if (living instanceof EntityPlayer player && (player.isSpectator() || player.isCreative())) {
			return false;
		}
		return living.isEntityAlive() && !living.getIsInvulnerable();
	}

	/** 1.12.2 has no freezing (powder snow). */
	public static boolean canFreeze(EntityLivingBase living) {
		return false;
	}

	public static AxisAlignedBB getHitbox(EntityLivingBase living) {
		return living.getEntityBoundingBox();
	}

	/** 1.12.2 has no riptide spin attack. */
	public static boolean isAutoSpinAttack(EntityLivingBase living) {
		return false;
	}

	/** 1.21 {@code lookAt(anchor, target)}: the head turned to the target, from the eyes. */
	public static void lookAt(EntityLivingBase living, Object anchor, Vec3d target) {
		double dx = target.x - living.posX;
		double dy = target.y - (living.posY + living.getEyeHeight());
		double dz = target.z - living.posZ;
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		living.rotationPitch = MathHelper.wrapDegrees((float) -(MathHelper.atan2(dy, horizontal) * 57.2957763671875D));
		living.rotationYaw = MathHelper.wrapDegrees((float) (MathHelper.atan2(dz, dx) * 57.2957763671875D) - 90.0F);
		living.rotationYawHead = living.rotationYaw;
		living.prevRotationPitch = living.rotationPitch;
		living.prevRotationYaw = living.rotationYaw;
	}

	// --- Mob, animals ---------------------------------------------------------------------------------------------------

	/** 1.21's aggressive flag only drives arm poses: 1.12.2 models don't have them. */
	public static void setAggressive(net.minecraft.entity.EntityLiving mob, boolean aggressive) {
	}

	public static boolean isOrderedToSit(net.minecraft.entity.passive.EntityTameable animal) {
		return animal.isSitting();
	}

	public static boolean canFallInLove(net.minecraft.entity.passive.EntityAnimal animal) {
		return !animal.isInLove();
	}

	public static IBlockState getCarriedBlock(net.minecraft.entity.monster.EntityEnderman enderman) {
		return enderman.getHeldBlockState();
	}

	/** 1.21 {@code navigation.isStuck()}: 1.12.2 navigation gives up instead of reporting it. */
	public static boolean isStuck(net.minecraft.pathfinding.PathNavigate navigation) {
		return false;
	}

	// --- Player ---------------------------------------------------------------------------------------------------------

	public static Iterable<ItemStack> getAllSlots(EntityLivingBase living) {
		return living.getEquipmentAndArmor();
	}

	/** 1.12.2 has no crawling or swimming pose. */
	public static boolean isVisuallySwimming(EntityLivingBase living) {
		return false;
	}

	public static BlockPos getRespawnPosition(net.minecraft.entity.player.EntityPlayerMP player) {
		return player.getBedLocation();
	}

	/** 1.21 {@code blockActionRestricted(level, pos, gameType)}: spectators can't, adventure players only where allowed. */
	public static boolean blockActionRestricted(EntityPlayer player, net.minecraft.world.World level, BlockPos pos, net.minecraft.world.GameType type) {
		if (type == net.minecraft.world.GameType.SPECTATOR) {
			return true;
		}
		return type == net.minecraft.world.GameType.ADVENTURE && !player.isAllowEdit();
	}

	// --- lightning ------------------------------------------------------------------------------------------------------

	/** 1.12.2 bolts have no cause or damage of their own (fire and 5 damage). */
	public static void setCause(net.minecraft.entity.effect.EntityLightningBolt bolt, net.minecraft.entity.player.EntityPlayerMP cause) {
	}

	public static void setDamage(net.minecraft.entity.effect.EntityLightningBolt bolt, float damage) {
	}
}
