/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.armour;

import ladder.carried.net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/**
 * 1.20.5's body armour of a mob (a horse's armour, a wolf's) on 1.20.4: 1.20.5's EquipmentSlot.BODY is the chest slot
 * here (AttributeAdapters.bodySlot), where 1.20.4's horse keeps its armour.
 */
public final class BodyArmour {
    private BodyArmour() {
    }

    /**
     * {@code mob.getBodyArmorItem()}: the chest slot's (a horse wears its armour there); a llama's carpet is only in its
     * inventory (on the server)
     */
    public static ItemStack getBodyArmorItem(Mob mob) {
        ItemStack worn = mob.getItemBySlot(EquipmentSlot.CHEST);
        if (worn.isEmpty() && mob instanceof AbstractHorse horse && horse.canWearArmor()) {
            return horse.getSlot(401).get();
        }
        return worn;
    }

    /**
     * {@code mob.setBodyArmorItem(stack)}: worn, and dropped when the mob is killed (1.20.5's setItemSlotAndDropWhenKilled).
     * A horse (or llama) of 1.20.4 keeps its armour in its inventory (its protection, save and drop are made of that):
     * its armour slot there.
     */
    public static void setBodyArmorItem(Mob mob, ItemStack stack) {
        if (mob instanceof AbstractHorse horse && horse.canWearArmor()) {
            horse.getSlot(401).set(stack);
            mob.setPersistenceRequired();
            return;
        }
        mob.setItemSlot(EquipmentSlot.CHEST, stack);
        mob.setGuaranteedDrop(EquipmentSlot.CHEST);
        mob.setPersistenceRequired();
    }

    /** {@code mob.isWearingBodyArmor()} */
    public static boolean isWearingBodyArmor(Mob mob) {
        return !getBodyArmorItem(mob).isEmpty();
    }

    /** {@code mob.isBodyArmorItem(stack)}: what the mob wears as body armour (a horse its armour; none else here) */
    public static boolean isBodyArmorItem(Mob mob, ItemStack stack) {
        return mob instanceof AbstractHorse horse && horse.isArmor(stack);
    }

    // the mobs that override it in 1.20.5 (a call through their own type resolves to them)
    public static boolean isBodyArmorItem(net.minecraft.world.entity.animal.horse.Horse horse, ItemStack stack) {
        return isBodyArmorItem((Mob) horse, stack);
    }

    public static boolean isBodyArmorItem(Wolf wolf, ItemStack stack) {
        return isBodyArmorItem((Mob) wolf, stack);
    }

    public static boolean isBodyArmorItem(net.minecraft.world.entity.animal.horse.Llama llama, ItemStack stack) {
        return isBodyArmorItem((Mob) llama, stack);
    }

    /** {@code wolf.hasArmor()}: 1.20.4's wolves wear none */
    public static boolean hasArmor(Wolf wolf) {
        return !wolf.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
    }

    /** {@code entity.getArmorAndBodyArmorSlots()}: the armour slots (the body's is the chest's here) */
    public static Iterable<ItemStack> getArmorAndBodyArmorSlots(LivingEntity entity) {
        return entity.getArmorSlots();
    }

    /** {@code armorItem.getDefaultAttributeModifiers()}: the item's own modifiers, as its stacks have them */
    public static ItemAttributeModifiers getDefaultAttributeModifiers(ArmorItem item) {
        EquipmentSlot slot = item.getEquipmentSlot();
        var group = ladder.carried.net.minecraft.world.entity.EquipmentSlotGroup.bySlot(slot);
        java.util.List<ItemAttributeModifiers.Entry> entries = new java.util.ArrayList<>();
        item.getDefaultAttributeModifiers(slot).forEach((attribute, modifier) -> entries.add(
                new ItemAttributeModifiers.Entry(net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute), modifier, group)));
        return new ItemAttributeModifiers(java.util.List.copyOf(entries), true);
    }
}
