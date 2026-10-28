package io.github.xponer2.translator.network;

import net.minecraft.entity.player.EntityPlayer;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * The context of a received payload (NeoForge {@code IPayloadContext}).
 */
public interface IPayloadContext {
    /**
     * The sender on the server, the local player on the client.
     */
    EntityPlayer player();

    /**
     * Runs a task on the main thread of the receiving side (right away when called from it, as the handlers are).
     */
    CompletableFuture<Void> enqueueWork(Runnable task);

    <T> CompletableFuture<T> enqueueWork(Supplier<T> task);

    /**
     * Sends a payload back: to the server on the client, to the sender on the server.
     */
    void reply(CustomPacketPayload payload);
}
