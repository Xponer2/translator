/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testa.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;

import ladder.testa.ClientFrames;

/**
 * Mixins into a method whose parameters changed below (1.21's render(DeltaTracker, boolean) is render(float, long,
 * boolean) on 1.20.6 and 1.20.4, with no call bridge): one takes the parameters, one takes none.
 */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void ladderTest$frame(DeltaTracker delta, boolean renderLevel, CallbackInfo ci) {
        ClientFrames.frames++;
        float partial = delta.getGameTimeDeltaPartialTick(true);
        if (partial < 0.0F || partial > 1.0F) {
            ClientFrames.badPartial++;
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void ladderTest$frameEnd(CallbackInfo ci) {
        ClientFrames.ends++;
    }
}
