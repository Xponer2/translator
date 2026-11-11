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

import javax.annotation.Nullable;

import io.github.xponer2.translator.entity.SynchedDataBuilder;
import io.github.xponer2.translator.registry.EntityType;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

/**
 * 1.21 {@code AbstractArrow} for the arrows of 1.21.1 mods that extend it: made with their EntityType and the stack
 * they are picked up as (1.12.2's arrow takes the world, a position or a shooter: none of the mods' arrow constructors
 * could run, so their arrows, darts and bolts didn't exist), their synched data after 1.12.2's arrow's, and 1.21's two
 * hit methods called from 1.12.2's one.
 */
public abstract class AbstractArrow extends EntityArrow {
	@Nullable
	private final EntityType<?> type;
	private ItemStack pickupItemStack = ItemStack.EMPTY;
	@Nullable
	private ItemStack firedFromWeapon;

	protected AbstractArrow(EntityType<?> type, World world) {
		super(world);
		this.type = type;
		this.sized();
	}

	protected AbstractArrow(EntityType<?> type, World world, ItemStack pickup, @Nullable ItemStack weapon) {
		this(type, world);
		this.carry(pickup, weapon);
	}

	protected AbstractArrow(EntityType<?> type, double x, double y, double z, World world, ItemStack pickup, @Nullable ItemStack weapon) {
		super(world, x, y, z);
		this.type = type;
		this.sized();
		this.carry(pickup, weapon);
	}

	protected AbstractArrow(EntityType<?> type, EntityLivingBase owner, World world, ItemStack pickup, @Nullable ItemStack weapon) {
		super(world, owner);
		this.type = type;
		this.sized();
		this.carry(pickup, weapon);
	}

	private void sized() {
		if (this.type != null && this.type.getWidth() > 0.0F && this.type.getHeight() > 0.0F) {
			this.setSize(this.type.getWidth(), this.type.getHeight());
		}
	}

	private void carry(@Nullable ItemStack pickup, @Nullable ItemStack weapon) {
		this.pickupItemStack = pickup == null ? ItemStack.EMPTY : pickup.copy();
		this.firedFromWeapon = weapon == null || weapon.isEmpty() ? null : weapon.copy();
	}

	public EntityType<?> getType() {
		return this.type;
	}

	/** 1.21 defineSynchedData: the mod's data after 1.12.2's arrow's. */
	protected void defineSynchedData(SynchedDataBuilder builder) {
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		this.defineSynchedData(new SynchedDataBuilder(this.dataManager));
	}

	/** 1.21 {@code getDefaultPickupItem()}: what the arrow is when it carries no stack of its own (a mod's arrow names its item). */
	protected ItemStack getDefaultPickupItem() {
		return new ItemStack(Items.ARROW);
	}

	/** 1.12.2 asks this when the arrow is picked up (1.21 {@code getPickupItem()}, the table's name for it). */
	@Override
	protected ItemStack getArrowStack() {
		return this.pickupItemStack.isEmpty() ? this.getDefaultPickupItem() : this.pickupItemStack.copy();
	}

	/** 1.21 {@code getPickupItemStackOrigin()}: the stack the arrow was made of. */
	public ItemStack getPickupItemStackOrigin() {
		return this.pickupItemStack;
	}

	/** 1.21 {@code setPickupItemStack(stack)}. */
	protected void setPickupItemStack(ItemStack stack) {
		this.pickupItemStack = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
	}

	/** 1.21 {@code getWeaponItem()}: the bow or crossbow that shot it, null when none is known. */
	@Nullable
	public ItemStack getWeaponItem() {
		return this.firedFromWeapon;
	}

	/** 1.21 {@code getOwner()}: who shot it (1.12.2's shooting entity). */
	@Nullable
	public net.minecraft.entity.Entity getOwner() {
		return this.shootingEntity;
	}

