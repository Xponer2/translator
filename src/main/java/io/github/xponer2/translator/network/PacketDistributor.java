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

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import javax.annotation.Nullable;

/**
 * Sends payloads (NeoForge {@code PacketDistributor}). The payload types must be registered for that direction
 * ({@link PayloadRegistrar}).
 */
public final class PacketDistributor {
    private PacketDistributor() { }

    /**
     * From the client to the server.
     */
    public static void sendToServer(CustomPacketPayload payload, CustomPacketPayload... payloads) {
        for (var p : all(payload, payloads)) {
            NetworkChannels.channel(p).sendToServer(NetworkChannels.packet(p, false));
        }
    }

    public static void sendToPlayer(EntityPlayerMP player, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        if (player.connection == null) {
            return; // fake players
        }
        for (var p : all(payload, payloads)) {
            NetworkChannels.channel(p).sendTo(NetworkChannels.packet(p, true), player);
        }
    }

    public static void sendToPlayersInDimension(WorldServer level, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        for (var p : all(payload, payloads)) {
            NetworkChannels.channel(p).sendToDimension(NetworkChannels.packet(p, true), level.provider.getDimension());
        }
    }

    public static void sendToPlayersNear(WorldServer level, @Nullable EntityPlayerMP excluded, double x, double y, double z,
                                         double radius, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        for (EntityPlayer player : level.playerEntities) {
            if (player instanceof EntityPlayerMP && player != excluded && player.getDistanceSq(x, y, z) <= radius * radius) {
                sendToPlayer((EntityPlayerMP) player, payload, payloads);
            }
        }
    }

    public static void sendToAllPlayers(CustomPacketPayload payload, CustomPacketPayload... payloads) {
        for (var p : all(payload, payloads)) {
            NetworkChannels.channel(p).sendToAll(NetworkChannels.packet(p, true));
        }
    }

    /**
     * To the players that see the entity, not the entity itself.
     */
    public static void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        for (var p : all(payload, payloads)) {
            NetworkChannels.channel(p).sendToAllTracking(NetworkChannels.packet(p, true), entity);
        }
    }

    public static void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        sendToPlayersTrackingEntity(entity, payload, payloads);
        if (entity instanceof EntityPlayerMP) {
            sendToPlayer((EntityPlayerMP) entity, payload, payloads);
        }
    }

    public static void sendToPlayersTrackingChunk(WorldServer level, ChunkPos chunkPos, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        var point = new NetworkRegistry.TargetPoint(level.provider.getDimension(), chunkPos.getXStart() + 8, 0, chunkPos.getZStart() + 8, 0);
        for (var p : all(payload, payloads)) {
            NetworkChannels.channel(p).sendToAllTracking(NetworkChannels.packet(p, true), point);
        }
    }

    private static CustomPacketPayload[] all(CustomPacketPayload payload, CustomPacketPayload[] payloads) {
        var all = new CustomPacketPayload[payloads.length + 1];
        all[0] = payload;
        System.arraycopy(payloads, 0, all, 1, payloads.length);
        return all;
    }
}
