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

/** 1.21's oxygen bonus, water movement efficiency and burning time where 1.20.6 asks respiration, depth strider and fire protection. */
@Mixin(LivingEntity.class)
abstract class LivingAttributesMixin {
    @Redirect(method = "decreaseAirSupply", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getRespiration(Lnet/minecraft/world/entity/LivingEntity;)I"))
    private int ladder$oxygen(LivingEntity entity) {
        return Math.max(EnchantmentHelper.getRespiration(entity), (int) NewAttributes.value(entity, NewAttributes.OXYGEN_BONUS));
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getDepthStrider(Lnet/minecraft/world/entity/LivingEntity;)I"))
    private int ladder$water(LivingEntity entity) {
        return Math.max(EnchantmentHelper.getDepthStrider(entity), (int) Math.round(NewAttributes.value(entity, NewAttributes.WATER_MOVEMENT_EFFICIENCY) * 3.0));
    }

    @Redirect(method = "igniteForTicks", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/ProtectionEnchantment;getFireAfterDampener(Lnet/minecraft/world/entity/LivingEntity;I)I"))
    private int ladder$burning(LivingEntity entity, int ticks) {
        return (int) Math.round(ProtectionEnchantment.getFireAfterDampener(entity, ticks) * NewAttributes.value(entity, NewAttributes.BURNING_TIME));
    }
}
