/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ladder.adapter.item.PostHurtApi;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A hit that counted (the item's hurtEnemy said so) is followed by a converted mod item's postHurtEnemy, as in 1.21. */
@Mixin(ItemStack.class)
abstract class PostHurtMixin {
    @Redirect(method = "hurtEnemy", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;hurtEnemy(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean ladder$postHurt(Item item, ItemStack stack, LivingEntity target, LivingEntity attacker, LivingEntity sameTarget, Player player) {
        boolean counted = item.hurtEnemy(stack, target, attacker);
        if (counted) {
            PostHurtApi.call(item, stack, target, attacker);
        }
        return counted;
    }
}
