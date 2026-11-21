/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ladder.rung1206.misc.MoreAdapters;
import net.minecraft.world.food.FoodProperties;

/** The food a builder builds keeps 1.21's "converts to" item the builder was given (MoreAdapters.usingConvertsTo). */
@Mixin(FoodProperties.Builder.class)
abstract class FoodBuilderMixin {
    @Inject(method = "build", at = @At("RETURN"))
    private void ladder$converts(CallbackInfoReturnable<FoodProperties> cir) {
        MoreAdapters.built((FoodProperties.Builder) (Object) this, cir.getReturnValue());
    }
}
