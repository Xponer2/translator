package io.github.xponer2.translator.entity;

import net.minecraft.inventory.EntityEquipmentSlot;

/**
 * 1.21 group of equipment slots (where an attribute modifier applies).
 */
public enum EquipmentSlotGroup {
	ANY,
	MAINHAND,
	OFFHAND,
	HAND,
	FEET,
	LEGS,
	CHEST,
	HEAD,
	ARMOR,
	BODY;

	public boolean test(EntityEquipmentSlot slot) {
		return switch (this) {
			case ANY -> true;
			case MAINHAND -> slot == EntityEquipmentSlot.MAINHAND;
			case OFFHAND -> slot == EntityEquipmentSlot.OFFHAND;
			case HAND -> slot.getSlotType() == EntityEquipmentSlot.Type.HAND;
			case FEET -> slot == EntityEquipmentSlot.FEET;
			case LEGS -> slot == EntityEquipmentSlot.LEGS;
			case CHEST, BODY -> slot == EntityEquipmentSlot.CHEST;
			case HEAD -> slot == EntityEquipmentSlot.HEAD;
			case ARMOR -> slot.getSlotType() == EntityEquipmentSlot.Type.ARMOR;
		};
	}

	public static EquipmentSlotGroup bySlot(EntityEquipmentSlot slot) {
		return switch (slot) {
			case MAINHAND -> MAINHAND;
			case OFFHAND -> OFFHAND;
			case FEET -> FEET;
			case LEGS -> LEGS;
			case CHEST -> CHEST;
			case HEAD -> HEAD;
		};
	}
}
