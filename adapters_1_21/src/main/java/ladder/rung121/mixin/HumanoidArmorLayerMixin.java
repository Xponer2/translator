/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import ladder.rung121.ArmorArgs;
import ladder.rung121.client.ClientItemApi;
import ladder.rung121.client.ClientItemDefaults;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;

/**
 * The armour layer of NeoForge 21.1 on 1.21: its render keeps the entity's animation values (ArmorArgs, for mods'
 * mixins into 21.1's piece), and a piece whose client extensions are a converted mod's that override 21.1's armour
 * hooks (model animations, the default dye colour, a layer's tint) is drawn as 21.1 draws it, those hooks asked.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private HumanoidArmorLayerMixin() {
        super(null);
    }

    @Shadow
    protected abstract void setPartVisibility(A model, EquipmentSlot slot);

    @Shadow
    protected abstract Model getArmorModelHook(T entity, ItemStack itemStack, EquipmentSlot slot, A model);

    @Shadow
    private boolean usesInnerModel(EquipmentSlot slot) {
        throw new AssertionError();
    }

    @Shadow
    private void renderTrim(Holder<ArmorMaterial> material, PoseStack poseStack, MultiBufferSource buffers, int light, ArmorTrim trim, Model model,
            boolean inner) {
    }

    @Shadow
    private void renderGlint(PoseStack poseStack, MultiBufferSource buffers, int light, Model model) {
    }

    /**
     * One layer of the piece in its tint: the model into the armour render type of the layer's texture (the layer's own
     * private renderModel changes its colour parameters between versions; the public calls convert down the ladder)
     */
    @Unique
    private static void ladder$renderLayer(PoseStack poseStack, MultiBufferSource buffers, int light, Model model, int color, ResourceLocation texture) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.armorCutoutNoCull(texture));
        model.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, color);
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"))
    private void ladder$animationValues(PoseStack poseStack, MultiBufferSource buffers, int light, LivingEntity entity, float limbSwing,
            float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        ArmorArgs.set(limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("RETURN"))
    private void ladder$animationValuesDone(PoseStack poseStack, MultiBufferSource buffers, int light, LivingEntity entity, float limbSwing,
            float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        ArmorArgs.clear();
    }

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void ladder$modArmourHooks(PoseStack poseStack, MultiBufferSource buffers, T entity, EquipmentSlot slot, int light, A armourModel,
            CallbackInfo ci) {
        ItemStack stack = entity.getItemBySlot(slot);
        if (!(stack.getItem() instanceof ArmorItem armour) || armour.getEquipmentSlot() != slot) {
            return;
        }
        ClientItemApi api = ClientItemDefaults.ownArmour(stack);
        if (api == null) {
            return;
        }
        ci.cancel();
        this.getParentModel().copyPropertiesTo(armourModel);
        this.setPartVisibility(armourModel, slot);
        Model model = this.getArmorModelHook(entity, stack, slot, armourModel);
        boolean inner = this.usesInnerModel(slot);
        ArmorMaterial material = armour.getMaterial().value();
        api.setupModelAnimations(entity, stack, slot, model, ArmorArgs.limbSwing(), ArmorArgs.limbSwingAmount(), ArmorArgs.partialTick(),
                ArmorArgs.ageInTicks(), ArmorArgs.netHeadYaw(), ArmorArgs.headPitch());
        int fallback = api.getDefaultDyeColor(stack);
        for (int index = 0; index < material.layers().size(); index++) {
            ArmorMaterial.Layer layer = material.layers().get(index);
            int color = api.getArmorLayerTintColor(stack, entity, layer, index, fallback);
            if (color != 0) {
                ResourceLocation texture = net.neoforged.neoforge.client.ClientHooks.getArmorTexture(entity, stack, layer, inner, slot);
                ladder$renderLayer(poseStack, buffers, light, model, color, texture);
            }
        }
        ArmorTrim trim = stack.get(DataComponents.TRIM);
        if (trim != null) {
            this.renderTrim(armour.getMaterial(), poseStack, buffers, light, trim, model, inner);
        }
        if (stack.hasFoil()) {
            this.renderGlint(poseStack, buffers, light, model);
        }
    }
}
