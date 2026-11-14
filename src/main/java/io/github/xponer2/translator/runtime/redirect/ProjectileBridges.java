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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.annotation.Nullable;

import io.github.xponer2.translator.runtime.mc.Projectile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.entity.projectile.EntityLlamaSpit;
import net.minecraft.entity.projectile.EntityShulkerBullet;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

/**
 * 1.21's Projectile on 1.12.2's projectiles: the table maps Projectile to Entity (a bow's arrow is a projectile in 1.21,
 * here a 1.12.2 EntityArrow), and the calls the mods make on a projectile come here: the facade's own methods for the
 * mods' and the carried projectiles (they extend {@link Projectile}), and their meaning for 1.12.2's arrows,
 * throwables, fireballs, llama spit and fishing hook.
 */
public final class ProjectileBridges {
	private ProjectileBridges() {
	}

	/** {@code entity instanceof Projectile} (the table's I line): the facade's and 1.12.2's projectiles. */
	public static boolean isProjectile(@Nullable Object entity) {
		return entity instanceof Projectile || entity instanceof IProjectile || entity instanceof EntityFireball || entity instanceof EntityFishHook
				|| entity instanceof EntityShulkerBullet || entity instanceof EntityFireworkRocket;
	}

	@Nullable
	public static Entity getOwner(Entity projectile) {
		if (projectile instanceof Projectile own) {
			return own.getOwner();
		}
		if (projectile instanceof io.github.xponer2.translator.runtime.mc.AbstractArrow arrow) {
			return arrow.getOwner();
		}
		if (projectile instanceof EntityArrow arrow) {
			return arrow.shootingEntity;
		}
		if (projectile instanceof EntityThrowable throwable) {
			return throwable.getThrower();
		}
		if (projectile instanceof EntityFireball fireball) {
			return fireball.shootingEntity;
		}
		if (projectile instanceof EntityFishHook hook) {
			return hook.getAngler();
		}
		if (projectile instanceof EntityLlamaSpit spit) {
			return spit.owner;
		}
		return null;
	}

	public static void setOwner(Entity projectile, @Nullable Entity owner) {
		if (projectile instanceof Projectile own) {
			own.setOwner(owner);
		} else if (projectile instanceof io.github.xponer2.translator.runtime.mc.AbstractArrow arrow) {
			arrow.setOwner(owner);
		} else if (projectile instanceof EntityArrow arrow) {
			arrow.shootingEntity = owner;
		} else if (projectile instanceof EntityFireball fireball) {
			if (owner == null || owner instanceof EntityLivingBase) {
				fireball.shootingEntity = (EntityLivingBase) owner;
			}
		} else if (projectile instanceof EntityThrowable throwable) {
			if (owner == null || owner instanceof EntityLivingBase) {
				setField(EntityThrowable.class, "field_70192_c", throwable, owner);
			}
		} else if (projectile instanceof EntityLlamaSpit spit) {
			if (owner == null || owner instanceof EntityLlama) {
				spit.owner = (EntityLlama) owner;
			}
		}
	}

	public static Entity getEffectSource(Entity projectile) {
		Entity owner = getOwner(projectile);
		return owner != null ? owner : projectile;
	}

	public static boolean ownedBy(Entity projectile, @Nullable Entity entity) {
		if (projectile instanceof Projectile own) {
			return own.ownedBy(entity);
		}
		return entity != null && getOwner(projectile) == entity;
	}

	/** Shot along a vector: 1.12.2's own shot for its projectiles (a fireball keeps flying that way), else the motion. */
	public static void shoot(Entity projectile, double x, double y, double z, float velocity, float inaccuracy) {
		if (projectile instanceof IProjectile shot) {
			shot.shoot(x, y, z, velocity, inaccuracy);
			return;
		}
		Vec3d motion = getMovementToShoot(projectile, x, y, z, velocity, inaccuracy);
		projectile.motionX = motion.x;
		projectile.motionY = motion.y;
		projectile.motionZ = motion.z;
		if (projectile instanceof EntityFireball fireball) {
			double length = motion.length();
			if (length > 0.0D) {
				fireball.accelerationX = motion.x / length * 0.1D;
				fireball.accelerationY = motion.y / length * 0.1D;
				fireball.accelerationZ = motion.z / length * 0.1D;
			}
		}
		double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
		projectile.rotationYaw = (float) (MathHelper.atan2(motion.x, motion.z) * (180.0D / Math.PI));
		projectile.rotationPitch = (float) (MathHelper.atan2(motion.y, horizontal) * (180.0D / Math.PI));
		projectile.prevRotationYaw = projectile.rotationYaw;
		projectile.prevRotationPitch = projectile.rotationPitch;
	}

