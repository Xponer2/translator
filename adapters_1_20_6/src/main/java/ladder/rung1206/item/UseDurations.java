/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.item;

import javax.annotation.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Use durations with the user (1.21) on 1.20.6: a converted mod item's override gets the entity using the stack. The
 * game's own call sites that know the user pass it (mixin.UseDurationMixin); elsewhere the user of the call in progress
 * on this thread, if any.
 */
public final class UseDurations {
    private static final ThreadLocal<LivingEntity> USER = new ThreadLocal<>();

    private UseDurations() {
    }

    /** 1.21's {@code item.getUseDuration(stack, entity)} on any item */
    public static int itemDuration(Item item, ItemStack stack, @Nullable LivingEntity entity) {
        if (item instanceof UseDurationApi api) {
            LivingEntity before = USER.get();
            USER.set(entity);
            try {
                return api.getUseDuration(stack, entity);
            } finally {
                USER.set(before);
            }
        }
        return item.getUseDuration(stack);
    }

    /** 1.21's {@code stack.getUseDuration(entity)} */
    public static int stackDuration(ItemStack stack, @Nullable LivingEntity entity) {
        return stack.getItem() instanceof UseDurationApi ? itemDuration(stack.getItem(), stack, entity) : stack.getUseDuration();
    }

    /** 1.20.6's {@code stack.getUseDuration()} asking the item: a converted mod item's override with this thread's user */
    public static int withCurrentUser(Item item, ItemStack stack) {
        return item instanceof UseDurationApi ? itemDuration(item, stack, USER.get()) : item.getUseDuration(stack);
    }
}
