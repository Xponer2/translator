/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ladder.rung1206.portal.PortalSupport;
import net.minecraft.client.player.LocalPlayer;

/** 1.21's client portal effect for a mod's confusion portal: 1.20.6's, before it reads isInsidePortal (PortalSupport). */
@Mixin(LocalPlayer.class)
abstract class LocalPlayerPortalMixin {
    @Inject(method = "handleNetherPortalClient", at = @At("HEAD"))
    private void ladder$confusionPortal(CallbackInfo ci) {
        PortalSupport.clientTick((LocalPlayer) (Object) this);
    }
}
