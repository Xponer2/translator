/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testd.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ladder.testd.LadderTestD;

/** The full Explosion constructor of 1.21 takes the sound as a Holder of SoundEvent; the 1.20.4 one takes the SoundEvent. */
@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Explosion$BlockInteraction;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/Holder;)V",
            at = @At("RETURN"))
    private void ladderTestD$created(Level level, Entity source, DamageSource damage, ExplosionDamageCalculator calculator, double x, double y,
            double z, float radius, boolean fire, Explosion.BlockInteraction interaction, ParticleOptions small, ParticleOptions large,
            Holder<SoundEvent> sound, CallbackInfo ci) {
        LadderTestD.record("explosion", radius + " " + interaction + " fire=" + fire + " at " + (int) x + "," + (int) y + "," + (int) z
                + " sound=" + sound.value().getLocation());
    }
}
