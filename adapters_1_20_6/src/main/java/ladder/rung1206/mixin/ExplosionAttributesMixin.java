/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ladder.rung1206.attribute.NewAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;

/** 1.21's explosion knockback resistance on 1.20.6's explosion knockback (after blast protection). */
@Mixin(net.minecraft.world.level.Explosion.class)
abstract class ExplosionAttributesMixin {
    @Redirect(method = "explode", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/ProtectionEnchantment;getExplosionKnockbackAfterDampener(Lnet/minecraft/world/entity/LivingEntity;D)D"))
    private double ladder$knockback(LivingEntity entity, double knockback) {
        return ProtectionEnchantment.getExplosionKnockbackAfterDampener(entity, knockback)
                * (1.0 - NewAttributes.value(entity, NewAttributes.EXPLOSION_KNOCKBACK_RESISTANCE));
    }
}
