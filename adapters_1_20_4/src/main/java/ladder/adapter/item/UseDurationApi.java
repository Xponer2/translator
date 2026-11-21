/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1.21's {@code Item.getUseDuration(stack, entity)} (the user known): a converted mod item that overrides it gets this
 * interface (an O row); 1.20.4 asks {@code getUseDuration(stack)}, and the adapters' mixins ask the override with the
 * user where the game has one ({@link UseDurations}).
 */
public interface UseDurationApi {
    /** (every mod item under Item gets the interface: one that doesn't override answers as the game) */
    default int getUseDuration(ItemStack stack, LivingEntity entity) {
        return ((Item) this).getUseDuration(stack);
    }

    /** the game's own answer (a super call): 1.20.4's, without the user */
    static int getUseDuration(Item self, ItemStack stack, LivingEntity entity) {
        return self.getUseDuration(stack);
    }
}
