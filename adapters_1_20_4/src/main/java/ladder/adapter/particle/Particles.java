/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import ladder.adapter.network.Payloads;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

/**
 * 1.20.5's particle types on 1.20.4: a mod type made with {@code super(overrideLimiter)} gets a deserializer (the K row)
 * that reads its options with the type's stream codec (network) or its codec (a command's SNBT); its options are written
 * with the same stream codec (mixin.ParticlePacketMixin).
 */
public final class Particles {
    private static final SimpleCommandExceptionType BAD = new SimpleCommandExceptionType(Component.literal("Invalid particle options"));

    private Particles() {
    }

    /** the K row's deserializer for {@code ParticleType(overrideLimiter)} */
    @SuppressWarnings("deprecation")
    public static <T extends ParticleOptions> ParticleOptions.Deserializer<T> deserializer(boolean overrideLimiter) {
        return new ParticleOptions.Deserializer<T>() {
            @Override
            public T fromCommand(ParticleType<T> type, StringReader reader) throws CommandSyntaxException {
                CompoundTag tag = reader.canRead() && reader.peek() == '{' ? new TagParser(reader).readStruct() : new CompoundTag();
                return type.codec().parse(NbtOps.INSTANCE, tag).result().orElseThrow(() -> BAD.createWithContext(reader));
            }

            @Override
            @SuppressWarnings("unchecked")
            public T fromNetwork(ParticleType<T> type, FriendlyByteBuf buffer) {
                if (!(type instanceof ParticleStreamApi<?> api)) {
                    throw new IllegalStateException("a converted particle type without a stream codec: " + type);
                }
                return ((ParticleStreamApi<T>) api).streamCodec().decode(Payloads.registryBuffer(buffer));
            }
        };
    }

    /** the options of a converted mod type written with its stream codec, others as 1.20.4 writes them */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void write(ParticleOptions options, FriendlyByteBuf buffer) {
        if (options.getType() instanceof ParticleStreamApi api) {
            api.streamCodec().encode(Payloads.registryBuffer(buffer), options);
        } else {
            options.writeToNetwork(buffer);
        }
    }
}