	/** 1.21 {@code setOwner(owner)}: who shot it; a player's arrow may be picked up, as 1.12.2's shooter-constructor sets. */
	public void setOwner(@Nullable net.minecraft.entity.Entity owner) {
		this.shootingEntity = owner;
		if (owner instanceof net.minecraft.entity.player.EntityPlayer) {
			this.pickupStatus = PickupStatus.ALLOWED;
		}
	}

	/** 1.21 {@code canHitEntity(target)}: a living, collidable entity other than its shooter in its first ticks. */
	protected boolean canHitEntity(net.minecraft.entity.Entity target) {
		if (target == null || !target.canBeCollidedWith() || target.isDead) {
			return false;
		}
		return target != this.shootingEntity || this.ticksExisted >= 5;
	}

	/** 1.21 {@code isNoPhysics()}: it flies through blocks (1.12.2's noClip). */
	public boolean isNoPhysics() {
		return this.noClip;
	}

	/** 1.21 {@code setNoPhysics(noPhysics)}. */
	public void setNoPhysics(boolean noPhysics) {
		this.noClip = noPhysics;
	}

	/** 1.21 {@code shootFromRotation(shooter, pitch, yaw, roll, velocity, inaccuracy)}: 1.12.2's shoot from a shooter's look. */
	public void shootFromRotation(net.minecraft.entity.Entity shooter, float pitch, float yaw, float roll, float velocity, float inaccuracy) {
		this.shoot(shooter, pitch, yaw, roll, velocity, inaccuracy);
	}

	/** 1.21 {@code getDefaultHitGroundSoundEvent()}: the sound of an arrow sticking in a block. */
	protected net.minecraft.util.SoundEvent getDefaultHitGroundSoundEvent() {
		return net.minecraft.init.SoundEvents.ENTITY_ARROW_HIT;
	}

	/**
	 * 1.21 {@code doKnockback(target, source)}: the knockback of the weapon's enchantment; 1.12.2's arrow applies its own
	 * (its knockback strength) in its hit, a mod's arrow has none to add here.
	 */
	protected void doKnockback(net.minecraft.entity.EntityLivingBase target, net.minecraft.util.DamageSource source) {
	}

	/** 1.21 {@code findHitEntity(from, to)}: the entity hit between the two points, as a hit result. */
	@Nullable
	protected RayTraceResult findHitEntity(net.minecraft.util.math.Vec3d from, net.minecraft.util.math.Vec3d to) {
		net.minecraft.entity.Entity hit = super.findEntityOnPath(from, to);
		return hit == null ? null : new RayTraceResult(hit);
	}

	/** 1.21 {@code tickDespawn()}: 1.12.2's arrow despawns by itself after a minute in the ground. */
	protected void tickDespawn() {
	}

	/** 1.21 {@code tryPickup(player)}: the arrow into the player's inventory when its pickup rule allows it. */
	protected boolean tryPickup(net.minecraft.entity.player.EntityPlayer player) {
		switch (this.pickupStatus) {
			case ALLOWED:
				return player.inventory.addItemStackToInventory(this.getArrowStack());
			case CREATIVE_ONLY:
				return player.capabilities.isCreativeMode;
			default:
				return false;
		}
	}

	/** 1.12.2 has one hit method; 1.21's arrows override the entity's and the block's. */
	@Override
	protected void onHit(RayTraceResult hit) {
		if (hit != null && hit.entityHit != null) {
			this.onHitEntity(hit);
		} else {
			this.onHitBlock(hit);
		}
	}

	/** 1.21 {@code onHitEntity(result)}: 1.12.2's arrow's own hit (damage, knockback, arrowHit). */
	protected void onHitEntity(RayTraceResult hit) {
		super.onHit(hit);
	}

	/** 1.21 {@code onHitBlock(result)}: 1.12.2's arrow's own hit (it sticks in the block). */
	protected void onHitBlock(RayTraceResult hit) {
		super.onHit(hit);
	}
}
