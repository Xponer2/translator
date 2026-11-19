/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import java.util.LinkedHashSet;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ladder.adapter.component.ComponentRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.GameData;

/**
 * 1.20.5 registers its component types before its items (an item's properties name them); NeoForge 20.4 registers a
 * new registry after the game's own. The component registry goes first.
 */
@Mixin(value = GameData.class, remap = false)
abstract class GameDataMixin {
    @Inject(method = "getRegistrationOrder", at = @At("RETURN"), cancellable = true)
    private static void ladder$componentsFirst(CallbackInfoReturnable<Set<ResourceLocation>> cir) {
        Set<ResourceLocation> order = cir.getReturnValue();
        // the armour materials too: an armour item reads its material when it is made
        Set<ResourceLocation> first = new LinkedHashSet<>();
        for (ResourceLocation early : new ResourceLocation[] {ComponentRegistry.KEY.location(), ladder.adapter.armour.ArmourAdapters.KEY.location()}) {
            if (order.contains(early)) {
                first.add(early);
            }
        }
        if (!first.isEmpty()) {
            first.addAll(order);
            cir.setReturnValue(first);
        }
    }
}
