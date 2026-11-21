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

import ladder.rung121.attachment.AttachmentSync;
import net.neoforged.neoforge.attachment.AttachmentType;

/** the type an attachment builder builds keeps the sync handler the builder was given (NeoForge 21.1's sync) */
@Mixin(value = AttachmentType.Builder.class, remap = false)
abstract class AttachmentTypeBuilderMixin<T> {
    @Inject(method = "build", at = @At("RETURN"))
    private void ladder$synced(CallbackInfoReturnable<AttachmentType<T>> cir) {
        AttachmentSync.built((AttachmentType.Builder<?>) (Object) this, cir.getReturnValue());
    }
}
