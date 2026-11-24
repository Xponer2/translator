/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.portal;

import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.ITeleporter;

/**
 * 1.21's dimension transitions on 1.20.6 (the X and F rows of DimensionTransition, Entity.changeDimension): 1.21 moves
 * an entity by a transition that names its level and what runs after; 1.20.6 by a level and a teleporter that gives the
 * PortalInfo. A transition is a Transition (a PortalInfo); the teleporter hands it to 1.20.6's changeDimension, and the
 * transition's post step runs on the moved entity.
 */
public final class PortalAdapters {
    private PortalAdapters() {
    }

    /** 1.21's {@code DimensionTransition.DO_NOTHING} */
    public static final ladder.rung1206.provided.DimensionTransition$PostDimensionTransition DO_NOTHING = entity -> {
    };
    /** 1.21's {@code PLAY_PORTAL_SOUND}: a player hears the portal's travel sound */
    public static final ladder.rung1206.provided.DimensionTransition$PostDimensionTransition PLAY_PORTAL_SOUND = entity -> {
        if (entity instanceof ServerPlayer player) {
            player.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
        }
    };
    /** 1.21's {@code PLACE_PORTAL_TICKET}: the chunks around the arrival stay loaded a while */
    public static final ladder.rung1206.provided.DimensionTransition$PostDimensionTransition PLACE_PORTAL_TICKET = entity -> {
        if (entity.level() instanceof ServerLevel level) {
            BlockPos pos = BlockPos.containing(entity.position());
            level.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(pos), 3, pos);
        }
    };

    public static ladder.rung1206.provided.DimensionTransition$PostDimensionTransition doNothing() {
        return DO_NOTHING;
    }

    public static ladder.rung1206.provided.DimensionTransition$PostDimensionTransition playPortalSound() {
        return PLAY_PORTAL_SOUND;
    }

    public static ladder.rung1206.provided.DimensionTransition$PostDimensionTransition placePortalTicket() {
        return PLACE_PORTAL_TICKET;
    }

    // ---------------------------------------------------------------- the transition (1.21's record)

    public static PortalInfo transition(ServerLevel level, Vec3 pos, Vec3 speed, float yRot, float xRot, boolean missingRespawnBlock,
            ladder.rung1206.provided.DimensionTransition$PostDimensionTransition post) {
        return new Transition(level, pos, speed, yRot, xRot, missingRespawnBlock, post);
    }

    public static PortalInfo transition(ServerLevel level, Vec3 pos, Vec3 speed, float yRot, float xRot,
            ladder.rung1206.provided.DimensionTransition$PostDimensionTransition post) {
        return new Transition(level, pos, speed, yRot, xRot, false, post);
    }

    /** to the level's spawn (1.21 adjusts it to the entity: 1.20.6 has no such search, the spawn's block centre) */
    public static PortalInfo transition(ServerLevel level, Entity entity,
            ladder.rung1206.provided.DimensionTransition$PostDimensionTransition post) {
        return new Transition(level, Vec3.atBottomCenterOf(level.getSharedSpawnPos()), Vec3.ZERO, 0.0F, 0.0F, false, post);
    }

    public static PortalInfo missingRespawnBlock(ServerLevel level, Entity entity,
            ladder.rung1206.provided.DimensionTransition$PostDimensionTransition post) {
        return new Transition(level, Vec3.atBottomCenterOf(level.getSharedSpawnPos()), Vec3.ZERO, 0.0F, 0.0F, true, post);
    }

    /**
     * the transition 1.20.6's changeDimension(level) stands for, as a handler of 1.21's changeDimension(transition)
     * sees it (an A row's plan): to that level, from where the entity is (1.20.6 finds the place later, by its teleporter)
     */
    public static PortalInfo toLevel(Entity entity, ServerLevel level) {
        return new Transition(level, entity.position(), entity.getDeltaMovement(), entity.getYRot(), entity.getXRot(), false, DO_NOTHING);
    }

    public static PortalInfo toLevel(net.minecraft.server.level.ServerPlayer player, ServerLevel level) {
        return toLevel((Entity) player, level);
    }

    @Nullable
    public static ServerLevel newLevel(PortalInfo info) {
        return info instanceof Transition t ? t.newLevel : null;
    }

    public static boolean missingRespawnBlock(PortalInfo info) {
        return info instanceof Transition t && t.missingRespawnBlock;
    }

    public static ladder.rung1206.provided.DimensionTransition$PostDimensionTransition postDimensionTransition(PortalInfo info) {
        return info instanceof Transition t ? t.post : DO_NOTHING;
    }

    // ---------------------------------------------------------------- the move (1.21's Entity.changeDimension)

    /**
     * 1.21's {@code entity.changeDimension(transition)}: within its level a teleport, else 1.20.6's changeDimension with
     * a teleporter that gives the transition; then the transition's post step on the entity that arrived (null: none)
     */
    @Nullable
    public static Entity changeDimension(Entity entity, PortalInfo info) {
        ServerLevel level = newLevel(info);
        if (level == null) {
            level = entity.level() instanceof ServerLevel own ? own : null;
            if (level == null) {
                return null;
            }
        }
        Entity moved;
        if (entity.level() == level) {
            entity.teleportTo(level, info.pos.x, info.pos.y, info.pos.z, Set.of(), info.yRot, info.xRot);
            entity.setDeltaMovement(info.speed);
            moved = entity;
        } else {
            moved = entity.changeDimension(level, new ITeleporter() {
                @Override
                public PortalInfo getPortalInfo(Entity e, ServerLevel destination, Function<ServerLevel, PortalInfo> defaultInfo) {
                    return info;
                }

                @Override
                public boolean isVanilla() {
                    return false;
                }

                @Override
                public boolean playTeleportSound(ServerPlayer player, ServerLevel from, ServerLevel to) {
                    // 1.21 plays it as a post step (PLAY_PORTAL_SOUND) when the transition asks
                    return false;
                }
            });
        }
        if (moved != null) {
            postDimensionTransition(info).onTransition(moved);
        }
        return moved;
    }
}
