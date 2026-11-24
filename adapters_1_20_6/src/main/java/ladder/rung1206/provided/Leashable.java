/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Either;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.item.Items;

/**
 * 1.21's {@code Leashable} (a P row): what can be put on a lead, its leash data and leash physics. 1.20.6's mobs are
 * leashable (mixin.MobLeashableMixin): Mob keeps its own leash members and code, its leash data read from and written to
 * its fields. A mod's own leashable entity gets 1.21's code here (its tick calls tickLeash).
 */
public interface Leashable {
    String LEASH_TAG = "leash";
    double LEASH_TOO_FAR_DIST = 10.0;
    double LEASH_ELASTIC_DIST = 6.0;

    @Nullable
    LeashData getLeashData();

    void setLeashData(@Nullable LeashData data);

    default boolean isLeashed() {
        return this.getLeashData() != null && this.getLeashData().leashHolder != null;
    }

    default boolean mayBeLeashed() {
        return this.getLeashData() != null;
    }

    default boolean canHaveALeashAttachedToIt() {
        return this.canBeLeashed() && !this.isLeashed();
    }

    /** whether a lead may be put on it (1.21: the entity's own rule; a mob: not an enemy) */
    default boolean canBeLeashed() {
        return true;
    }

    default void setDelayedLeashHolderId(int id) {
        this.setLeashData(new LeashData(id));
        dropLeash((Entity & Leashable) this, false, false);
    }

    @Nullable
    default LeashData readLeashData(CompoundTag tag) {
        if (tag.contains(LEASH_TAG, 10)) {
            return new LeashData(Either.left(tag.getCompound(LEASH_TAG).getUUID("UUID")));
        }
        if (tag.contains(LEASH_TAG, 11)) {
            Either<UUID, BlockPos> info = NbtUtils.readBlockPos(tag, LEASH_TAG).<Either<UUID, BlockPos>>map(Either::right).orElse(null);
            if (info != null) {
                return new LeashData(info);
            }
        }
        return null;
    }

    default void writeLeashData(CompoundTag tag, @Nullable LeashData data) {
        if (data == null) {
            return;
        }
        Either<UUID, BlockPos> info = data.delayedLeashInfo;
        if (data.leashHolder instanceof LeashFenceKnotEntity knot) {
            info = Either.right(knot.getPos());
        } else if (data.leashHolder != null) {
            info = Either.left(data.leashHolder.getUUID());
        }
        if (info != null) {
            tag.put(LEASH_TAG, info.map(uuid -> {
                CompoundTag id = new CompoundTag();
                id.putUUID("UUID", uuid);
                return id;
            }, NbtUtils::writeBlockPos));
        }
    }

    private static <E extends Entity & Leashable> void restoreLeashFromSave(E entity, LeashData data) {
        if (data.delayedLeashInfo != null && entity.level() instanceof ServerLevel level) {
            Optional<UUID> uuid = data.delayedLeashInfo.left();
            Optional<BlockPos> pos = data.delayedLeashInfo.right();
            if (uuid.isPresent()) {
                Entity holder = level.getEntity(uuid.get());
                if (holder != null) {
                    setLeashedTo(entity, holder, true);
                    return;
                }
            } else if (pos.isPresent()) {
                setLeashedTo(entity, LeashFenceKnotEntity.getOrCreateKnot(level, pos.get()), true);
                return;
            }
            if (entity.tickCount > 100) {
                entity.spawnAtLocation(Items.LEAD);
                entity.setLeashData(null);
            }
        }
    }

    default void dropLeash(boolean broadcast, boolean dropItem) {
        dropLeash((Entity & Leashable) this, broadcast, dropItem);
    }

    private static <E extends Entity & Leashable> void dropLeash(E entity, boolean broadcast, boolean dropItem) {
        LeashData data = entity.getLeashData();
        if (data != null && data.leashHolder != null) {
            entity.setLeashData(null);
            if (!entity.level().isClientSide && dropItem) {
                entity.spawnAtLocation(Items.LEAD);
            }
            if (broadcast && entity.level() instanceof ServerLevel level) {
                level.getChunkSource().broadcast(entity, new ClientboundSetEntityLinkPacket(entity, null));
            }
        }
    }

