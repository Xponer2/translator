/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.misc;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/** Small 1.21 members on 1.20.6 (the X rows): a tame animal's teleport to its owner, an explosion's block triggering, a holder's key. */
public final class TailAdapters {
    private TailAdapters() {
    }

    /**
     * 1.21's {@code TamableAnimal.tryToTeleportToOwner()}: near its owner, on a walkable free place (1.20.6's
     * FollowOwnerGoal does the same from the goal; a flying animal may land on leaves)
     */
    public static void tryToTeleportToOwner(TamableAnimal animal) {
        LivingEntity owner = animal.getOwner();
        if (owner == null) {
            return;
        }
        BlockPos around = owner.blockPosition();
        for (int i = 0; i < 10; i++) {
            int x = animal.getRandom().nextIntBetweenInclusive(-3, 3);
            int z = animal.getRandom().nextIntBetweenInclusive(-3, 3);
            if (Math.abs(x) >= 2 || Math.abs(z) >= 2) {
                int y = animal.getRandom().nextIntBetweenInclusive(-1, 1);
                if (maybeTeleportTo(animal, around.getX() + x, around.getY() + y, around.getZ() + z)) {
                    return;
                }
            }
        }
    }

    private static boolean maybeTeleportTo(TamableAnimal animal, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (WalkNodeEvaluator.getPathTypeStatic(animal, pos) != PathType.WALKABLE) {
            return false;
        }
        if (!(animal instanceof FlyingAnimal) && animal.level().getBlockState(pos.below()).getBlock() instanceof LeavesBlock) {
            return false;
        }
        if (!animal.level().noCollision(animal, animal.getBoundingBox().move(pos.subtract(animal.blockPosition())))) {
            return false;
        }
        animal.moveTo(x + 0.5, y, z + 0.5, animal.getYRot(), animal.getXRot());
        animal.getNavigation().stop();
        return true;
    }

    /** 1.21's {@code Explosion.canTriggerBlocks()}: a block-triggering explosion on the server (a breeze's wind charge only with mob griefing) */
    public static boolean canTriggerBlocks(Explosion explosion) {
        Level level = explosion.level;
        if (explosion.getBlockInteraction() != Explosion.BlockInteraction.TRIGGER_BLOCK || level.isClientSide()) {
            return false;
        }
        Entity source = explosion.getDirectSourceEntity();
        return source == null || source.getType() != EntityType.BREEZE_WIND_CHARGE || level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    /** NeoForge 21's {@code Holder.Reference.getKey()}: its key, null while unbound */
    @Nullable
    public static <T> ResourceKey<T> getKey(Holder.Reference<T> holder) {
        return holder.unwrapKey().orElse(null);
    }

    /** 1.21's {@code GenerationChunkHolder.getChunkIfPresentUnchecked(status)}: 1.20.6's holder's done chunk of that status, else null */
    @Nullable
    public static net.minecraft.world.level.chunk.ChunkAccess getChunkIfPresentUnchecked(net.minecraft.server.level.ChunkHolder holder,
            net.minecraft.world.level.chunk.status.ChunkStatus status) {
        java.util.concurrent.CompletableFuture<net.minecraft.server.level.ChunkResult<net.minecraft.world.level.chunk.ChunkAccess>> future =
                holder.getFutureIfPresentUnchecked(status);
        return future == null ? null : future.getNow(net.minecraft.server.level.ChunkHolder.UNLOADED_CHUNK).orElse(null);
    }

    /** 1.21's {@code RegistryData.requiredNonEmpty()}: 1.20.6's registry data has no such check (none required) */
    public static boolean requiredNonEmpty(net.minecraft.resources.RegistryDataLoader.RegistryData<?> data) {
        return false;
    }
}
