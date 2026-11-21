/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.misc;

import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import ladder.rung1206.client.ClientKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;

/** 1.21's tooltip, attribute and enchantment members on 1.20.6 (receiver first). */
public final class TooltipAdapters {
    private TooltipAdapters() {
    }

    /** NeoForge 21's {@code TooltipFlag.hasShiftDown()}: the client's key (none on a server) */
    public static boolean hasShiftDown(TooltipFlag flag) {
        return FMLEnvironment.dist.isClient() && ClientKeys.shift();
    }

    public static boolean hasControlDown(TooltipFlag flag) {
        return FMLEnvironment.dist.isClient() && ClientKeys.control();
    }

    public static boolean hasAltDown(TooltipFlag flag) {
        return FMLEnvironment.dist.isClient() && ClientKeys.alt();
    }

    /** 1.21's {@code TooltipContext.level()}: the level the context was made of (1.20.6's keeps it in a field), else none */
    @Nullable
    public static Level level(Item.TooltipContext context) {
        for (Class<?> c = context.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                if (Level.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        return (Level) f.get(context);
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /** 1.21's {@code Attribute.getStyle(positive)}: 1.20.6's attributes are all of the positive sentiment */
    public static ChatFormatting getStyle(Attribute attribute, boolean positive) {
        return positive ? ChatFormatting.BLUE : ChatFormatting.RED;
    }

    /** 1.21's {@code Enchantment.isSupportedItem(stack)}: the stack is in the enchantment's supported items */
    public static boolean isSupportedItem(Enchantment enchantment, ItemStack stack) {
        return stack.is(enchantment.getSupportedItems());
    }

    /** 1.21's {@code ItemStack.forEachModifier(group, action)}: the stack's modifiers (else its item's) of that slot group */
    public static void forEachModifier(ItemStack stack, EquipmentSlotGroup group, BiConsumer<Holder<Attribute>, AttributeModifier> action) {
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        if (modifiers.modifiers().isEmpty()) {
            modifiers = stack.getItem().getDefaultAttributeModifiers();
        }
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.slot() == group) {
                action.accept(entry.attribute(), entry.modifier());
            }
        }
    }
}
