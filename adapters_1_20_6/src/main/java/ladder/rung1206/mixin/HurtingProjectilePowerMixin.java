/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ladder.rung1206.entity.ProjectileAdapters;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;

/**
 * 1.21's {@code AbstractHurtingProjectile.accelerationPower} as a mod wrote it (ProjectileAdapters): the signed value, the
 * direction 1.20.6's power vector has for it, and whether it waits for the projectile to move (applied on its next tick).
 */
@Mixin(AbstractHurtingProjectile.class)
abstract class HurtingProjectilePowerMixin implements ProjectileAdapters.PowerHolder {
    @Unique
    private boolean ladder$powerSet;
    @Unique
    private boolean ladder$powerPending;
    @Unique
    private double ladder$power;
    @Unique
    private double ladder$powerLength;

    @Override
    public boolean ladder$powerSet() {
        return this.ladder$powerSet;
    }

    @Override
    public double ladder$power() {
        return this.ladder$power;
    }

    @Override
    public double ladder$powerLength() {
        return this.ladder$powerLength;
    }

    @Override
    public boolean ladder$powerPending() {
        return this.ladder$powerPending;
    }

    @Override
    public void ladder$setPower(double power, double length, boolean pending) {
        this.ladder$powerSet = true;
        this.ladder$power = power;
        this.ladder$powerLength = length;
        this.ladder$powerPending = pending;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void ladder$pendingPower(CallbackInfo ci) {
        if (this.ladder$powerPending) {
            ProjectileAdapters.setAccelerationPower((AbstractHurtingProjectile) (Object) this, this.ladder$power);
        }
    }
}