    static <E extends Entity & Leashable> void tickLeash(E entity) {
        LeashData data = entity.getLeashData();
        if (data != null && data.delayedLeashInfo != null) {
            restoreLeashFromSave(entity, data);
        }
        if (data != null && data.leashHolder != null) {
            if (!entity.isAlive() || !data.leashHolder.isAlive()) {
                dropLeash(entity, true, true);
            }
            Entity holder = entity.getLeashHolder();
            if (holder != null && holder.level() == entity.level()) {
                float distance = entity.distanceTo(holder);
                if (!entity.handleLeashAtDistance(holder, distance)) {
                    return;
                }
                if ((double) distance > LEASH_TOO_FAR_DIST) {
                    entity.leashTooFarBehaviour();
                } else if ((double) distance > LEASH_ELASTIC_DIST) {
                    entity.elasticRangeLeashBehaviour(holder, distance);
                    entity.checkSlowFallDistance();
                } else {
                    entity.closeRangeLeashBehaviour(holder);
                }
            }
        }
    }

    default boolean handleLeashAtDistance(Entity holder, float distance) {
        return true;
    }

    default void leashTooFarBehaviour() {
        this.dropLeash(true, true);
    }

    default void closeRangeLeashBehaviour(Entity holder) {
    }

    default void elasticRangeLeashBehaviour(Entity holder, float distance) {
        legacyElasticRangeLeashBehaviour((Entity & Leashable) this, holder, distance);
    }

    private static <E extends Entity & Leashable> void legacyElasticRangeLeashBehaviour(E entity, Entity holder, float distance) {
        double dx = (holder.getX() - entity.getX()) / (double) distance;
        double dy = (holder.getY() - entity.getY()) / (double) distance;
        double dz = (holder.getZ() - entity.getZ()) / (double) distance;
        entity.setDeltaMovement(entity.getDeltaMovement().add(Math.copySign(dx * dx * 0.4, dx), Math.copySign(dy * dy * 0.4, dy),
                Math.copySign(dz * dz * 0.4, dz)));
    }

    default void setLeashedTo(Entity holder, boolean broadcast) {
        setLeashedTo((Entity & Leashable) this, holder, broadcast);
    }

    private static <E extends Entity & Leashable> void setLeashedTo(E entity, Entity holder, boolean broadcast) {
        LeashData data = entity.getLeashData();
        if (data == null) {
            data = new LeashData(holder);
            entity.setLeashData(data);
        } else {
            data.setLeashHolder(holder);
        }
        if (broadcast && entity.level() instanceof ServerLevel level) {
            level.getChunkSource().broadcast(entity, new ClientboundSetEntityLinkPacket(entity, holder));
        }
        if (entity.isPassenger()) {
            entity.stopRiding();
        }
    }

    @Nullable
    default Entity getLeashHolder() {
        return getLeashHolder((Entity & Leashable) this);
    }

    @Nullable
    private static <E extends Entity & Leashable> Entity getLeashHolder(E entity) {
        LeashData data = entity.getLeashData();
        if (data == null) {
            return null;
        }
        if (data.delayedLeashHolderId != 0 && entity.level().isClientSide) {
            Entity holder = entity.level().getEntity(data.delayedLeashHolderId);
            if (holder != null) {
                data.setLeashHolder(holder);
            }
        }
        return data.leashHolder;
    }

    /** 1.21's {@code Leashable.LeashData} (a P row): the holder, or what a save or the client names of it */
    final class LeashData {
        public int delayedLeashHolderId;
        @Nullable
        public Entity leashHolder;
        @Nullable
        public Either<UUID, BlockPos> delayedLeashInfo;

        public LeashData(Either<UUID, BlockPos> delayedLeashInfo) {
            this.delayedLeashInfo = delayedLeashInfo;
        }

        public LeashData(Entity leashHolder) {
            this.leashHolder = leashHolder;
        }

        public LeashData(int delayedLeashHolderId) {
            this.delayedLeashHolderId = delayedLeashHolderId;
        }

        public void setLeashHolder(Entity leashHolder) {
            this.leashHolder = leashHolder;
            this.delayedLeashInfo = null;
            this.delayedLeashHolderId = 0;
        }
    }
}
