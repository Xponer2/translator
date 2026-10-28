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
package io.github.xponer2.translator.item;

import java.util.UUID;

import com.google.common.collect.Multimap;

import io.github.xponer2.translator.registry.Holder;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

/**
 * 1.21 {@code ArmorItem}: armor of a 1.21 armor material. The 1.12.2 material gives armor, toughness, enchantability,
 * sound and texture; the knockback resistance is added as a modifier, the repair item is the material's ingredient.
 */
public class ArmorItem extends ItemArmor {
	// one id per armor slot (feet, legs, chest, head), so the pieces add up
	private static final UUID[] KNOCKBACK_MODIFIERS = {
			UUID.fromString("3b7f1a4e-2c5d-4e8f-9a1b-6c7d8e9f0a11"), UUID.fromString("3b7f1a4e-2c5d-4e8f-9a1b-6c7d8e9f0a12"),
			UUID.fromString("3b7f1a4e-2c5d-4e8f-9a1b-6c7d8e9f0a13"), UUID.fromString("3b7f1a4e-2c5d-4e8f-9a1b-6c7d8e9f0a14") };
	private final ArmorMaterialData material;
	private final ArmorType type;

	public ArmorItem(Holder<ArmorMaterialData> material, ArmorType type, ItemProperties properties) {
		super(material.value().toolMaterial(), 0, type.getSlot());
		this.material = material.value();
		this.type = type;
		properties.applyTo(this);
	}

	public ArmorMaterialData getMaterial() {
		return this.material;
	}

	public ArmorType getType() {
		return this.type;
	}

	@Override
	public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
		var modifiers = super.getItemAttributeModifiers(slot);
		if (slot == this.armorType && this.material.knockbackResistance() > 0) {
			modifiers.put(SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getName(),
					new AttributeModifier(KNOCKBACK_MODIFIERS[slot.getIndex()], "Armor knockback resistance", this.material.knockbackResistance(), 0));
		}
		return modifiers;
	}

	@Override
	public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
		return this.material.repairIngredient().get().apply(repair);
	}
}
