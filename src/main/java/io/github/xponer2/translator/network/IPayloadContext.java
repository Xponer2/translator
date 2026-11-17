/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
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

    /** Where the payload went: to the client when the receiving player is the client's own. */
    default PacketFlow flow() {
        EntityPlayer player = this.player();
        return player == null || player.world.isRemote ? PacketFlow.CLIENTBOUND : PacketFlow.SERVERBOUND;
    }

    /** Disconnects the sender (on the server). */
    default void disconnect(net.minecraft.util.text.ITextComponent reason) {
        if (this.player() instanceof net.minecraft.entity.player.EntityPlayerMP player) {
            player.connection.disconnect(reason);
        }
    }
}
