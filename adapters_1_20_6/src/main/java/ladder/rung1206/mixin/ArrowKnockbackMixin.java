/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1.21's {@code AbstractArrow.doKnockback(target, source)} on 1.20.6 (added to the class), and 1.20.6's onHitEntity
 * knocking back through it: a mod arrow's override decides its knockback, as on 1.21. The base method is 1.20.6's own
 * knockback (its punch level), so a vanilla arrow knocks back as before.
 */
@Mixin(AbstractArrow.class)
abstract class ArrowKnockbackMixin extends Projectile {
    @Shadow
    private int knockback;

    ArrowKnockbackMixin(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    protected void doKnockback(LivingEntity target, DamageSource source) {
        if (this.knockback > 0) {
            double resistance = Math.max(0.0, 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            Vec3 push = this.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(this.knockback * 0.6 * resistance);
            if (push.lengthSqr() > 0.0) {
                target.push(push.x, 0.1, push.z);
            }
        }
    }

    /** 1.20.6's inline knockback test: the hook knocks back (a mod's override, or the base one), the inline code is skipped */
    @ModifyExpressionValue(method = "onHitEntity", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;knockback:I", ordinal = 0))
    private int ladder$knockbackHook(int knockback, @Local LivingEntity target, @Local DamageSource source) {
        this.doKnockback(target, source);
        return 0;
    }
}
