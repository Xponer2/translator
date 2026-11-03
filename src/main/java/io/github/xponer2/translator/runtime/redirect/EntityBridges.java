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

import java.util.function.Predicate;

import javax.annotation.Nullable;

import io.github.xponer2.translator.entity.Attribute;
import io.github.xponer2.translator.entity.AttributeInstance;
import io.github.xponer2.translator.registry.Holder;
import io.github.xponer2.translator.util.Attachments;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Team;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Static stand-ins for 1.21 methods of entities and players the 1.12.2 classes lack, and the NeoForge data
 * attachments of entities, block entities and levels (runtime, neoforge-1.21.1-redirects.tsv).
 */
public final class EntityBridges {
	private EntityBridges() {
	}

	// --- data attachments (IAttachmentHolder): the attachment type is a Supplier (DeferredHolder) or the type ---------

	public static Object getData(Entity entity, Object type) {
		return Attachments.get(entity, type);
	}

	public static Object setData(Entity entity, Object type, Object value) {
		return Attachments.set(entity, type, value);
	}

	public static boolean hasData(Entity entity, Object type) {
		return Attachments.has(entity, type);
	}

	public static Object removeData(Entity entity, Object type) {
		return Attachments.remove(entity, type);
	}

	public static Object getData(TileEntity tile, Object type) {
		return Attachments.get(tile, type);
	}

	public static Object setData(TileEntity tile, Object type, Object value) {
		tile.markDirty();
		return Attachments.set(tile, type, value);
	}

	public static boolean hasData(TileEntity tile, Object type) {
		return Attachments.has(tile, type);
	}

	public static Object removeData(TileEntity tile, Object type) {
		return Attachments.remove(tile, type);
	}

	public static Object getData(World world, Object type) {
		return Attachments.get(world, type);
	}

	public static Object setData(World world, Object type, Object value) {
		return Attachments.set(world, type, value);
	}

	public static boolean hasData(World world, Object type) {
		return Attachments.has(world, type);
	}

	// --- Entity ------------------------------------------------------------------------------------------------------

	public static io.github.xponer2.translator.registry.RegistryAccess registryAccess(Entity entity) {
		return io.github.xponer2.translator.registry.RegistryAccess.INSTANCE;
	}

	/** Entity capabilities (NeoForge EntityCapability): none on 1.12.2 entities. */
	@Nullable
	public static Object getCapability(Entity entity, Object capability) {
		return null;
	}

	@Nullable
	public static Object getCapability(Entity entity, Object capability, Object context) {
		return null;
	}

	public static boolean isUnderWater(Entity entity) {
		return entity.isInsideOfMaterial(Material.WATER);
	}

	public static boolean isInLiquid(Entity entity) {
		return entity.isInWater() || entity.isInLava();
	}

	public static boolean isInWaterOrRain(Entity entity) {
		return entity.isWet();
	}

	public static Vec3d getPosition(Entity entity, float partialTick) {
		return new Vec3d(entity.prevPosX + (entity.posX - entity.prevPosX) * partialTick, entity.prevPosY + (entity.posY - entity.prevPosY) * partialTick,
				entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTick);
	}

	public static Vec3d getLightProbePosition(Entity entity, float partialTick) {
		return entity.getPositionEyes(partialTick);
	}

	public static float getViewXRot(Entity entity, float partialTick) {
		return entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTick;
	}

	public static float getViewYRot(Entity entity, float partialTick) {
		return entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTick;
	}

	public static ChunkPos chunkPosition(Entity entity) {
		return new ChunkPos(MathHelper.floor(entity.posX) >> 4, MathHelper.floor(entity.posZ) >> 4);
	}

	public static BlockPos getOnPos(Entity entity) {
		return new BlockPos(entity.posX, entity.posY - 0.2, entity.posZ);
	}

	public static BlockPos getBlockPosBelowThatAffectsMyMovement(Entity entity) {
		return new BlockPos(entity.posX, entity.getEntityBoundingBox().minY - 0.5, entity.posZ);
	}

	@Nullable
	public static Team getTeam(Entity entity) {
		return entity.getTeam();
	}

	public static String getScoreboardName(Entity entity) {
		return entity instanceof EntityPlayer ? entity.getName() : entity.getCachedUniqueIdString();
	}

	public static float maxUpStep(Entity entity) {
		return entity.stepHeight;
	}

	public static void setMaxUpStep(Entity entity, float height) {
		entity.stepHeight = height;
	}

	public static void resetFallDistance(Entity entity) {
		entity.fallDistance = 0.0F;
	}

	public static void push(Entity entity, Vec3d motion) {
		entity.addVelocity(motion.x, motion.y, motion.z);
	}

	public static void push(Entity entity, double x, double y, double z) {
		entity.addVelocity(x, y, z);
	}

	public static void setOnGround(Entity entity, boolean onGround) {
		entity.onGround = onGround;
	}

	public static void moveTo(Entity entity, Vec3d pos) {
		entity.setLocationAndAngles(pos.x, pos.y, pos.z, entity.rotationYaw, entity.rotationPitch);
	}

	public static void moveTo(Entity entity, double x, double y, double z) {
		entity.setLocationAndAngles(x, y, z, entity.rotationYaw, entity.rotationPitch);
	}

	public static void setPosRaw(Entity entity, double x, double y, double z) {
		entity.posX = x;
		entity.posY = y;
		entity.posZ = z;
	}

	public static void move(Entity entity, net.minecraft.entity.MoverType type, Vec3d motion) {
		entity.move(type, motion.x, motion.y, motion.z);
	}

