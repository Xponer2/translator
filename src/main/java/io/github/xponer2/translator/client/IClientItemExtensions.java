/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.client;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The parts of NeoForge's client item extensions 1.12.2 can do: the first person hand transform (GL transforms,
 * applied before the item in that hand is drawn).
 */
@SideOnly(Side.CLIENT)
public interface IClientItemExtensions {
	/** Where a font is asked for (1.21 {@code IClientItemExtensions.FontContext}). */
	enum FontContext {
		ITEM_COUNT,
		TOOLTIP,
		SELECTED_ITEM_NAME
	}

	/** 1.21 {@code getFont(stack, context)}: null, the game's font. */
	@javax.annotation.Nullable
	default net.minecraft.client.gui.FontRenderer getFont(ItemStack stack, FontContext context) {
		return null;
	}

	/** 1.21 {@code getHumanoidArmorModel(entity, stack, slot, original)}: the original model by default. */
	default net.minecraft.client.model.ModelBiped getHumanoidArmorModel(net.minecraft.entity.EntityLivingBase entity, ItemStack stack,
			net.minecraft.inventory.EntityEquipmentSlot slot, net.minecraft.client.model.ModelBiped original) {
		return original;
	}

	/** 1.21 {@code getCustomRenderer()}: the renderer of the item's built-in model (the shared 1.12.2 one by default). */
	default net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer getCustomRenderer() {
		return net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer.instance;
	}

	/**
	 * NeoForge {@code getGenericArmorModel(entity, stack, slot, original)}: the item's humanoid armor model with the
	 * original's pose, or the original. The carried armor layer asks for it every frame for every armor piece (Twilight
	 * Forest's armor: the call failed and the piece wasn't drawn, ~3500 times a test).
	 */
	default net.minecraft.client.model.ModelBase getGenericArmorModel(net.minecraft.entity.EntityLivingBase entity, ItemStack stack,
			net.minecraft.inventory.EntityEquipmentSlot slot, net.minecraft.client.model.ModelBiped original) {
		net.minecraft.client.model.ModelBiped replacement = this.getHumanoidArmorModel(entity, stack, slot, original);
		if (replacement == null || replacement == original) {
			return original;
		}
		replacement.setModelAttributes(original);
		net.minecraft.client.model.ModelBase.copyModelAngles(original.bipedHead, replacement.bipedHead);
		net.minecraft.client.model.ModelBase.copyModelAngles(original.bipedHeadwear, replacement.bipedHeadwear);
		net.minecraft.client.model.ModelBase.copyModelAngles(original.bipedBody, replacement.bipedBody);
		net.minecraft.client.model.ModelBase.copyModelAngles(original.bipedRightArm, replacement.bipedRightArm);
		net.minecraft.client.model.ModelBase.copyModelAngles(original.bipedLeftArm, replacement.bipedLeftArm);
		net.minecraft.client.model.ModelBase.copyModelAngles(original.bipedRightLeg, replacement.bipedRightLeg);
		net.minecraft.client.model.ModelBase.copyModelAngles(original.bipedLeftLeg, replacement.bipedLeftLeg);
		return replacement;
	}

	/** NeoForge {@code setupModelAnimations(...)}: the item's own animation of its armor model (none by default). */
	default void setupModelAnimations(net.minecraft.entity.EntityLivingBase entity, ItemStack stack, net.minecraft.inventory.EntityEquipmentSlot slot,
			net.minecraft.client.model.ModelBase model, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
			float headPitch) {
	}

	/** NeoForge {@code getDefaultDyeColor(stack)}: leather's brown for leather armor, else white (no tint). */
	default int getDefaultDyeColor(ItemStack stack) {
		return stack.getItem() instanceof net.minecraft.item.ItemArmor armor && armor.getArmorMaterial() == net.minecraft.item.ItemArmor.ArmorMaterial.LEATHER
				? 0xFFA06540 : 0xFFFFFFFF;
	}

	/** NeoForge {@code getArmorLayerTintColor(stack, entity, layer, layerIndex, fallback)}: the dye color for a dyeable layer, else white. */
	default int getArmorLayerTintColor(ItemStack stack, net.minecraft.entity.EntityLivingBase entity, io.github.xponer2.translator.item.ArmorMaterialData.Layer layer,
			int layerIndex, int fallbackColor) {
		return layer != null && layer.dyeable() ? fallbackColor : 0xFFFFFFFF;
	}

	/** NeoForge {@code shouldSpreadAsEntity(stack)}: a dropped stack of several is drawn as a pile (the item renderer asks). */
	default boolean shouldSpreadAsEntity(ItemStack stack) {
		return true;
	}

	/** NeoForge {@code getArmPose(entity, hand, stack)}: the item's own arm pose for the player renderer; none by default. */
	@javax.annotation.Nullable
	default net.minecraft.client.model.ModelBiped.ArmPose getArmPose(net.minecraft.entity.EntityLivingBase entity, net.minecraft.util.EnumHand hand,
			ItemStack stack) {
		return null;
	}

	/** True when the extension replaced the vanilla transform (1.12.2 still applies it; kept for the 1.21 shape). */
	default boolean applyHandTransform(EntityPlayerSP player, EnumHandSide arm, ItemStack stack, float partialTick, float equipProcess, float swingProcess) {
		return false;
	}
}
