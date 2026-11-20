/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.client;

import ladder.rung121.Dispatch;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.Model;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * 21.1's defaults of the newer IClientItemExtensions hooks (the O rows' super calls), and the question whether a
 * stack's client extensions are a converted mod's that override one of them.
 */
public final class ClientItemDefaults {
    public static final ResourceLocation SPYGLASS_SCOPE = ResourceLocation.withDefaultNamespace("textures/misc/spyglass_scope.png");

    private ClientItemDefaults() {
    }

    /** the stack's client extensions when they are a converted mod's that override the named hook, else null */
    public static ClientItemApi own(ItemStack stack, String name, Class<?>... parameters) {
        IClientItemExtensions extensions = IClientItemExtensions.of(stack);
        return extensions instanceof ClientItemApi api && Dispatch.declares(extensions.getClass(), name, parameters) ? api : null;
    }

    /** whether a converted mod's extensions of the stack override any of the armour hooks */
    public static ClientItemApi ownArmour(ItemStack stack) {
        IClientItemExtensions extensions = IClientItemExtensions.of(stack);
        if (!(extensions instanceof ClientItemApi api)) {
            return null;
        }
        Class<?> type = extensions.getClass();
        boolean own = Dispatch.declares(type, "setupModelAnimations", LivingEntity.class, ItemStack.class, EquipmentSlot.class, Model.class, float.class,
                float.class, float.class, float.class, float.class, float.class)
                || Dispatch.declares(type, "getDefaultDyeColor", ItemStack.class)
                || Dispatch.declares(type, "getArmorLayerTintColor", ItemStack.class, LivingEntity.class, ArmorMaterial.Layer.class, int.class, int.class);
        return own ? api : null;
    }

    static int defaultDyeColor(ItemStack stack) {
        return stack.is(ItemTags.DYEABLE) ? FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(stack, DyedItemColor.LEATHER_COLOR)) : 0xFFFFFFFF;
    }

    static int armorLayerTintColor(ArmorMaterial.Layer layer, int fallbackColor) {
        return layer.dyeable() ? fallbackColor : 0xFFFFFFFF;
    }

    // the O rows' defaults (a mod's super calls)

    public static void setupModelAnimations(IClientItemExtensions self, LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot,
            Model model, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    public static int getDefaultDyeColor(IClientItemExtensions self, ItemStack stack) {
        return defaultDyeColor(stack);
    }

    public static int getArmorLayerTintColor(IClientItemExtensions self, ItemStack stack, LivingEntity entity, ArmorMaterial.Layer layer, int layerIdx,
            int fallbackColor) {
        return armorLayerTintColor(layer, fallbackColor);
    }

    public static ResourceLocation getScopeOverlayTexture(IClientItemExtensions self, ItemStack stack) {
        return SPYGLASS_SCOPE;
    }

    public static void renderHelmetOverlay(IClientItemExtensions self, ItemStack stack, Player player, GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
    }

    // the X rows: a call through IClientItemExtensions (a mod's override when it has one, else 21.1's default)

    public static void callSetupModelAnimations(IClientItemExtensions self, LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot,
            Model model, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (self instanceof ClientItemApi api && Dispatch.declares(self.getClass(), "setupModelAnimations", LivingEntity.class, ItemStack.class,
                EquipmentSlot.class, Model.class, float.class, float.class, float.class, float.class, float.class, float.class)) {
            api.setupModelAnimations(livingEntity, itemStack, equipmentSlot, model, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
        }
    }

    public static int callGetDefaultDyeColor(IClientItemExtensions self, ItemStack stack) {
        if (self instanceof ClientItemApi api && Dispatch.declares(self.getClass(), "getDefaultDyeColor", ItemStack.class)) {
            return api.getDefaultDyeColor(stack);
        }
        return defaultDyeColor(stack);
    }

    public static int callGetArmorLayerTintColor(IClientItemExtensions self, ItemStack stack, LivingEntity entity, ArmorMaterial.Layer layer, int layerIdx,
            int fallbackColor) {
        if (self instanceof ClientItemApi api && Dispatch.declares(self.getClass(), "getArmorLayerTintColor", ItemStack.class, LivingEntity.class,
                ArmorMaterial.Layer.class, int.class, int.class)) {
            return api.getArmorLayerTintColor(stack, entity, layer, layerIdx, fallbackColor);
        }
        return armorLayerTintColor(layer, fallbackColor);
    }

    public static ResourceLocation callGetScopeOverlayTexture(IClientItemExtensions self, ItemStack stack) {
        if (self instanceof ClientItemApi api && Dispatch.declares(self.getClass(), "getScopeOverlayTexture", ItemStack.class)) {
            return api.getScopeOverlayTexture(stack);
        }
        return SPYGLASS_SCOPE;
    }

    public static void callRenderHelmetOverlay(IClientItemExtensions self, ItemStack stack, Player player, GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (self instanceof ClientItemApi api && Dispatch.declares(self.getClass(), "renderHelmetOverlay", ItemStack.class, Player.class,
                GuiGraphics.class, DeltaTracker.class)) {
            api.renderHelmetOverlay(stack, player, guiGraphics, deltaTracker);
        }
    }
}
