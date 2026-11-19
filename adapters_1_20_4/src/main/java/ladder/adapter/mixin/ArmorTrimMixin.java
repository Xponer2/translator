/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ladder.adapter.item.TrimAdapters;
import net.minecraft.world.item.armortrim.ArmorTrim;

/** A shown trim isn't equal to a hidden one (1.20.5 compares show_in_tooltip too): TrimAdapters.Hidden. */
@Mixin(ArmorTrim.class)
public abstract class ArmorTrimMixin {
    @Inject(method = "equals", at = @At("HEAD"), cancellable = true)
    private void ladder$shownToo(Object other, CallbackInfoReturnable<Boolean> cir) {
        if (other instanceof ArmorTrim trim && TrimAdapters.shown((ArmorTrim) (Object) this) != TrimAdapters.shown(trim)) {
            cir.setReturnValue(false);
        }
    }
}
