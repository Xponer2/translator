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

/** 1.21's sweeping damage ratio and submerged mining speed where 1.20.6 asks sweeping edge and aqua affinity. */
@Mixin(net.minecraft.world.entity.player.Player.class)
abstract class PlayerAttributesMixin {
    @Redirect(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getSweepingDamageRatio(Lnet/minecraft/world/entity/LivingEntity;)F"))
    private float ladder$sweeping(LivingEntity entity) {
        return Math.max(EnchantmentHelper.getSweepingDamageRatio(entity), (float) NewAttributes.value(entity, NewAttributes.SWEEPING_DAMAGE_RATIO));
    }

    @Redirect(method = "getDigSpeed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;hasAquaAffinity(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean ladder$submerged(LivingEntity entity) {
        return EnchantmentHelper.hasAquaAffinity(entity) || NewAttributes.value(entity, NewAttributes.SUBMERGED_MINING_SPEED) >= 1.0;
    }
}
