package io.github.xponer2.translator.item;

import io.github.xponer2.translator.entity.Attribute;
import io.github.xponer2.translator.entity.AttributeModifier;
import io.github.xponer2.translator.entity.EquipmentSlotGroup;
import io.github.xponer2.translator.registry.Holder;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * The attribute modifiers of a stack (NeoForge {@code ItemAttributeModifierEvent}). Forge 1.12.2 has no such event:
 * items fire it from {@code Item#getAttributeModifiers(slot, stack)} through {@link #fire} (the Cucumber base items
 * do), one slot at a time.
 */
public class ItemAttributeModifierEvent extends Event {
    private final ItemStack stack;
    private final EntityEquipmentSlot slot;
    private final Multimap<String, net.minecraft.entity.ai.attributes.AttributeModifier> modifiers;

    protected ItemAttributeModifierEvent(ItemStack stack, EntityEquipmentSlot slot, Multimap<String, net.minecraft.entity.ai.attributes.AttributeModifier> modifiers) {
        this.stack = stack;
        this.slot = slot;
        this.modifiers = modifiers;
    }

    /**
     * Posts the event for the modifiers of a stack in a slot; returns them with the ones the listeners added.
     */
    public static Multimap<String, net.minecraft.entity.ai.attributes.AttributeModifier> fire(ItemStack stack, EntityEquipmentSlot slot, Multimap<String, net.minecraft.entity.ai.attributes.AttributeModifier> modifiers) {
        if (stack.isEmpty()) {
            return modifiers;
        }
        // the item's map may be immutable
        var result = HashMultimap.create(modifiers);
        MinecraftForge.EVENT_BUS.post(new ItemAttributeModifierEvent(stack, slot, result));
        return result;
    }

    public ItemStack getItemStack() {
        return this.stack;
    }

    /**
     * Adds a modifier if its slot group contains the slot the modifiers are asked for and no modifier with the same
     * id exists. Attributes 1.12.2 keeps outside the attribute map (step height, creative flight) can't be item
     * modifiers and are skipped.
     */
    public boolean addModifier(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {
        var forge = attribute.value().forge();
        if (forge == null || !slot.test(this.slot)) {
            return false;
        }
        var added = modifier.forge();
        var existing = this.modifiers.get(forge.getName());
        for (var other : existing) {
            if (other.getID().equals(added.getID())) {
                return false;
            }
        }
        existing.add(added);
        return true;
    }
}