	/** Shot the way a shooter looks: 1.12.2's own for an arrow or a throwable (the bows of the mods shoot 1.12.2 arrows). */
	public static void shootFromRotation(Entity projectile, Entity shooter, float pitch, float yaw, float roll, float velocity, float inaccuracy) {
		if (projectile instanceof Projectile own) {
			own.shootFromRotation(shooter, pitch, yaw, roll, velocity, inaccuracy);
		} else if (projectile instanceof EntityArrow arrow) {
			arrow.shoot(shooter, pitch, yaw, roll, velocity, inaccuracy);
		} else if (projectile instanceof EntityThrowable throwable) {
			throwable.shoot(shooter, pitch, yaw, roll, velocity, inaccuracy);
		} else {
			float toRadians = (float) Math.PI / 180.0F;
			shoot(projectile, -MathHelper.sin(yaw * toRadians) * MathHelper.cos(pitch * toRadians), -MathHelper.sin((pitch + roll) * toRadians),
					MathHelper.cos(yaw * toRadians) * MathHelper.cos(pitch * toRadians), velocity, inaccuracy);
			projectile.motionX += shooter.motionX;
			projectile.motionZ += shooter.motionZ;
			if (!shooter.onGround) {
				projectile.motionY += shooter.motionY;
			}
		}
	}

	public static Vec3d getMovementToShoot(Entity projectile, double x, double y, double z, float velocity, float inaccuracy) {
		if (projectile instanceof Projectile own) {
			return own.getMovementToShoot(x, y, z, velocity, inaccuracy);
		}
		double spread = 0.0172275D * inaccuracy;
		java.util.Random random = projectile.world != null ? projectile.world.rand : new java.util.Random();
		return new Vec3d(x, y, z).normalize()
				.add((random.nextDouble() - random.nextDouble()) * spread, (random.nextDouble() - random.nextDouble()) * spread,
						(random.nextDouble() - random.nextDouble()) * spread)
				.scale(velocity);
	}

	public static boolean canHitEntity(Entity projectile, Entity target) {
		if (projectile instanceof Projectile own) {
			return own.canHitEntity(target);
		}
		if (target.isDead || !target.canBeCollidedWith() || target instanceof EntityPlayer player && player.isSpectator()) {
			return false;
		}
		Entity owner = getOwner(projectile);
		return owner == null || target != owner || projectile.ticksExisted >= 5;
	}

	public static boolean checkLeftOwner(Entity projectile) {
		return !(projectile instanceof Projectile own) || own.checkLeftOwner();
	}

	public static void updateRotation(Entity projectile) {
		if (projectile instanceof Projectile own) {
			own.updateRotation();
			return;
		}
		double horizontal = Math.sqrt(projectile.motionX * projectile.motionX + projectile.motionZ * projectile.motionZ);
		projectile.rotationPitch = Projectile.lerpRotation(projectile.prevRotationPitch,
				(float) (MathHelper.atan2(projectile.motionY, horizontal) * (180.0D / Math.PI)));
		projectile.rotationYaw = Projectile.lerpRotation(projectile.prevRotationYaw,
				(float) (MathHelper.atan2(projectile.motionX, projectile.motionZ) * (180.0D / Math.PI)));
	}

	/** May it change a block there: a player owner as the player may, else the mob griefing rule. */
	public static boolean mayInteract(Entity projectile, World world, BlockPos pos) {
		if (projectile instanceof Projectile own) {
			return own.mayInteract(world, pos);
		}
		if (projectile instanceof EntityPlayer player) {
			return player.capabilities.allowEdit && world.isBlockModifiable(player, pos);
		}
		Entity owner = getOwner(projectile);
		if (owner instanceof EntityPlayer player) {
			return player.capabilities.allowEdit && world.isBlockModifiable(player, pos);
		}
		return owner == null || net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(world, projectile);
	}

