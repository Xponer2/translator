/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.component;

import ladder.carried.net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

/**
 * 1.20.5's items give their attribute modifiers as a component of their own (Item.Properties.attributes); 1.20.4 asks
 * the item for each slot (getAttributeModifiers, then ItemAttributeModifierEvent). The event adds the item's own
 * component's modifiers for the slot, unless the stack has modifiers of its own (they replace the item's, as in both).
 */
public final class ItemAttributes {
    private ItemAttributes() {
    }

    /** 1.20.5's {@code Item.Properties.attributes(modifiers)}: the item's attribute_modifiers component */
    public static Item.Properties attributes(Item.Properties properties, ItemAttributeModifiers modifiers) {
        return ItemDefaults.component(properties, DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ItemAttributes::onModifiers);
    }

    static void onModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("AttributeModifiers", Tag.TAG_LIST)) {
            return;
        }
        if (ItemDefaults.of(stack.getItem()).get(DataComponents.ATTRIBUTE_MODIFIERS) instanceof ItemAttributeModifiers own) {
            for (ItemAttributeModifiers.Entry entry : own.modifiers()) {
                // 1.20.4's own item may give the same modifier already (a sword's, made from the same numbers)
                if (entry.slot().test(event.getSlotType()) && event.getModifiers().get(entry.attribute().value()).stream()
                        .noneMatch(m -> m.getId().equals(entry.modifier().getId()))) {
                    event.addModifier(entry.attribute().value(), entry.modifier());
                }
            }
        }
    }
}
