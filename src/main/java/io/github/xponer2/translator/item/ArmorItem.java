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
public class ArmorItem extends ItemArmor implements TooltipContext.HoverText {
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

	/** 1.21 {@code appendHoverText}: what a mod's armor adds to its tooltip (nothing here; its override and super call). */
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.util.text.ITextComponent> tooltip,
			net.minecraft.client.util.ITooltipFlag flag) {
	}

	/** 1.12.2's tooltip: the 1.21 lines of the mod's armor (Twilight Forest's arctic, fiery, yeti armor named their powers there). */
	@Override
	@net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
	public void addInformation(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.World world, java.util.List<String> tooltip,
			net.minecraft.client.util.ITooltipFlag flag) {
		try {
			TooltipContext.addInformation(this, stack, world, tooltip, flag);
		} catch (Throwable t) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("the tooltip of " + this.getClass().getName(), t);
		}
	}

	/**
	 * NeoForge {@code getArmorTexture(stack, entity, slot, layer, innerModel)}: a mod's own texture for a layer of its
	 * armor, null for the material's (a mod's override and its super call).
	 */
	@javax.annotation.Nullable
	public net.minecraft.util.ResourceLocation getArmorTexture(ItemStack stack, net.minecraft.entity.Entity entity, EntityEquipmentSlot slot,
			ArmorMaterialData.Layer layer, boolean innerModel) {
		return null;
	}

	/** 1.12.2 asks by slot and type (null, or "overlay" for the dyed layer's overlay): the 1.21 texture of that layer. */
	@Override
	@javax.annotation.Nullable
	public String getArmorTexture(ItemStack stack, net.minecraft.entity.Entity entity, EntityEquipmentSlot slot, @javax.annotation.Nullable String type) {
		var layers = this.material.layers();
		ArmorMaterialData.Layer layer = layers.isEmpty() ? null : layers.get("overlay".equals(type) && layers.size() > 1 ? 1 : 0);
		if (layer != null) {
			try {
				var texture = this.getArmorTexture(stack, entity, slot, layer, slot == EntityEquipmentSlot.LEGS);
				if (texture != null) {
					return texture.toString();
				}
			} catch (RuntimeException | LinkageError e) {
				io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.getClass().getName() + ".getArmorTexture", e);
			}
		}
		return super.getArmorTexture(stack, entity, slot, type);
	}

	/** 1.21 {@code getName(stack)}: the name by the 1.21 rules (1.12.2's vanilla items show "Blood Bucket.name"). */
	public net.minecraft.util.text.ITextComponent getName(net.minecraft.item.ItemStack stack) {
		return new net.minecraft.util.text.TextComponentString(io.github.xponer2.translator.text.ItemNames.displayName(this, stack));
	}

	@Override
	public String getItemStackDisplayName(net.minecraft.item.ItemStack stack) {
		try {
			return this.getName(stack).getFormattedText();
		} catch (Throwable t) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("the name of " + this.getClass().getName(), t);
			return io.github.xponer2.translator.text.ItemNames.displayName(this, stack);
		}
	}
}
