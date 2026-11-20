/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.Model;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

/**
 * NeoForge 21.1's newer hooks of {@code IClientItemExtensions} (an armour piece's animation, its layers' colours, the
 * scope's texture, the helmet's overlay): a converted mod's client extensions get this interface (the O rows), and
 * the rung's mixins ask them where 21.1 does.
 */
public interface ClientItemApi {
    default void setupModelAnimations(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, Model model, float limbSwing,
            float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    default int getDefaultDyeColor(ItemStack stack) {
        return ClientItemDefaults.defaultDyeColor(stack);
    }

    default int getArmorLayerTintColor(ItemStack stack, LivingEntity entity, ArmorMaterial.Layer layer, int layerIdx, int fallbackColor) {
        return ClientItemDefaults.armorLayerTintColor(layer, fallbackColor);
    }

    default ResourceLocation getScopeOverlayTexture(ItemStack stack) {
        return ClientItemDefaults.SPYGLASS_SCOPE;
    }

    default void renderHelmetOverlay(ItemStack stack, Player player, GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
    }
}
