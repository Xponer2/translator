/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.handler.codec.EncoderException;
import ladder.adapter.bridge.Conversions;
import ladder.carried.net.minecraft.network.RegistryFriendlyByteBuf;
import ladder.carried.net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The stream codecs of the registered 1.20.5 payloads, by phase (play, configuration) and payload id: 1.20.4 reads a
 * payload with a reader of its phase and writes it with the payload's own {@code write}, both made from the codec here.
 * A play payload's codec works on a {@code RegistryFriendlyByteBuf} (the buffer with the current registries), a
 * configuration or common one on the plain buffer, as in 1.21. One id may have a codec in each phase: a payload sent
 * through the adapters (PacketDistributor, a reply) then goes in a {@link Phased} wrapper that writes it with its
 * phase's codec.
 */
public final class Payloads {
    record Entry(StreamCodec<?, ?> codec, boolean registries) {
        @SuppressWarnings("unchecked")
        void encode(FriendlyByteBuf buffer, Object payload) {
            ((StreamCodec<Object, Object>) this.codec).encode(this.registries ? registryBuffer(buffer) : buffer, payload);
        }
    }

    private static final Map<ResourceLocation, Entry> PLAY = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Entry> CONFIGURATION = new ConcurrentHashMap<>();

    private Payloads() {
    }

    /** A payload's codec in a phase; the same id registered again in that phase (the other side) keeps the first. */
    static void register(ResourceLocation id, StreamCodec<?, ?> codec, boolean registries, boolean play) {
        (play ? PLAY : CONFIGURATION).putIfAbsent(id, new Entry(codec, registries));
    }

    /** 1.20.4's reader of a payload: its codec's decode, on the buffer it was registered for. */
    @SuppressWarnings("unchecked")
    static <T> FriendlyByteBuf.Reader<T> reader(StreamCodec<?, T> codec, boolean registries) {
        StreamCodec<Object, T> c = (StreamCodec<Object, T>) codec;
        return buffer -> c.decode(registries ? registryBuffer(buffer) : buffer);
    }

    /** A payload's own {@code write} (the phase unknown): its play codec, else its configuration one. */
    static void write(CustomPacketPayload payload, FriendlyByteBuf buffer) {
        ResourceLocation id = payload.id();
        Entry entry = PLAY.get(id);
        if (entry == null) {
            entry = CONFIGURATION.get(id);
        }
        if (entry == null) {
            throw new EncoderException("Payload " + id + " is not registered (no stream codec for it)");
        }
        entry.encode(buffer, payload);
    }

    /**
     * The payload to send in a phase: itself, unless its id has another codec in the other phase; then a wrapper that
     * writes it with this phase's codec (a handler gets the payload itself again, see {@link #unwrap}).
     */
    static CustomPacketPayload inPhase(CustomPacketPayload payload, boolean play) {
        if (payload instanceof Phased) {
            return payload;
        }
        ResourceLocation id = payload.id();
        Entry mine = (play ? PLAY : CONFIGURATION).get(id), other = (play ? CONFIGURATION : PLAY).get(id);
        return mine != null && other != null && mine.codec() != other.codec() ? new Phased(payload, mine) : payload;
    }

    static CustomPacketPayload[] inPhase(CustomPacketPayload[] payloads, boolean play) {
        CustomPacketPayload[] out = new CustomPacketPayload[payloads.length];
        for (int i = 0; i < payloads.length; i++) {
            out[i] = inPhase(payloads[i], play);
        }
        return out;
    }

    /** Single player passes payloads as objects (not encoded): a handler gets the wrapped payload back. */
    static Object unwrap(Object payload) {
        return payload instanceof Phased phased ? phased.payload() : payload;
    }

    /** A payload sent in one phase, written with that phase's codec. */
    record Phased(CustomPacketPayload payload, Entry entry) implements CustomPacketPayload {
        @Override
        public void write(FriendlyByteBuf buffer) {
            this.entry.encode(buffer, this.payload);
        }

        @Override
        public ResourceLocation id() {
            return this.payload.id();
        }
    }

    /** The buffer with the current registries (it shares the buffer's bytes and indices). */
    static RegistryFriendlyByteBuf registryBuffer(FriendlyByteBuf buffer) {
        return buffer instanceof RegistryFriendlyByteBuf registry ? registry : new RegistryFriendlyByteBuf(buffer, Conversions.registries());
    }
}
