/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * NeoForge 20.5's {@code PacketDistributor} (static sends) on 20.4's (targets with an argument): the same receivers,
 * the payloads sent together as one.
 */
public final class Distributors {
    private Distributors() {
    }

    /** The payloads to send, in the play phase (a payload with another codec for configuration is written with its play one). */
    private static CustomPacketPayload[] all(CustomPacketPayload payload, CustomPacketPayload[] payloads) {
        CustomPacketPayload[] all = new CustomPacketPayload[payloads.length + 1];
        all[0] = payload;
        System.arraycopy(payloads, 0, all, 1, payloads.length);
        return Payloads.inPhase(all, true);
    }

    public static void sendToServer(CustomPacketPayload payload, CustomPacketPayload... payloads) {
        PacketDistributor.SERVER.noArg().send(all(payload, payloads));
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        PacketDistributor.PLAYER.with(player).send(all(payload, payloads));
    }

    public static void sendToPlayersInDimension(ServerLevel level, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        PacketDistributor.DIMENSION.with(level.dimension()).send(all(payload, payloads));
    }

    public static void sendToPlayersNear(ServerLevel level, ServerPlayer excluded, double x, double y, double z, double radius,
            CustomPacketPayload payload, CustomPacketPayload... payloads) {
        PacketDistributor.NEAR.with(new PacketDistributor.TargetPoint(excluded, x, y, z, radius, level.dimension())).send(all(payload, payloads));
    }

    public static void sendToAllPlayers(CustomPacketPayload payload, CustomPacketPayload... payloads) {
        PacketDistributor.ALL.noArg().send(all(payload, payloads));
    }

    public static void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        PacketDistributor.TRACKING_ENTITY.with(entity).send(all(payload, payloads));
    }

    public static void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(entity).send(all(payload, payloads));
    }

    public static void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos pos, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        PacketDistributor.TRACKING_CHUNK.with(level.getChunk(pos.x, pos.z)).send(all(payload, payloads));
    }
}
