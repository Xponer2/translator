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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.datafixers.util.Either;

import ladder.rung1206.provided.Leashable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

/**
 * 1.20.6's mobs are 1.21's leashable entities: Mob has Leashable's members (its own code stays); canBeLeashed() is
 * 1.21's mob rule. Its leash data is one live object per mob, kept in step with 1.20.6's leash fields both ways: a change
 * of the fields (the mob's own code) shows in the object, and a change made through the object (a mod's
 * {@code getLeashData().setLeashHolder(..)}, or its fields) goes to the mob before the mob's leash code next runs.
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

    /** the mob's live leash data (null while it has no leash), and the field values both sides last agreed on */
    @Unique
    @Nullable
    private Leashable.LeashData ladder$leashData;
    @Unique
    @Nullable
    private Entity ladder$syncedHolder;
    @Unique
    private int ladder$syncedId;
    @Unique
    @Nullable
    private Either<UUID, BlockPos> ladder$syncedInfo;

    @Override
    public boolean canBeLeashed() {
        return !(this instanceof Enemy);
    }

    @Override
    @Nullable
    public Leashable.LeashData getLeashData() {
        this.ladder$sync();
        return this.ladder$leashData;
    }

    @Override
    public void setLeashData(@Nullable Leashable.LeashData data) {
        this.ladder$leashData = data;
        this.leashHolder = data == null ? null : data.leashHolder;
        this.delayedLeashHolderId = data == null ? 0 : data.delayedLeashHolderId;
        this.delayedLeashInfo = data == null ? null : data.delayedLeashInfo;
        this.ladder$agreed();
    }

    /** the mob's leash code reads its fields: a change made through the leash data first goes to them */
    @Inject(method = {"tickLeash()V", "dropLeash(ZZ)V", "setLeashedTo(Lnet/minecraft/world/entity/Entity;Z)V", "setDelayedLeashHolderId(I)V",
            "addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V"}, at = @At("HEAD"))
    private void ladder$syncBefore(CallbackInfo ci) {
        this.ladder$sync();
    }

    @Inject(method = {"getLeashHolder()Lnet/minecraft/world/entity/Entity;", "isLeashed()Z", "mayBeLeashed()Z"}, at = @At("HEAD"))
    private void ladder$syncBeforeAnswer(CallbackInfoReturnable<?> cir) {
        this.ladder$sync();
    }

    @Unique
    private void ladder$sync() {
        Leashable.LeashData data = this.ladder$leashData;
        boolean mobChanged = this.leashHolder != this.ladder$syncedHolder || this.delayedLeashHolderId != this.ladder$syncedId
                || this.delayedLeashInfo != this.ladder$syncedInfo;
        if (data != null && !mobChanged && (data.leashHolder != this.ladder$syncedHolder || data.delayedLeashHolderId != this.ladder$syncedId
                || data.delayedLeashInfo != this.ladder$syncedInfo)) {
            // changed through the leash data: the mob takes it
            this.leashHolder = data.leashHolder;
            this.delayedLeashHolderId = data.delayedLeashHolderId;
            this.delayedLeashInfo = data.delayedLeashInfo;
        }
        if (this.leashHolder == null && this.delayedLeashHolderId == 0 && this.delayedLeashInfo == null) {
            this.ladder$leashData = null;
        } else if (data == null) {
            this.ladder$leashData = this.leashHolder != null ? new Leashable.LeashData(this.leashHolder)
                    : this.delayedLeashInfo != null ? new Leashable.LeashData(this.delayedLeashInfo) : new Leashable.LeashData(this.delayedLeashHolderId);
            this.ladder$leashData.leashHolder = this.leashHolder;
            this.ladder$leashData.delayedLeashHolderId = this.delayedLeashHolderId;
            this.ladder$leashData.delayedLeashInfo = this.delayedLeashInfo;
        } else {
            data.leashHolder = this.leashHolder;
            data.delayedLeashHolderId = this.delayedLeashHolderId;
            data.delayedLeashInfo = this.delayedLeashInfo;
        }
        this.ladder$agreed();
    }

    @Unique
    private void ladder$agreed() {
        this.ladder$syncedHolder = this.leashHolder;
        this.ladder$syncedId = this.delayedLeashHolderId;
        this.ladder$syncedInfo = this.delayedLeashInfo;
    }
}
