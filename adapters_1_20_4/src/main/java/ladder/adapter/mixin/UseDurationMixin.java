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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The entity's own uses of a stack's use duration pass the entity (1.21's getUseDuration(stack, entity)). */
@Mixin(LivingEntity.class)
abstract class UseDurationMixin {
    @Redirect(method = {"startUsingItem", "getTicksUsingItem", "shouldTriggerItemUseEffects", "onSyncedDataUpdated"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseDuration()I"))
    private int ladder$withUser(ItemStack stack) {
        return UseDurations.stackDuration(stack, (LivingEntity) (Object) this);
    }

    @Redirect(method = "isBlocking", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;getUseDuration(Lnet/minecraft/world/item/ItemStack;)I"))
    private int ladder$blockingWithUser(Item item, ItemStack stack) {
        return UseDurations.itemDuration(item, stack, (LivingEntity) (Object) this);
    }
}
