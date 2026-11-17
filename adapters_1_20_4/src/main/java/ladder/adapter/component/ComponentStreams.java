/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.component;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import ladder.adapter.bridge.Conversions;
import ladder.carried.net.minecraft.network.codec.StreamCodec;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.RegistryOps;

/** 1.20.5's {@code ByteBufCodecs.fromCodecWithRegistries(codec)}: a value as its codec's NBT, written to the buffer. */
final class ComponentStreams {
    private ComponentStreams() {
    }

    static <T> StreamCodec<ByteBuf, T> fromCodec(Codec<T> codec) {
        return new StreamCodec<>() {
            @Override
            public T decode(ByteBuf buffer) {
                Tag tag = buffer(buffer).readNbt(NbtAccounter.create(2097152L));
                return codec.parse(RegistryOps.create(NbtOps.INSTANCE, Conversions.registries()), tag).resultOrPartial(e -> {
                    throw new IllegalStateException("Failed to decode: " + e);
                }).orElseThrow();
            }

            @Override
            public void encode(ByteBuf buffer, T value) {
                Tag tag = codec.encodeStart(RegistryOps.create(NbtOps.INSTANCE, Conversions.registries()), value).resultOrPartial(e -> {
                    throw new IllegalStateException("Failed to encode: " + e);
                }).orElseThrow();
                buffer(buffer).writeNbt(tag);
            }
        };
    }

    private static FriendlyByteBuf buffer(ByteBuf buffer) {
        return buffer instanceof FriendlyByteBuf friendly ? friendly : new FriendlyByteBuf(buffer);
    }
}
