/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import ladder.adapter.armour.ArmourTextureApi;
import ladder.adapter.armour.RecordMaterial;
import ladder.adapter.component.ComponentTags;
import ladder.carried.net.minecraft.world.item.ArmorMaterial.Layer;
import ladder.carried.net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;

/**
 * Armour of a 1.20.5 material drawn as 1.20.5 draws it: each of the material's layers with its own texture (its suffix,
 * the inner model's for leggings), a dyeable layer tinted with the stack's dye colour (DYED_COLOR, leather's when none;
 * only for items in the dyeable tag), an item's own 1.20.5 texture hook ({@code getArmorTexture(stack, entity, slot,
 * layer, inner)}) first. 1.20.4 draws one texture named after the material, and tints only its DyeableLeatherItem.
 */
@Mixin(HumanoidArmorLayer.class)
abstract class ArmourLayerMixin<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private ArmourLayerMixin(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Shadow
    protected abstract void setPartVisibility(A model, EquipmentSlot slot);

    @Shadow
    protected abstract Model getArmorModelHook(T entity, ItemStack stack, EquipmentSlot slot, A model);

    @Shadow
    private boolean usesInnerModel(EquipmentSlot slot) {
        throw new AssertionError();
    }

    @Shadow
    private void renderModel(PoseStack pose, MultiBufferSource buffers, int light, ArmorItem item, Model model, boolean inner, float red, float green,
            float blue, ResourceLocation texture) {
        throw new AssertionError();
    }

    @Shadow
    private void renderTrim(ArmorMaterial material, PoseStack pose, MultiBufferSource buffers, int light, ArmorTrim trim, Model model, boolean inner) {
        throw new AssertionError();
    }

    @Shadow
    private void renderGlint(PoseStack pose, MultiBufferSource buffers, int light, Model model) {
        throw new AssertionError();
    }

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void ladder$recordArmour(PoseStack pose, MultiBufferSource buffers, T entity, EquipmentSlot slot, int light, A model, CallbackInfo ci) {
        ItemStack stack = entity.getItemBySlot(slot);
        if (!(stack.getItem() instanceof ArmorItem armour) || !(armour.getMaterial() instanceof RecordMaterial record) || armour.getEquipmentSlot() != slot) {
            return;
        }
        ci.cancel();
        this.getParentModel().copyPropertiesTo(model);
        this.setPartVisibility(model, slot);
        Model used = this.getArmorModelHook(entity, stack, slot, model);
        boolean inner = this.usesInnerModel(slot);
        int dye = stack.is(ComponentTags.dyeable()) ? DyedItemColor.getOrDefault(stack, DyedItemColor.LEATHER_COLOR) : -1;
        for (Layer layer : record.layers()) {
            int colour = layer.dyeable() ? dye : -1;
            this.renderModel(pose, buffers, light, armour, used, inner, (colour >> 16 & 0xFF) / 255.0F, (colour >> 8 & 0xFF) / 255.0F, (colour & 0xFF) / 255.0F,
                    ladder$texture(entity, stack, slot, layer, inner));
        }
        ArmorTrim.getTrim(entity.level().registryAccess(), stack, true).ifPresent(trim -> this.renderTrim(record, pose, buffers, light, trim, used, inner));
        if (stack.hasFoil()) {
            this.renderGlint(pose, buffers, light, used);
        }
    }

    private static ResourceLocation ladder$texture(Entity entity, ItemStack stack, EquipmentSlot slot, Layer layer, boolean inner) {
        if (stack.getItem() instanceof ArmourTextureApi own) {
            ResourceLocation texture = own.getArmorTexture(stack, entity, slot, layer, inner);
            if (texture != null) {
                return texture;
            }
        }
        return layer.texture(inner);
    }
}
