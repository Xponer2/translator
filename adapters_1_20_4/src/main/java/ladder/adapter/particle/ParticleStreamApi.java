/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.particle;

import ladder.carried.net.minecraft.network.RegistryFriendlyByteBuf;
import ladder.carried.net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

/**
 * 1.20.5's particle type: a stream codec ({@code streamCodec()}) where 1.20.4's has a deserializer and the options write
 * themselves. A converted mod type gets this interface (an O row); its options go over the network with it
 * ({@link Particles}).
 */
public interface ParticleStreamApi<T extends ParticleOptions> {
    StreamCodec<RegistryFriendlyByteBuf, T> streamCodec();

    /** a super call of 1.20.5's abstract streamCodec(): none can be made */
    static StreamCodec<RegistryFriendlyByteBuf, ?> streamCodec(ParticleType<?> self) {
        throw new AbstractMethodError("ParticleType.streamCodec");
    }
}
