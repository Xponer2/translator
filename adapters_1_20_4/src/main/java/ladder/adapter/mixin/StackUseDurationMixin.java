/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ladder.adapter.item.UseDurations;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 1.20.4's {@code stack.getUseDuration()}: a converted mod item's override, with the user of the call in progress. */
@Mixin(ItemStack.class)
abstract class StackUseDurationMixin {
    @Redirect(method = "getUseDuration()I", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;getUseDuration(Lnet/minecraft/world/item/ItemStack;)I"))
    private int ladder$override(Item item, ItemStack stack) {
        return UseDurations.withCurrentUser(item, stack);
    }
}
