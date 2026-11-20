/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import net.neoforged.neoforge.common.extensions.IItemStackExtension;

/**
 * NeoForge 21.1's {@code onEntitySwing(stack, entity, hand)}: a swing of an item knows the hand (20.4's takes no hand).
 * A converted mod item that overrides it gets this interface (an O row); 1.20.4's LivingEntity.swing asks it with the
 * swinging hand (SwingMixin). Its default is 21.1's: the hand-less one.
 */
public interface SwingApi {
    default boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        return ((Item) this).onEntitySwing(stack, entity);
    }

    /** the O row's defaults: a mod item's {@code super.onEntitySwing(stack, entity, hand)} */
    static boolean swingDefault(IItemExtension self, ItemStack stack, LivingEntity entity, InteractionHand hand) {
        return self.onEntitySwing(stack, entity);
    }

    /** {@code item.onEntitySwing(stack, entity, hand)} on any item */
    static boolean itemSwing(IItemExtension self, ItemStack stack, LivingEntity entity, InteractionHand hand) {
        return self instanceof SwingApi api ? api.onEntitySwing(stack, entity, hand) : self.onEntitySwing(stack, entity);
    }

    /** {@code stack.onEntitySwing(entity, hand)} */
    static boolean stackSwing(IItemStackExtension self, LivingEntity entity, InteractionHand hand) {
        ItemStack stack = (ItemStack) self;
        return itemSwing(stack.getItem(), stack, entity, hand);
    }
}
