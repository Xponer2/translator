package io.github.xponer2.translator.item;

import net.minecraft.inventory.EntityEquipmentSlot;

/**
 * The armor piece kinds (1.21 {@code ArmorItem.Type}) with their 1.12.2 slot. Durability per kind is the same base
 * as 1.12.2's armor (13, 15, 16, 11 times the factor).
 */
public enum ArmorType {
    HELMET(EntityEquipmentSlot.HEAD, 11),
    CHESTPLATE(EntityEquipmentSlot.CHEST, 16),
    LEGGINGS(EntityEquipmentSlot.LEGS, 15),
    BOOTS(EntityEquipmentSlot.FEET, 13),
    BODY(EntityEquipmentSlot.CHEST, 16);

    private final EntityEquipmentSlot slot;
    private final int durability;

    ArmorType(EntityEquipmentSlot slot, int durability) {
        this.slot = slot;
        this.durability = durability;
    }

    public EntityEquipmentSlot getSlot() {
        return this.slot;
    }

    public int getDurability(int factor) {
        return this.durability * factor;
    }

    public String getName() {
        return this.name().toLowerCase(java.util.Locale.ROOT);
    }
}
