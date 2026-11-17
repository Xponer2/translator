/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testa.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import ladder.testa.MixinTest;

/**
 * Mixins into a method whose parameter changed below (Holder<MobEffect> on 1.21.1, MobEffect on 1.20.4): one reads
 * the parameter and answers for it, one doesn't take it at all.
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Inject(method = "hasEffect", at = @At("HEAD"), cancellable = true)
    private void ladderTest$hasEffect(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        if (MixinTest.recording) {
            MixinTest.seen.add(effect.unwrapKey().map(k -> k.location().toString()).orElse("?"));
        }
        if (effect == MobEffects.BAD_OMEN) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "hasEffect", at = @At("RETURN"))
    private void ladderTest$hasEffectCount(CallbackInfoReturnable<Boolean> cir) {
        if (MixinTest.recording) {
            MixinTest.returns++;
            // the mixin's own call of the other handler (its name and parameters as written)
            this.ladderTest$hasEffect(MobEffects.UNLUCK, new CallbackInfoReturnable<>("ladder", true));
        }
    }
}
