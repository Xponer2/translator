/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
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

	/** By the 1.21 names (any, mainhand, offhand, hand, feet, legs, chest, head, armor, body). */
	public static final com.mojang.serialization.Codec<EquipmentSlotGroup> CODEC = com.mojang.serialization.Codec.STRING.xmap(
			name -> valueOf(name.toUpperCase(java.util.Locale.ROOT)), group -> group.name().toLowerCase(java.util.Locale.ROOT));

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
