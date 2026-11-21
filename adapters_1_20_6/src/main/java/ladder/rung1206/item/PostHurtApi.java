/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.21's {@code Item.postHurtEnemy(stack, target, attacker)}: after a hit that counted (hurtEnemy said so), the item's
 * own wear. A converted mod item that overrides it gets this interface (an O row); 1.20.6's ItemStack.hurtEnemy asks it
 * (mixin.PostHurtMixin). The game's own items wear in their hurtEnemy below, so the default (a super call) does nothing.
 */
public interface PostHurtApi {
    default void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
    }

    /** the game's own answer (a super call): nothing, the lower hurtEnemy has worn the item already */
    static void postHurtEnemy(net.minecraft.world.item.Item self, ItemStack stack, LivingEntity target, LivingEntity attacker) {
    }

    /** {@code item.postHurtEnemy(stack, target, attacker)} on any item */
    static void call(net.minecraft.world.item.Item item, ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (item instanceof PostHurtApi own) {
            own.postHurtEnemy(stack, target, attacker);
        }
    }

    /** 1.21's {@code stack.postHurtEnemy(target, player)} */
    static void stackCall(ItemStack stack, LivingEntity target, Player player) {
        call(stack.getItem(), stack, target, player);
    }
}
