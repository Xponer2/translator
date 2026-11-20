/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.mixin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ladder.rung121.SmallAdapters;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.packs.resources.ResourceManager;

/** the registries a loot reload reads its tables with, for NeoForge 21.1's {@code LootTableLoadEvent.getRegistries()} */
@Mixin(ReloadableServerRegistries.class)
public abstract class ReloadableServerRegistriesMixin {
    @Inject(method = "reload", at = @At("HEAD"))
    private static void ladder$lootRegistries(LayeredRegistryAccess<RegistryLayer> registries, ResourceManager resources, Executor executor,
            CallbackInfoReturnable<CompletableFuture<LayeredRegistryAccess<RegistryLayer>>> cir) {
        SmallAdapters.lootRegistries = registries.getAccessForLoading(RegistryLayer.RELOADABLE);
    }
}
