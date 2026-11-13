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
package io.github.xponer2.translator.client.model;

import io.github.xponer2.translator.client.PoseStack;
import io.github.xponer2.translator.client.VertexConsumer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Facade of the 1.21 {@code PlayerModel} (S row) for the models that extend it (Supplementaries' jarred and pickle
 * players, the API's piglins): 1.12.2's player model with the 1.21 constructor from a baked layer (head, body, arms,
 * legs, sleeves, pants, jacket) and the 1.21 animation methods, as HumanoidModel's. Without it their constructor's
 * {@code super(root, slim)} had nothing to call, and the renderers weren't made.
 */
@SideOnly(Side.CLIENT)
public class PlayerModel<T extends EntityLivingBase> extends ModelPlayer {
	public PlayerModel(ModelPart root, boolean slim) {
		super(0.0F, slim);
		HumanoidModel.assign(this, root);
	}

	/** 1.21 {@code setupAnim}: the player model's own pose (a mod's model overrides it and calls it as super). */
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, 0.0625F, entity);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale,
			Entity entity) {
		if (entity instanceof EntityLivingBase living) {
			this.setupAnim((T) living, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		} else {
			super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
		}
	}

	/** 1.21 {@code prepareMobModel}: before the pose. */
	public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
		super.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTick);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTick) {
		this.prepareMobModel((T) entity, limbSwing, limbSwingAmount, partialTick);
	}

	/** 1.21 {@code renderToBuffer}: the parts and their outer layer in the pose stack. */
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
		for (ModelRenderer part : new ModelRenderer[] {this.bipedHead, this.bipedHeadwear, this.bipedBody, this.bipedRightArm, this.bipedLeftArm,
				this.bipedRightLeg, this.bipedLeftLeg, this.bipedBodyWear, this.bipedRightArmwear, this.bipedLeftArmwear, this.bipedRightLegwear,
				this.bipedLeftLegwear}) {
			if (part instanceof ModelPart modelPart) {
				modelPart.render(poseStack, buffer, light, overlay, color);
			} else if (part != null) {
				part.render(0.0625F);
			}
		}
	}

	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay) {
		this.renderToBuffer(poseStack, buffer, light, overlay, -1);
	}

	/** 1.21 {@code getArm(arm)} (1.12.2's name): the arm as a 1.21 part. */
	@Override
	protected ModelPart getArmForSide(net.minecraft.util.EnumHandSide side) {
		return ModelPart.of(super.getArmForSide(side));
	}

	/** 1.21 {@code translateToHand(arm, poseStack)} (1.12.2's name): to the hand of that arm, for a held item. */
	public void postRenderArm(net.minecraft.util.EnumHandSide side, PoseStack poseStack) {
		ModelPart arm = this.getArmForSide(side);
		if (arm != null) {
			arm.translateAndRotate(poseStack);
		}
	}

	/** 1.21 {@code rotlerpRad(angle, maxAngle, mul)}. */
	protected float rotlerpRad(float angle, float maxAngle, float mul) {
		return HumanoidModel.lerpAngle(angle, maxAngle, mul);
	}

	/** 1.21 {@code getHead()} (HeadedModel). */
	public ModelPart getHead() {
		return ModelPart.of(this.bipedHead);
	}

	/** 1.21 {@code copyPropertiesTo(other)}: swing, riding, child, sneaking and arm poses of this model to the other. */
	public void copyPropertiesTo(ModelBiped other) {
		other.setModelAttributes(this);
	}
}
