/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.entity;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1.21's projectile API on 1.20.6 (X, F, K rows): a deflection that answers whether it happened, a fireball's
 * acceleration power (1.20.6's power vector: its length, set along it), its direction constructors (1.20.6's power
 * arguments),
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

    /** what a mod wrote to a projectile's acceleration power (HurtingProjectilePowerMixin) */
    public interface PowerHolder {
        boolean ladder$powerSet();

        double ladder$power();

        /** the power vector's length when it was written (1.20.6 changing the vector makes the write stale) */
        double ladder$powerLength();

        boolean ladder$powerPending();

        void ladder$setPower(double power, double length, boolean pending);
    }

    private static double length(AbstractHurtingProjectile projectile) {
        return Math.sqrt(projectile.xPower * projectile.xPower + projectile.yPower * projectile.yPower + projectile.zPower * projectile.zPower);
    }

    /** whether the mod's written value still stands (1.20.6 hasn't changed the vector since: a deflection, say) */
    private static boolean written(AbstractHurtingProjectile projectile, PowerHolder holder) {
        return holder.ladder$powerSet() && (holder.ladder$powerPending() || Math.abs(length(projectile) - holder.ladder$powerLength()) < 1.0E-9);
    }

    /** 1.21's {@code accelerationPower}: what a mod wrote (signed), else the length of 1.20.6's power vector (0.1 when fired) */
    public static double accelerationPower(AbstractHurtingProjectile projectile) {
        PowerHolder holder = (PowerHolder) projectile;
        return written(projectile, holder) ? holder.ladder$power() : length(projectile);
    }

    /**
     * a write of it: 1.20.6's power vector at that signed length, along the vector (a negative value reverses it) or,
     * with no power yet, along the motion; with no motion either, on the projectile's next tick that has one
     */
    public static void setAccelerationPower(AbstractHurtingProjectile projectile, double power) {
        PowerHolder holder = (PowerHolder) projectile;
        double length = length(projectile);
        Vec3 direction;
        if (length > 1.0E-9) {
            direction = new Vec3(projectile.xPower / length, projectile.yPower / length, projectile.zPower / length);
            if (written(projectile, holder) && !holder.ladder$powerPending() && holder.ladder$power() < 0.0) {
                // the vector points against the direction of the earlier negative write
                direction = direction.scale(-1.0);
            }
        } else {
            Vec3 motion = projectile.getDeltaMovement();
            if (motion.lengthSqr() <= 1.0E-18) {
                holder.ladder$setPower(power, 0.0, true);
                return;
            }
            direction = motion.normalize();
        }
        projectile.xPower = direction.x * power;
        projectile.yPower = direction.y * power;
        projectile.zPower = direction.z * power;
        holder.ladder$setPower(power, length(projectile), false);
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
}
