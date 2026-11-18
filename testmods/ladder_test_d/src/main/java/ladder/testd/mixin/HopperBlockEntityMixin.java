/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testd.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ladder.testd.LadderTestD;

/**
 * 1.21's ejectItems(Level, BlockPos, HopperBlockEntity): the 1.20.4 one takes the block state too. Inside it, 1.21
 * calls getAttachedContainer(Level, BlockPos, HopperBlockEntity), 1.20.4 getAttachedContainer(Level, BlockPos, BlockState).
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    @Inject(method = "ejectItems", at = @At("HEAD"))
    private static void ladderTestD$eject(Level level, BlockPos pos, HopperBlockEntity hopper, CallbackInfoReturnable<Boolean> cir) {
        LadderTestD.record("eject", pos.toShortString() + " slots=" + hopper.getContainerSize());
    }

    @WrapOperation(method = "ejectItems", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;getAttachedContainer(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/HopperBlockEntity;)Lnet/minecraft/world/Container;"))
    private static Container ladderTestD$attached(Level level, BlockPos pos, HopperBlockEntity hopper, Operation<Container> original) {
        Container container = original.call(level, pos, hopper);
        LadderTestD.record("attached", container == null ? "null" : container.getContainerSize() + " slots");
        return container;
    }
}
