/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.provided.network;

import java.util.function.Function;

import ladder.adapter.network.PayloadBridge;
import ladder.carried.net.minecraft.network.RegistryFriendlyByteBuf;
import ladder.carried.net.minecraft.network.codec.StreamCodec;
import ladder.carried.net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type;
import ladder.carried.net.neoforged.neoforge.network.registration.HandlerThread;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

/**
 * NeoForge 20.5's {@code PayloadRegistrar} on 20.4's {@code IPayloadRegistrar}: a payload is registered with its type,
 * its stream codec and its handler. 20.4 makes one registrar per namespace (the payload id's own) with the version and
 * the optional flag; its handlers run where 1.21's do (the main thread unless {@code executesOn(NETWORK)}).
 */
public class PayloadRegistrar {
    private final String version;
    private final Function<String, IPayloadRegistrar> lower;
    private final boolean optional;
    private final HandlerThread thread;

    public PayloadRegistrar(String version) {
        this(version, PayloadBridge.current(), false, HandlerThread.MAIN);
    }

    PayloadRegistrar(String version, Function<String, IPayloadRegistrar> lower, boolean optional, HandlerThread thread) {
        this.version = version;
        this.lower = lower;
        this.optional = optional;
        this.thread = thread;
    }

    private IPayloadRegistrar lower(CustomPacketPayload$Type<?> type) {
        IPayloadRegistrar registrar = this.lower.apply(type.id().getNamespace()).versioned(this.version);
        return this.optional ? registrar.optional() : registrar;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playToClient(CustomPacketPayload$Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            IPayloadHandler<T> handler) {
        PayloadBridge.play(this.lower(type), type, codec, handler, null, this.thread);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playToServer(CustomPacketPayload$Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            IPayloadHandler<T> handler) {
        PayloadBridge.play(this.lower(type), type, codec, null, handler, this.thread);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playBidirectional(CustomPacketPayload$Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            IPayloadHandler<T> handler) {
        PayloadBridge.play(this.lower(type), type, codec, handler, handler, this.thread);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar configurationToClient(CustomPacketPayload$Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec,
            IPayloadHandler<T> handler) {
        PayloadBridge.configuration(this.lower(type), type, codec, handler, null, this.thread);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar configurationToServer(CustomPacketPayload$Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec,
            IPayloadHandler<T> handler) {
        PayloadBridge.configuration(this.lower(type), type, codec, null, handler, this.thread);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar configurationBidirectional(CustomPacketPayload$Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec,
            IPayloadHandler<T> handler) {
        PayloadBridge.configuration(this.lower(type), type, codec, handler, handler, this.thread);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar commonToClient(CustomPacketPayload$Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec,
            IPayloadHandler<T> handler) {
        PayloadBridge.common(this.lower(type), type, codec, handler, null, this.thread);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar commonToServer(CustomPacketPayload$Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec,
            IPayloadHandler<T> handler) {
        PayloadBridge.common(this.lower(type), type, codec, null, handler, this.thread);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar commonBidirectional(CustomPacketPayload$Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec,
            IPayloadHandler<T> handler) {
        PayloadBridge.common(this.lower(type), type, codec, handler, handler, this.thread);
        return this;
    }

    /** A copy whose handlers run on that thread (1.21: a new registrar, this one unchanged). */
    public PayloadRegistrar executesOn(HandlerThread thread) {
        return new PayloadRegistrar(this.version, this.lower, this.optional, thread);
    }

    public PayloadRegistrar versioned(String version) {
        return new PayloadRegistrar(version, this.lower, this.optional, this.thread);
    }

    public PayloadRegistrar optional() {
        return new PayloadRegistrar(this.version, this.lower, true, this.thread);
    }
}
