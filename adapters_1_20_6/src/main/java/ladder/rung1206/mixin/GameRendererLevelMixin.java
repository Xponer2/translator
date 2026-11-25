/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import ladder.rung1206.provided.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;

/**
 * 1.21's {@code GameRenderer.renderLevel(DeltaTracker)} on 1.20.6 (the method is the class's own, as 1.21 has it): 1.20.6's
 * renderLevel(partial tick, frame start) of the tracker's partial tick, the frame's start render was given. The frame's
 * render calls it, so a mod's wrap of the whole level's render (sodium-extra's panini projection) wraps it; the mods'
 * points inside the level's render stay 1.20.6's renderLevel (the D and A rows).
 */
@Mixin(value = GameRenderer.class, priority = 500)
abstract class GameRendererLevelMixin {
    /** the frame's start render was given (1.21's tracker has none: 1.20.6's renderLevel takes it) */
    @Unique
    private long ladder$frameNanos;

    @Shadow
    public abstract void renderLevel(float partialTick, long nanos);

    /** 1.21's renderLevel(DeltaTracker): 1.20.6's level render of the tracker's partial tick */
    public void renderLevel(DeltaTracker tracker) {
        this.renderLevel(tracker.getGameTimeDeltaPartialTick(true), this.ladder$frameNanos);
    }

    /** the frame's level render goes through 1.21's renderLevel(DeltaTracker), with 1.20.6's partial tick and start */
    @WrapOperation(method = "render(FJZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJ)V"))
    private void ladder$renderLevelOfTracker(GameRenderer self, float partialTick, long nanos, Operation<Void> original) {
        long outer = this.ladder$frameNanos;
        this.ladder$frameNanos = nanos;
        try {
            this.renderLevel(ladder.rung1206.bridge.Conversions.deltaTracker(partialTick));
        } finally {
            this.ladder$frameNanos = outer;
        }
    }
}