	public static boolean mayBreak(Entity projectile, World world) {
		if (projectile instanceof Projectile own) {
			return own.mayBreak(world);
		}
		return net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(world, projectile);
	}

	public static boolean deflect(Entity projectile, Object deflection, @Nullable Entity entity, @Nullable Entity owner, boolean byAttack) {
		if (projectile instanceof Projectile own) {
			return own.deflect((ProjectileDeflection) deflection, entity, owner, byAttack);
		}
		turn(deflection, projectile, entity);
		if (!projectile.world.isRemote) {
			setOwner(projectile, owner);
		}
		return true;
	}

	/** A carried 1.21 deflection turning the projectile (its deflect takes 1.12.2 entities and a Random here). */
	public static void turn(@Nullable Object deflection, Entity projectile, @Nullable Entity entity) {
		if (deflection == null) {
			return;
		}
		try {
			for (Method method : deflection.getClass().getMethods()) {
				if (method.getName().equals("deflect") && method.getParameterCount() == 3) {
					method.invoke(deflection, projectile, entity, projectile.world.rand);
					return;
				}
			}
		} catch (java.lang.reflect.InvocationTargetException e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(deflection.getClass().getName() + ".deflect", e.getCause());
		} catch (ReflectiveOperationException | RuntimeException e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(deflection.getClass().getName() + ".deflect", e);
		}
	}

	public static void onDeflection(Entity projectile, @Nullable Entity entity, boolean byAttack) {
		if (projectile instanceof Projectile own) {
			own.onDeflection(entity, byAttack);
		}
	}

	/** A hit, to the projectile's hit method: the facade's, or 1.12.2's own (an arrow's onHit, a throwable's onImpact). */
	public static void onHit(Entity projectile, RayTraceResult hit) {
		if (projectile instanceof Projectile own) {
			own.onHit(hit);
		} else if (projectile instanceof EntityArrow) {
			invoke(ARROW_HIT, projectile, hit);
		} else if (projectile instanceof EntityThrowable) {
			invoke(THROWABLE_IMPACT, projectile, hit);
		} else if (projectile instanceof EntityFireball) {
			invoke(FIREBALL_IMPACT, projectile, hit);
		}
	}

	public static void onHitEntity(Entity projectile, RayTraceResult hit) {
		if (projectile instanceof Projectile own) {
			own.onHitEntity(hit);
		} else {
			onHit(projectile, hit);
		}
	}

	public static void onHitBlock(Entity projectile, RayTraceResult hit) {
		if (projectile instanceof Projectile own) {
			own.onHitBlock(hit);
		} else {
			onHit(projectile, hit);
		}
	}

	@Nullable
	public static Object hitTargetOrDeflectSelf(Entity projectile, RayTraceResult hit) {
		if (projectile instanceof Projectile own) {
			return own.hitTargetOrDeflectSelf(hit);
		}
		onHit(projectile, hit);
		return ProjectileDeflection.NONE;
	}

	private static final Method ARROW_HIT = method(EntityArrow.class, "func_184549_a");
	private static final Method THROWABLE_IMPACT = method(EntityThrowable.class, "func_70184_a");
	private static final Method FIREBALL_IMPACT = method(EntityFireball.class, "func_70227_a");

	@Nullable
	private static Method method(Class<?> owner, String srgName) {
		try {
			return ObfuscationReflectionHelper.findMethod(owner, srgName, void.class, RayTraceResult.class);
		} catch (RuntimeException | LinkageError e) {
			return null;
		}
	}

	private static void invoke(@Nullable Method method, Entity projectile, RayTraceResult hit) {
		if (method == null) {
			return;
		}
		try {
			method.invoke(projectile, hit);
		} catch (java.lang.reflect.InvocationTargetException e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(projectile.getClass().getName() + ".onHit", e.getCause());
		} catch (ReflectiveOperationException | RuntimeException e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(projectile.getClass().getName() + ".onHit", e);
		}
	}

	private static void setField(Class<?> owner, String srgName, Object target, @Nullable Object value) {
		try {
			Field field = ObfuscationReflectionHelper.findField(owner, srgName);
			field.set(target, value);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(owner.getName() + "." + srgName, e);
		}
	}
}
