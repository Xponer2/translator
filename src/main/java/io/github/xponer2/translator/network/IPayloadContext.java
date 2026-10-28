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
