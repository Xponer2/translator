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
package io.github.xponer2.translator.runtime.mc;

import com.google.common.collect.Multimap;

import io.github.xponer2.translator.item.ItemAttributeModifiers;
import io.github.xponer2.translator.item.ItemProperties;
import io.github.xponer2.translator.item.Tier;
import io.github.xponer2.translator.item.ToolItems;

import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

/** Facade of the 1.21 {@code PickaxeItem}: the 1.12.2 ItemPickaxe of the tier's tool material, with properties. */
public class PickaxeItem extends net.minecraft.item.ItemPickaxe {
	private final Tier tier;
	private final ItemProperties properties;

	public PickaxeItem(Tier tier, ItemProperties properties) {
		super(tier.toolMaterial());
		this.tier = tier;
		this.properties = properties;
		properties.applyTo(this);
	}

	/** 1.21 {@code createAttributes(tier, attackDamage, attackSpeed)}: the main hand modifiers of the tool. */
	public static ItemAttributeModifiers createAttributes(Tier tier, float attackDamage, float attackSpeed) {
		return new ItemAttributeModifiers(attackDamage + tier.getAttackDamageBonus(), attackSpeed);
	}

	public static ItemAttributeModifiers createAttributes(Tier tier, int attackDamage, float attackSpeed) {
		return createAttributes(tier, (float) attackDamage, attackSpeed);
	}

	public Tier getTier() {
		return this.tier;
	}

	@Override
	public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
		ItemAttributeModifiers modifiers = this.properties.attributeModifiers();
		if (modifiers == null) {
			return super.getItemAttributeModifiers(slot);
		}
		return ToolItems.mainHandModifiers(slot, ATTACK_DAMAGE_MODIFIER, ATTACK_SPEED_MODIFIER, modifiers.attackDamage(), modifiers.attackSpeed());
	}

	@Override
	public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
		return ToolItems.isRepairable(this.tier, repair);
	}
}
