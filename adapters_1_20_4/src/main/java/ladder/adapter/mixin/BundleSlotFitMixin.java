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
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A game container check asks the stack (NeoForge 21.1's stack.canFitInsideContainerItems(), which a mod item may answer per stack) where 1.20.4 asks the item. */
@Mixin(net.minecraft.world.item.BundleItem.class)
abstract class BundleSlotFitMixin {
    @Redirect(method = "overrideStackedOnOther", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;canFitInsideContainerItems()Z"))
    private boolean ladder$fits(Item item, ItemStack bundle, Slot slot, ClickAction action, Player player) {
        return ContainerFitApi.fits(slot.getItem());
    }
}
