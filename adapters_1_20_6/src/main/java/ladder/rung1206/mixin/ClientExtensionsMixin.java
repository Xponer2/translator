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

import ladder.rung1206.provided.RegisterClientExtensionsEvent;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.ClientHooks;

/** NeoForge 21's RegisterClientExtensionsEvent, posted on the mod buses when the client's hooks start. */
@Mixin(value = ClientHooks.class, remap = false)
abstract class ClientExtensionsMixin {
    @Inject(method = "initClientHooks", at = @At("HEAD"))
    private static void ladder$clientExtensions(CallbackInfo ci) {
        ModLoader.postEvent(new RegisterClientExtensionsEvent());
    }
}
