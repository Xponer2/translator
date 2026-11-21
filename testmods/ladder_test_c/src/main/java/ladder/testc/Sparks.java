/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testc;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * A particle type of the mod's own (1.20.5's shape: made without a deserializer, its own codec and stream codec) with
 * colour options: the server sends one to the player, the client's provider records what arrived.
 */
final class Sparks {
    static final DeferredRegister<ParticleType<?>> TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, LadderTestC.MODID);
    static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> SPARK = TYPES.register("spark", () -> new ParticleType<ColorParticleOption>(false) {
        @Override
        public MapCodec<ColorParticleOption> codec() {
            return ColorParticleOption.codec(this);
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption> streamCodec() {
            return ColorParticleOption.streamCodec(this);
        }
    });

    private Sparks() {
    }

    static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }

    /** one spark of a known colour, to the player alone */
    static void send(ServerPlayer player) {
        player.serverLevel().sendParticles(player, ColorParticleOption.create(SPARK.get(), 0xFF336699), true, player.getX(), player.getY() + 1, player.getZ(),
                1, 0, 0, 0, 0);
    }
}
