/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ladder.rung121.PoiExtensions;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;

/** A point of interest type is also the states the mods added to it (NeoForge 21.1's ExtendPoiTypesEvent). */
@Mixin(PoiType.class)
abstract class PoiTypeMixin {
    @Inject(method = "is", at = @At("RETURN"), cancellable = true)
    private void ladder$added(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            java.util.Set<BlockState> added = PoiExtensions.ADDED.get((PoiType) (Object) this);
            if (added != null && added.contains(state)) {
                cir.setReturnValue(true);
            }
        }
    }
}
