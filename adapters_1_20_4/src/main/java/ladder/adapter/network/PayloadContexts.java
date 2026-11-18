/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.network;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * NeoForge 20.5's {@code IPayloadContext} members on 20.4's (the same interface name, other members; receiver first):
 * 20.4 has them behind its work, reply and packet handlers.
 */
public final class PayloadContexts {
    private PayloadContexts() {
    }

    public static CompletableFuture<Void> enqueueWork(IPayloadContext context, Runnable task) {
        return context.workHandler().submitAsync(task);
    }

    public static <T> CompletableFuture<T> enqueueWork(IPayloadContext context, Supplier<T> task) {
        return context.workHandler().submitAsync(task);
    }

    /** 1.21: the player of the connection (the server's player on the server, the local player on the client). */
    public static Player player(IPayloadContext context) {
        return context.player().orElseThrow(() -> new UnsupportedOperationException("No player in this payload context (" + context.protocol() + ")"));
    }

    /** The answer goes in the phase of the payload being handled. */
    public static void reply(IPayloadContext context, CustomPacketPayload payload) {
        context.replyHandler().send(Payloads.inPhase(payload, context.protocol() != ConnectionProtocol.CONFIGURATION));
    }

    public static void disconnect(IPayloadContext context, Component reason) {
        context.replyHandler().disconnect(reason);
    }

    public static void handle(IPayloadContext context, Packet<?> packet) {
        context.packetHandler().handle(packet);
    }

    public static void handle(IPayloadContext context, CustomPacketPayload payload) {
        context.packetHandler().handle(payload);
    }

    /** 1.21: the connection; in 20.4 it is the "packet_handler" of the channel's pipeline. */
    public static Connection connection(IPayloadContext context) {
        return (Connection) context.channelHandlerContext().pipeline().get("packet_handler");
    }
}
