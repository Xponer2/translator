/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1.21's {@code calculateBoundingBox(pos, direction)} of hanging entities on 1.20.6 (the method is added to the class):
 * an entity that has it is placed and sized by it, as 1.21's recalculateBoundingBox does; 1.20.6's own keep theirs.
 */
@Mixin(HangingEntity.class)
abstract class HangingBoxMixin extends Entity {
    @Shadow
    protected BlockPos pos;
    @Shadow
    protected Direction direction;

    protected HangingBoxMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** null: 1.20.6's own box (a converted mod's hanging entity returns its own) */
    protected AABB calculateBoundingBox(BlockPos pos, Direction direction) {
        return null;
    }

    @Inject(method = "recalculateBoundingBox", at = @At("HEAD"), cancellable = true)
    private void ladder$ownBox(CallbackInfo ci) {
        if (this.direction == null) {
            return;
        }
        AABB box = this.calculateBoundingBox(this.pos, this.direction);
        if (box != null) {
            Vec3 center = box.getCenter();
            this.setPosRaw(center.x, center.y, center.z);
            this.setBoundingBox(box);
            ci.cancel();
        }
    }
}
