/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ladder.adapter.item.ContainerFitApi;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The carried bundle contents (1.20.5's, carried from the vanilla code, without NeoForge's patch) ask the stack, as
 * NeoForge 21.1's tryInsert does ({@code stack.canFitInsideContainerItems()}, which a mod item may answer per stack).
 */
@Mixin(targets = "ladder.carried.net.minecraft.world.item.component.BundleContents$Mutable", remap = false)
abstract class CarriedBundleFitMixin {
    @Redirect(method = "tryInsert", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;canFitInsideContainerItems()Z"))
    private boolean ladder$fits(Item item, ItemStack stack) {
        return ContainerFitApi.fits(stack);
    }
}
