/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ladder.adapter.particle.Particles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;

/** The particle packet writes a converted mod's options with its type's stream codec (1.20.5's), as its deserializer reads them. */
@Mixin(ClientboundLevelParticlesPacket.class)
abstract class ParticlePacketMixin {
    @Redirect(method = "write", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/particles/ParticleOptions;writeToNetwork(Lnet/minecraft/network/FriendlyByteBuf;)V"))
    private void ladder$write(ParticleOptions options, FriendlyByteBuf buffer) {
        Particles.write(options, buffer);
    }
}
