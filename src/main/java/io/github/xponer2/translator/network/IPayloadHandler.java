package io.github.xponer2.translator.network;

/**
 * Handles a received payload (NeoForge {@code IPayloadHandler}). Runs on the main thread of the receiving side.
 */
@FunctionalInterface
public interface IPayloadHandler<T extends CustomPacketPayload> {
    void handle(T payload, IPayloadContext context);
}
