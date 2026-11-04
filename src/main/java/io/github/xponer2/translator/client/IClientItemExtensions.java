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
		TOOLTIP
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

	/** True when the extension replaced the vanilla transform (1.12.2 still applies it; kept for the 1.21 shape). */
	default boolean applyHandTransform(EntityPlayerSP player, EnumHandSide arm, ItemStack stack, float partialTick, float equipProcess, float swingProcess) {
		return false;
	}
}
