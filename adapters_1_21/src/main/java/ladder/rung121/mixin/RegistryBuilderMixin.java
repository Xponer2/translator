/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import ladder.rung121.RegistryIntrusion;
import net.neoforged.neoforge.registries.RegistryBuilder;

/** A registry builder that asked for intrusive holders (NeoForge 21.1's withIntrusiveHolders) makes its registry with them. */
@Mixin(value = RegistryBuilder.class, remap = false)
abstract class RegistryBuilderMixin {
    @ModifyArg(method = "create", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/MappedRegistry;<init>(Lnet/minecraft/resources/ResourceKey;Lcom/mojang/serialization/Lifecycle;Z)V"), index = 2)
    private boolean ladder$intrusive(boolean intrusive) {
        return intrusive || RegistryIntrusion.asked(this);
    }

    @ModifyArg(method = "create", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/DefaultedMappedRegistry;<init>(Ljava/lang/String;Lnet/minecraft/resources/ResourceKey;Lcom/mojang/serialization/Lifecycle;Z)V"),
            index = 3)
    private boolean ladder$intrusiveDefaulted(boolean intrusive) {
        return intrusive || RegistryIntrusion.asked(this);
    }
}