	public static Iterable<Entity> getIndirectPassengers(Entity entity) {
		return entity.getRecursivePassengers();
	}

	public static java.util.stream.Stream<Entity> getSelfAndPassengers(Entity entity) {
		return java.util.stream.Stream.concat(java.util.stream.Stream.of(entity), entity.getRecursivePassengers().stream());
	}

	public static Vec3d getDismountLocationForPassenger(Entity entity, EntityLivingBase passenger) {
		return new Vec3d(entity.posX, entity.getEntityBoundingBox().maxY, entity.posZ);
	}

	public static int getRemainingFireTicks(Entity entity) {
		return entity.isBurning() ? 20 : 0;
	}

	@Nullable
	public static net.minecraft.util.text.ITextComponent getCustomName(Entity entity) {
		return entity.hasCustomName() ? new net.minecraft.util.text.TextComponentString(entity.getCustomNameTag()) : null;
	}

	public static boolean isMultipartEntity(Entity entity) {
		return entity.getParts() != null;
	}

	public static boolean isAlwaysTicking(Entity entity) {
		return entity instanceof EntityPlayer;
	}

	public static boolean isAddedToLevel(Entity entity) {
		return entity.addedToChunk;
	}

	public static void lerpTo(Entity entity, double x, double y, double z, float yaw, float pitch, int steps) {
		entity.setPositionAndRotationDirect(x, y, z, yaw, pitch, steps, false);
	}

	// --- EntityLivingBase --------------------------------------------------------------------------------------------

	public static AttributeInstance getAttribute(EntityLivingBase entity, Holder<Attribute> attribute) {
		return AttributeInstance.of(entity, attribute);
	}

	public static double getAttributeValue(EntityLivingBase entity, Holder<Attribute> attribute) {
		var instance = AttributeInstance.of(entity, attribute);
		return instance == null ? 0.0 : instance.getValue();
	}

	public static double getAttributeBaseValue(EntityLivingBase entity, Holder<Attribute> attribute) {
		var instance = AttributeInstance.of(entity, attribute);
		return instance == null ? 0.0 : instance.getBaseValue();
	}

	public static boolean isDeadOrDying(EntityLivingBase entity) {
		return entity.getHealth() <= 0.0F;
	}

	public static boolean isHolding(EntityLivingBase entity, Predicate<ItemStack> test) {
		return test.test(entity.getHeldItemMainhand()) || test.test(entity.getHeldItemOffhand());
	}

	public static EntityEquipmentSlot getEquipmentSlotForItem(EntityLivingBase entity, ItemStack stack) {
		return EntityLiving.getSlotForItemStack(stack);
	}

	public static void swing(EntityLivingBase entity, EnumHand hand) {
		entity.swingArm(hand);
	}

	public static void swing(EntityLivingBase entity, EnumHand hand, boolean update) {
		entity.swingArm(hand);
	}

	public static float getScale(EntityLivingBase entity) {
		return 1.0F;
	}

	// --- EntityPlayer ------------------------------------------------------------------------------------------------

	/** Custom statistics (1.21 Stats.CUSTOM by id) aren't kept on 1.12.2. */
	public static void awardStat(EntityPlayer player, ResourceLocation stat) {
	}

	public static void awardStat(EntityPlayer player, ResourceLocation stat, int amount) {
	}

	public static void awardStat(EntityPlayer player, Object stat) {
	}

	public static boolean canInteractWithEntity(EntityPlayer player, Entity entity, double extra) {
		double reach = 3.0 + extra;
		return player.getDistanceSq(entity) < (reach + entity.width) * (reach + entity.width);
	}

	public static boolean mayInteract(EntityPlayer player, World world, BlockPos pos) {
		return world.isBlockModifiable(player, pos);
	}

	public static boolean hasCorrectToolForDrops(EntityPlayer player, IBlockState state, World world, BlockPos pos) {
		return net.minecraftforge.common.ForgeHooks.canHarvestBlock(state.getBlock(), player, world, pos);
	}

	public static boolean hasCorrectToolForDrops(EntityPlayer player, IBlockState state) {
		return state.getMaterial().isToolNotRequired() || player.getHeldItemMainhand().canHarvestBlock(state);
	}

	/** The arrows a bow shoots: the first arrow item in the inventory, an arrow in creative mode. */
	public static ItemStack getProjectile(EntityPlayer player, ItemStack weapon) {
		for (EnumHand hand : EnumHand.values()) {
			if (player.getHeldItem(hand).getItem() instanceof ItemArrow) {
				return player.getHeldItem(hand);
			}
		}
		for (ItemStack stack : player.inventory.mainInventory) {
			if (stack.getItem() instanceof ItemArrow) {
				return stack;
			}
		}
		return player.capabilities.isCreativeMode ? new ItemStack(Items.ARROW) : ItemStack.EMPTY;
	}

	public static boolean isScoping(EntityPlayer player) {
		return false;
	}

	public static void disableShield(EntityPlayer player) {
		player.getCooldownTracker().setCooldown(Items.SHIELD, 100);
		player.resetActiveHand();
	}

	public static void setIgnoreFallDamageFromCurrentImpulse(EntityPlayer player, boolean ignore) {
	}

	public static void updateTutorialInventoryAction(EntityPlayer player, ItemStack carried, ItemStack clicked, Object action) {
	}

	/** 1.21 {@code LivingEntity.walkAnimation}: the entity's limb swing. */
	public static io.github.xponer2.translator.entity.WalkAnimationState walkAnimation(net.minecraft.entity.EntityLivingBase entity) {
		return io.github.xponer2.translator.entity.WalkAnimationState.of(entity);
	}
}
