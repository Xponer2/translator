/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.entity;

import javax.annotation.Nullable;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1.21's projectile API on 1.20.6 (X, F, K rows): a deflection that answers whether it happened, a fireball's
 * acceleration power (1.20.6's power vector: its length, set along it), its direction constructors (1.20.6's power
 * arguments), an arrow's knockback hook (1.20.6's knockback, as its onHitEntity does it).
 */
public final class ProjectileAdapters {
    private ProjectileAdapters() {
    }

    /** 1.21's {@code deflect(...)}: 1.20.6's, which always deflects (1.21's answers true) */
    public static boolean deflect(Projectile projectile, ProjectileDeflection deflection, @Nullable Entity by, @Nullable Entity owner, boolean byAttack) {
        projectile.deflect(deflection, by, owner, byAttack);
        return true;
    }

    // ---------------------------------------------------------------- a fireball's acceleration (F row, read and write)

    /** 1.21's {@code accelerationPower}: the length of 1.20.6's power vector (0.1 when fired) */
    public static double accelerationPower(AbstractHurtingProjectile projectile) {
        return Math.sqrt(projectile.xPower * projectile.xPower + projectile.yPower * projectile.yPower + projectile.zPower * projectile.zPower);
    }

    /** a write of it: 1.20.6's power vector at that length (along its motion while it has no power yet) */
    public static void setAccelerationPower(AbstractHurtingProjectile projectile, double power) {
        double length = accelerationPower(projectile);
        Vec3 direction;
        if (length > 1.0E-9) {
            direction = new Vec3(projectile.xPower / length, projectile.yPower / length, projectile.zPower / length);
        } else {
            Vec3 motion = projectile.getDeltaMovement();
            if (motion.lengthSqr() <= 1.0E-18) {
                return;
            }
            direction = motion.normalize();
        }
        projectile.xPower = direction.x * power;
        projectile.yPower = direction.y * power;
        projectile.zPower = direction.z * power;
    }

    // ---------------------------------------------------------------- the direction constructors (K rows)

    public static double powerX(EntityType<?> type, LivingEntity owner, Vec3 direction, Level level) {
        return direction.x;
    }

    public static double powerY(EntityType<?> type, LivingEntity owner, Vec3 direction, Level level) {
        return direction.y;
    }

    public static double powerZ(EntityType<?> type, LivingEntity owner, Vec3 direction, Level level) {
        return direction.z;
    }

    public static double atPowerX(EntityType<?> type, double x, double y, double z, Vec3 direction, Level level) {
        return direction.x;
    }

    public static double atPowerY(EntityType<?> type, double x, double y, double z, Vec3 direction, Level level) {
        return direction.y;
    }

    public static double atPowerZ(EntityType<?> type, double x, double y, double z, Vec3 direction, Level level) {
        return direction.z;
    }

    // ---------------------------------------------------------------- an arrow's knockback (U and X rows)

    /** 1.21's {@code AbstractArrow.doKnockback(target, source)}: 1.20.6's knockback of the arrow (its punch level) */
    public static void doKnockback(AbstractArrow arrow, LivingEntity target, DamageSource source) {
        if (arrow.knockback > 0) {
            double resistance = Math.max(0.0, 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            Vec3 push = arrow.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(arrow.knockback * 0.6 * resistance);
            if (push.lengthSqr() > 0.0) {
                target.push(push.x, 0.1, push.z);
            }
        }
    }
}
