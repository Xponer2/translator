/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ladder.testc.ShapeChecks;
import net.minecraft.client.gui.screens.options.OptionsScreen;

/** As MidnightLib and Puzzle do: at the end of the options screen's repositionElements. */
@Mixin(OptionsScreen.class)
abstract class OptionsScreenMixin {
    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void ladderTestC$repositioned(CallbackInfo ci) {
        ShapeChecks.repositioned++;
    }
}
