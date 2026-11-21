/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ladder.rung1206.event.DamageEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** NeoForge 21's LivingDamageEvent.Post when a hit's damage has been taken (the end of 20.6's actuallyHurt). */
@Mixin(LivingEntity.class)
abstract class DamagePostMixin {
    @Inject(method = "actuallyHurt", at = @At("RETURN"))
    private void ladder$post(DamageSource source, float amount, CallbackInfo ci) {
        DamageEvents.afterHurt((LivingEntity) (Object) this, source);
    }
}
