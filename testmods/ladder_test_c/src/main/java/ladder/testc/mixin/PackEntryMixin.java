/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ladder.testc.ShapeChecks;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;

/** As libjf does: at the pack entry's super.mouseClicked, the last thing its mouseClicked does. */
@Mixin(TransferableSelectionList.PackEntry.class)
abstract class PackEntryMixin {
    @Inject(method = "mouseClicked", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ObjectSelectionList$Entry;mouseClicked(DDI)Z"))
    private void ladderTestC$superClick(double x, double y, int button, CallbackInfoReturnable<Boolean> cir) {
        ShapeChecks.superClicks++;
    }
}
