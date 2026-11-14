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

import java.util.UUID;

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.EntityType;
import io.github.xponer2.translator.runtime.redirect.EntityGapBridges;
import io.github.xponer2.translator.runtime.redirect.LevelGapBridges;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.entity.projectile.ProjectileDeflection;

/**
 * 1.21 {@code Projectile} for the projectiles that extend it: the mods' own and the carried 1.21 throwables, fireballs,
 * fishing hook, shulker bullet and firework (the E line). Everywhere else the table maps Projectile to 1.12.2's Entity,
 * so 1.12.2's arrows are projectiles too: a bow's {@code createProjectile} returns one, a dispenser's
 * {@code asProjectile} too (as Projectile, the carried class, they were not, and the bows of Twilight Forest, the darts
 * of the Aether and Supplementaries' rope arrows didn't verify). An owner (kept by its UUID, saved), whether it has left
 * its owner, being shot along a vector or from a rotation, its two hit methods called from one, the owner sent to the
 * client in the spawn data.
 */
public abstract class Projectile extends Entity implements net.minecraft.entity.IProjectile, net.minecraft.world.entity.TraceableEntity,
		net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData {
	@Nullable
	public UUID ownerUUID;
	@Nullable
	public net.minecraft.entity.Entity cachedOwner;
	public boolean leftOwner;
	public boolean hasBeenShot;
	@Nullable
	public net.minecraft.entity.Entity lastDeflectedBy;

	protected Projectile(EntityType<?> type, World world) {
		super(type, world);
	}

	// --- the owner ------------------------------------------------------------------------------------------------------

	public void setOwner(@Nullable net.minecraft.entity.Entity owner) {
		if (owner != null) {
			this.ownerUUID = owner.getUniqueID();
			this.cachedOwner = owner;
		}
	}

	/** The owner: the one known, or found again on the server by its UUID (after loading). */
	@Override
	@Nullable
	public net.minecraft.entity.Entity getOwner() {
		if (this.cachedOwner != null && !this.cachedOwner.isDead) {
			return this.cachedOwner;
		}
		if (this.ownerUUID != null && this.world instanceof WorldServer server) {
			this.cachedOwner = server.getEntityFromUuid(this.ownerUUID);
			return this.cachedOwner;
		}
		return null;
	}

	/** Who a hit is credited to: the owner, else the projectile. */
	public net.minecraft.entity.Entity getEffectSource() {
		net.minecraft.entity.Entity owner = this.getOwner();
		return owner != null ? owner : this;
	}

	public boolean ownedBy(@Nullable net.minecraft.entity.Entity entity) {
		return entity != null && this.ownerUUID != null && this.ownerUUID.equals(entity.getUniqueID());
	}

	/** Whether nothing the owner rides with is still close (until then it doesn't hit its owner's vehicle). */
	public boolean checkLeftOwner() {
		net.minecraft.entity.Entity owner = this.getOwner();
		if (owner == null) {
			return true;
		}
		net.minecraft.entity.Entity ownerRoot = owner.getLowestRidingEntity();
		AxisAlignedBB around = this.getEntityBoundingBox().expand(this.motionX, this.motionY, this.motionZ).grow(1.0D);
		for (net.minecraft.entity.Entity near : this.world.getEntitiesInAABBexcluding(this, around,
				e -> e != null && !(e instanceof EntityPlayer player && player.isSpectator()) && e.canBeCollidedWith())) {
			if (near.getLowestRidingEntity() == ownerRoot) {
				return false;
			}
		}
		return true;
	}

	// --- saved data, tick -----------------------------------------------------------------------------------------------

	@Override
	public void addAdditionalSaveData(NBTTagCompound tag) {
		if (this.ownerUUID != null) {
			tag.setUniqueId("Owner", this.ownerUUID);
		}
		if (this.leftOwner) {
			tag.setBoolean("LeftOwner", true);
		}
		tag.setBoolean("HasBeenShot", this.hasBeenShot);
	}

	@Override
	public void readAdditionalSaveData(NBTTagCompound tag) {
		if (tag.hasUniqueId("Owner")) {
			this.ownerUUID = tag.getUniqueId("Owner");
			this.cachedOwner = null;
		}
		this.leftOwner = tag.getBoolean("LeftOwner");
		this.hasBeenShot = tag.getBoolean("HasBeenShot");
	}

	/** 1.21 restoreFrom (1.12.2's copyDataFromOld is private): the owner comes along. */
	public void copyDataFromOld(net.minecraft.entity.Entity old) {
		if (old instanceof Projectile projectile) {
			this.cachedOwner = projectile.cachedOwner;
		}
	}

	@Override
	public void tick() {
		this.hasBeenShot = true;
		if (!this.leftOwner) {
			this.leftOwner = this.checkLeftOwner();
		}
		super.tick();
	}

	// --- shooting -------------------------------------------------------------------------------------------------------

	/** The motion of a shot along a vector: its direction, spread by the inaccuracy, at the velocity. */
	public Vec3d getMovementToShoot(double x, double y, double z, float velocity, float inaccuracy) {
		double spread = 0.0172275D * inaccuracy;
		return new Vec3d(x, y, z).normalize().add(this.spread(spread), this.spread(spread), this.spread(spread)).scale(velocity);
	}

	private double spread(double spread) {
		return (this.rand.nextDouble() - this.rand.nextDouble()) * spread;
	}

	@Override
	public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
		Vec3d motion = this.getMovementToShoot(x, y, z, velocity, inaccuracy);
		this.motionX = motion.x;
		this.motionY = motion.y;
		this.motionZ = motion.z;
		this.faceMotion();
		this.prevRotationYaw = this.rotationYaw;
		this.prevRotationPitch = this.rotationPitch;
	}

	/** Shot the way a shooter looks (pitch, yaw, an upward roll), with the shooter's own motion added. */
	public void shootFromRotation(net.minecraft.entity.Entity shooter, float pitch, float yaw, float roll, float velocity, float inaccuracy) {
		float toRadians = (float) Math.PI / 180.0F;
		float x = -MathHelper.sin(yaw * toRadians) * MathHelper.cos(pitch * toRadians);
		float y = -MathHelper.sin((pitch + roll) * toRadians);
		float z = MathHelper.cos(yaw * toRadians) * MathHelper.cos(pitch * toRadians);
		this.shoot(x, y, z, velocity, inaccuracy);
		this.motionX += shooter.motionX;
		this.motionZ += shooter.motionZ;
		if (!shooter.onGround) {
			this.motionY += shooter.motionY;
		}
	}

	private void faceMotion() {
		double horizontal = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
		this.rotationYaw = (float) (MathHelper.atan2(this.motionX, this.motionZ) * (180.0D / Math.PI));
		this.rotationPitch = (float) (MathHelper.atan2(this.motionY, horizontal) * (180.0D / Math.PI));
	}

	/** Turns toward its motion, a fifth of the way each tick. */
	public void updateRotation() {
		double horizontal = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
		this.rotationPitch = lerpRotation(this.prevRotationPitch, (float) (MathHelper.atan2(this.motionY, horizontal) * (180.0D / Math.PI)));
		this.rotationYaw = lerpRotation(this.prevRotationYaw, (float) (MathHelper.atan2(this.motionX, this.motionZ) * (180.0D / Math.PI)));
	}

	public static float lerpRotation(float from, float to) {
		while (to - from < -180.0F) {
			from -= 360.0F;
		}
		while (to - from >= 180.0F) {
			from += 360.0F;
		}
		return from + 0.2F * (to - from);
	}

	/** 1.21 lerpMotion: the client's motion from the server, facing it when it had no rotation yet. */
	@Override
	public void setVelocity(double x, double y, double z) {
		this.motionX = x;
		this.motionY = y;
		this.motionZ = z;
		if (this.prevRotationPitch == 0.0F && this.prevRotationYaw == 0.0F) {
			this.faceMotion();
			this.prevRotationYaw = this.rotationYaw;
			this.prevRotationPitch = this.rotationPitch;
			this.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
		}
	}

	// --- hitting --------------------------------------------------------------------------------------------------------

	/** Hits what it hit (no 1.12.2 entity deflects a projectile, so none is turned away). */
	@Nullable
	public ProjectileDeflection hitTargetOrDeflectSelf(RayTraceResult hit) {
		this.onHit(hit);
		return ProjectileDeflection.NONE;
	}

	public boolean deflect(ProjectileDeflection deflection, @Nullable net.minecraft.entity.Entity entity, @Nullable net.minecraft.entity.Entity owner,
			boolean byAttack) {
		io.github.xponer2.translator.runtime.redirect.ProjectileBridges.turn(deflection, this, entity);
		if (!this.world.isRemote) {
			this.setOwner(owner);
			this.onDeflection(entity, byAttack);
		}
		return true;
	}

	public void onDeflection(@Nullable net.minecraft.entity.Entity entity, boolean byAttack) {
	}

	/** An entity hit or a block hit, to its method. */
	public void onHit(RayTraceResult hit) {
		if (hit.typeOfHit == RayTraceResult.Type.ENTITY) {
			this.onHitEntity(hit);
		} else if (hit.typeOfHit == RayTraceResult.Type.BLOCK) {
			this.onHitBlock(hit);
		}
	}

	public void onHitEntity(RayTraceResult hit) {
	}

	/** The block's own reaction to the hit (a 1.21 block of a mod: a target, a bell). */
	public void onHitBlock(RayTraceResult hit) {
		IBlockState state = this.world.getBlockState(hit.getBlockPos());
		LevelGapBridges.onProjectileHit(state, this.world, state, hit, this);
	}

	/** Living (or at least pickable) things, not its owner's vehicle until it has left it. */
	public boolean canHitEntity(net.minecraft.entity.Entity target) {
		if (target.isDead || !target.canBeCollidedWith() || target instanceof EntityPlayer player && player.isSpectator()) {
			return false;
		}
		net.minecraft.entity.Entity owner = this.getOwner();
		return owner == null || this.leftOwner || !owner.isRidingSameEntity(target);
	}

	/** A player owner as the player may, else the mob griefing rule (Forge's event). */
	public boolean mayInteract(World world, BlockPos pos) {
		net.minecraft.entity.Entity owner = this.getOwner();
		if (owner instanceof EntityPlayer player) {
			return player.capabilities.allowEdit && world.isBlockModifiable(player, pos);
		}
		return owner == null || net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(world, this);
	}

	public boolean mayBreak(World world) {
		return net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(world, this);
	}

	// --- the client -----------------------------------------------------------------------------------------------------

	/** 1.21's spawn packet: its owner's id as the data. */
	public net.minecraft.network.Packet<?> getAddEntityPacket(@Nullable net.minecraft.server.level.ServerEntity serverEntity) {
		net.minecraft.entity.Entity owner = this.getOwner();
		return new net.minecraft.network.play.server.SPacketSpawnObject(this, 0, owner == null ? 0 : owner.getEntityId());
	}

	/** On the client: the owner from the spawn data. */
	public void recreateFromPacket(net.minecraft.network.play.server.SPacketSpawnObject packet) {
		net.minecraft.entity.Entity owner = this.world.getEntityByID(packet.getData());
		if (owner != null) {
			this.setOwner(owner);
		}
	}

	/** The spawn data a mod's projectile puts in its spawn packet (its owner's id by default), sent with FML's message. */
	@Override
	public void writeSpawnData(ByteBuf buffer) {
		buffer.writeInt(EntityGapBridges.spawnPacketData(this));
	}

	@Override
	public void readSpawnData(ByteBuf buffer) {
		EntityGapBridges.recreateFromSpawnData(this, buffer.readInt());
	}
}
