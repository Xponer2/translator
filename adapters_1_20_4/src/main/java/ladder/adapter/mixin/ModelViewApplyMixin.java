/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;

import ladder.adapter.render.ModelViewStack;

/** The model view the game applies is also what the mods changed on 1.20.5's matrix stack (ModelViewStack). */
@Mixin(value = RenderSystem.class, remap = false)
abstract class ModelViewApplyMixin {
    @Inject(method = "applyModelViewMatrix", at = @At("HEAD"))
    private static void ladder$modsStack(CallbackInfo ci) {
        ModelViewStack.beforeApply();
    }
}
