/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import java.util.UUID;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.datafixers.util.Either;

import ladder.rung1206.provided.Leashable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

/**
 * 1.20.6's mobs are 1.21's leashable entities: Mob has Leashable's members (its own code stays); its leash data is
 * read from and written to its leash fields; canBeLeashed() is 1.21's mob rule.
 */
@Mixin(Mob.class)
abstract class MobLeashableMixin implements Leashable {
    @Shadow
    @Nullable
    private Entity leashHolder;
    @Shadow
    private int delayedLeashHolderId;
    @Shadow
    @Nullable
    private Either<UUID, BlockPos> delayedLeashInfo;

    @Override
    public boolean canBeLeashed() {
        return !(this instanceof Enemy);
    }

    @Override
    @Nullable
    public Leashable.LeashData getLeashData() {
        if (this.leashHolder != null) {
            return new Leashable.LeashData(this.leashHolder);
        }
        if (this.delayedLeashInfo != null) {
            return new Leashable.LeashData(this.delayedLeashInfo);
        }
        return this.delayedLeashHolderId != 0 ? new Leashable.LeashData(this.delayedLeashHolderId) : null;
    }

    @Override
    public void setLeashData(@Nullable Leashable.LeashData data) {
        this.leashHolder = data == null ? null : data.leashHolder;
        this.delayedLeashHolderId = data == null ? 0 : data.delayedLeashHolderId;
        this.delayedLeashInfo = data == null ? null : data.delayedLeashInfo;
    }
}
