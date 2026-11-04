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

import java.util.function.Function;

import javax.annotation.Nullable;

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
 * Facade of the 1.21 {@code HumanoidModel} for the mods' models that extend it (armor models, Iron's Spellbooks'
 * wizards, Twilight Forest's giants): the 1.12.2 biped with the 1.21 constructor from a baked layer (its head, hat,
 * body, arms and legs become the biped's parts) and the 1.21 animation methods, called by 1.12.2 through
 * setRotationAngles / setLivingAnimations. Without it their constructor's {@code super(root)} had nothing to call.
 */
@SideOnly(Side.CLIENT)
public class HumanoidModel<T extends EntityLivingBase> extends ModelBiped {
	public HumanoidModel(ModelPart root) {
		this(root, null);
	}

	public HumanoidModel(ModelPart root, @Nullable Function<?, ?> renderType) {
		super(0.0F);
		assign(this, root);
	}

	/** The parts of a 1.21 humanoid layer on a 1.12.2 biped (a part the layer lacks keeps the biped's own). */
	public static void assign(ModelBiped model, ModelPart root) {
		model.boxList.clear();
		model.bipedHead = part(root, "head", model.bipedHead);
		model.bipedHeadwear = part(root, "hat", model.bipedHeadwear);
		model.bipedBody = part(root, "body", model.bipedBody);
		model.bipedRightArm = part(root, "right_arm", model.bipedRightArm);
		model.bipedLeftArm = part(root, "left_arm", model.bipedLeftArm);
		model.bipedRightLeg = part(root, "right_leg", model.bipedRightLeg);
		model.bipedLeftLeg = part(root, "left_leg", model.bipedLeftLeg);
		if (model instanceof ModelPlayer player) {
			player.bipedBodyWear = part(root, "jacket", player.bipedBodyWear);
			player.bipedLeftArmwear = part(root, "left_sleeve", player.bipedLeftArmwear);
			player.bipedRightArmwear = part(root, "right_sleeve", player.bipedRightArmwear);
			player.bipedLeftLegwear = part(root, "left_pants", player.bipedLeftLegwear);
			player.bipedRightLegwear = part(root, "right_pants", player.bipedRightLegwear);
		}
	}

	private static ModelRenderer part(ModelPart root, String name, ModelRenderer fallback) {
		return root.hasChild(name) ? root.getChild(name) : fallback;
	}

	// --- 1.21 animation, called by 1.12.2 ---------------------------------------------------------------------------

	/** 1.21 {@code setupAnim}: the biped's own pose (a mod's model overrides it and calls it as super). */
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

	/** 1.21 {@code prepareMobModel}: before the pose (the biped's arm swing and riding state). */
	public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
		super.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTick);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTick) {
		this.prepareMobModel((T) entity, limbSwing, limbSwingAmount, partialTick);
	}

	/** 1.21 {@code renderToBuffer}: the parts in the pose stack (a layer drawing the model itself). */
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
		for (ModelRenderer part : new ModelRenderer[] {this.bipedHead, this.bipedHeadwear, this.bipedBody, this.bipedRightArm, this.bipedLeftArm,
				this.bipedRightLeg, this.bipedLeftLeg}) {
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

	/** 1.21 {@code copyPropertiesTo(other)}: swing, riding, child, sneaking and arm poses of this model to the other. */
	public void copyPropertiesTo(ModelBiped other) {
		other.setModelAttributes(this);
	}
}
