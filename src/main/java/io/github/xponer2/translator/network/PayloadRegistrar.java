/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package io.github.xponer2.translator.network;

/**
 * Registers the payloads of a mod (NeoForge {@code PayloadRegistrar}), from {@link RegisterPayloadHandlersEvent}.
 * The configuration phase of 1.21 doesn't exist on 1.12.2: the {@code common*} methods register play payloads.
 * Versions aren't checked (1.12.2 compares the mod versions when a client connects).
 */
public class PayloadRegistrar {
    private final String version;

    public PayloadRegistrar(String version) {
        this.version = version;
    }

    public String version() {
        return this.version;
    }

    public PayloadRegistrar versioned(String version) {
        return new PayloadRegistrar(version);
    }

    public PayloadRegistrar optional() {
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playToClient(CustomPacketPayload.Type<T> type,
                                                                         StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                                         IPayloadHandler<T> handler) {
        NetworkChannels.register(type, codec, handler, null);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playToServer(CustomPacketPayload.Type<T> type,
                                                                         StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                                         IPayloadHandler<T> handler) {
        NetworkChannels.register(type, codec, null, handler);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playBidirectional(CustomPacketPayload.Type<T> type,
                                                                              StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                                              IPayloadHandler<T> handler) {
        NetworkChannels.register(type, codec, handler, handler);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar commonToClient(CustomPacketPayload.Type<T> type,
                                                                           StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                                           IPayloadHandler<T> handler) {
        return this.playToClient(type, codec, handler);
    }

    public <T extends CustomPacketPayload> PayloadRegistrar commonToServer(CustomPacketPayload.Type<T> type,
                                                                           StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                                           IPayloadHandler<T> handler) {
        return this.playToServer(type, codec, handler);
    }

    public <T extends CustomPacketPayload> PayloadRegistrar commonBidirectional(CustomPacketPayload.Type<T> type,
                                                                                StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                                                IPayloadHandler<T> handler) {
        return this.playBidirectional(type, codec, handler);
    }
}
