/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * NeoForge 21.1's {@code IItemExtension.canFitInsideContainerItems(stack)} (whether this stack may go into a bundle or a
 * shulker box): a converted mod item that overrides it gets this interface (an O row); the game's checks ask it
 * (mixin.*FitMixin) and so do the X rows' calls ({@link MoreAdapters}).
 */
public interface ContainerFitApi {
    default boolean canFitInsideContainerItems(ItemStack stack) {
        return canFitInsideContainerItems((Item) this, stack);
    }

    /** 21.1's default (a super call): the item's own answer */
    static boolean canFitInsideContainerItems(Item self, ItemStack stack) {
        return self.canFitInsideContainerItems();
    }

    /** the stack's answer as 21.1 gives it: a mod item's override, else the item's */
    static boolean fits(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof ContainerFitApi own && Dispatch.declares(item.getClass(), "canFitInsideContainerItems", ItemStack.class)) {
            return own.canFitInsideContainerItems(stack);
        }
        return item.canFitInsideContainerItems();
    }
}
