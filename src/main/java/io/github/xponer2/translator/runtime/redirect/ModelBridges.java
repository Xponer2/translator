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
package io.github.xponer2.translator.runtime.redirect;

import io.github.xponer2.translator.client.MultiBufferSource;
import io.github.xponer2.translator.client.PoseStack;
import io.github.xponer2.translator.client.RenderType;
import io.github.xponer2.translator.client.VertexConsumer;
import io.github.xponer2.translator.client.model.EntityModel;
import io.github.xponer2.translator.client.model.HumanoidModel;
import io.github.xponer2.translator.client.model.Model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

/**
 * Static stand-ins for 1.21 model calls (neoforge-1.21.1-redirects.tsv): the item display transform ({@code
 * ItemTransform}, 1.12.2's {@link ItemTransformVec3f}, JOML vectors in 1.21 and LWJGL ones in 1.12.2), and the
 * {@code Model} / {@code EntityModel} methods on 1.12.2's ModelBase, which the tables map both to. Client only.
 */
public final class ModelBridges {
	private ModelBridges() {
	}

	/** 1.21 {@code new ItemTransform(rotation, translation, scale)} (Sophisticated Storage's barrel item transforms). */
	public static ItemTransformVec3f newItemTransformVec3f(org.joml.Vector3f rotation, org.joml.Vector3f translation, org.joml.Vector3f scale) {
		return new ItemTransformVec3f(lwjgl(rotation), lwjgl(translation), lwjgl(scale));
	}

	/** NeoForge's {@code new ItemTransform(rotation, translation, scale, rightRotation)}: the right rotation left out. */
	public static ItemTransformVec3f newItemTransformVec3f(org.joml.Vector3f rotation, org.joml.Vector3f translation, org.joml.Vector3f scale,
			org.joml.Vector3f rightRotation) {
		return newItemTransformVec3f(rotation, translation, scale);
	}

	// the record's fields, read by renderers that apply a transform themselves (Iron's Spells' staffs, display items)

	public static org.joml.Vector3f rotation(ItemTransformVec3f transform) {
		return joml(transform.rotation);
	}

	public static org.joml.Vector3f translation(ItemTransformVec3f transform) {
		return joml(transform.translation);
	}

	public static org.joml.Vector3f scale(ItemTransformVec3f transform) {
		return joml(transform.scale);
	}

	// --- 1.21 model calls on any model (the tables map Model and EntityModel to 1.12.2's ModelBase, so a mod's model,
	// a 1.12.2 biped and the facades are one kind): the Translator's 1.21 models answer themselves, 1.12.2 models
	// through their 1.12.2 methods

	/** 1.21 {@code model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch)}. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void setupAnim(ModelBase model, Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {
		if (model instanceof EntityModel entityModel) {
			entityModel.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		} else if (model instanceof HumanoidModel humanoid && entity instanceof EntityLivingBase living) {
			humanoid.setupAnim(living, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		} else if (model != null) {
			model.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, 0.0625F, entity);
		}
	}

	/** 1.21 {@code model.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick)}. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void prepareMobModel(ModelBase model, Entity entity, float limbSwing, float limbSwingAmount, float partialTick) {
		if (model instanceof EntityModel entityModel) {
			entityModel.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
		} else if (model != null && entity instanceof EntityLivingBase living) {
			model.setLivingAnimations(living, limbSwing, limbSwingAmount, partialTick);
		}
	}

	/** 1.21 {@code model.copyPropertiesTo(other)}: swing, riding, child (and a biped's poses) to the other model. */
	public static void copyPropertiesTo(ModelBase model, ModelBase other) {
		if (model instanceof EntityModel<?> entityModel) {
			entityModel.copyPropertiesTo(other);
		} else if (model != null && other != null) {
			other.setModelAttributes(model);
		}
	}

	/** 1.21 {@code model.renderType(texture)}: the model's own, the entity cutout of 1.21's default otherwise. */
	public static RenderType renderType(ModelBase model, ResourceLocation texture) {
		return model instanceof Model own ? own.renderType(texture) : RenderType.entityCutoutNoCull(texture);
	}

	public static void renderToBuffer(ModelBase model, PoseStack pose, VertexConsumer buffer, int light, int overlay) {
		renderToBuffer(model, pose, buffer, light, overlay, -1);
	}

	/**
	 * 1.21 {@code model.renderToBuffer(pose, buffer, light, overlay, color)}. A 1.12.2 model (a vanilla mob's or
	 * player's, a biped armor model) is drawn by its parts as posed, in the GL matrix, with the texture and state of
	 * the buffer's render type, tinted by the color and lit by the light.
	 */
	public static void renderToBuffer(ModelBase model, PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
		if (model instanceof Model own) {
			own.renderToBuffer(pose, buffer, light, overlay, color);
			return;
		}
		if (model instanceof HumanoidModel<?> humanoid) {
			humanoid.renderToBuffer(pose, buffer, light, overlay, color);
			return;
		}
		if (model == null) {
			return;
		}
		MultiBufferSource.INSTANCE.endBatch();
		RenderType type = buffer.type();
		ResourceLocation texture = type.texture();
		if (texture != null) {
			Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		}
		type.setupRenderState();
		GlStateManager.enableRescaleNormal();
		GlStateManager.color((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, (color >>> 24) / 255.0F);
		net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit, light & 0xFFFF,
				light >> 16 & 0xFFFF);
		try {
			if (model instanceof ModelBiped biped) {
				renderBiped(biped);
			} else {
				for (ModelRenderer part : topLevel(model)) {
					part.render(0.0625F);
				}
			}
		} finally {
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			type.clearRenderState();
		}
	}

	/** 1.21 {@code humanoidModel.translateToHand(arm, pose)}: the GL matrix moved to the hand, as 1.12.2's held items. */
	public static void translateToHand(ModelBiped model, net.minecraft.util.EnumHandSide arm, PoseStack pose) {
		model.postRenderArm(0.0625F, arm);
	}

	/** 1.21 {@code playerRenderer.getModel()} on a 1.12.2 renderer (the player's): its main model. */
	public static ModelBase getModel(net.minecraft.client.renderer.entity.RenderLivingBase<?> renderer) {
		return renderer.getMainModel();
	}

	/** 1.21 {@code armPose.isTwoHanded()}: of 1.12.2's poses only the bow's (the crossbow poses are it here). */
	public static boolean isTwoHanded(ModelBiped.ArmPose pose) {
		return pose == ModelBiped.ArmPose.BOW_AND_ARROW;
	}

	/** A write of 1.21 {@code humanoidModel.swimAmount} (the Aether's shield copies it): 1.12.2's players don't swim. */
	public static void swimAmount(ModelBiped model, float amount) {
	}

	/** 1.21 {@code headedModel.getHead()}: a biped's head as a 1.21 part. */
	public static io.github.xponer2.translator.client.model.ModelPart getHead(ModelBiped model) {
		return io.github.xponer2.translator.client.model.ModelPart.of(model.bipedHead);
	}

	/** A biped's parts as 1.12.2 draws them (a child's head bigger), without posing it again. */
	private static void renderBiped(ModelBiped model) {
		var parts = new java.util.ArrayList<ModelRenderer>(java.util.Arrays.asList(model.bipedBody, model.bipedRightArm, model.bipedLeftArm,
				model.bipedRightLeg, model.bipedLeftLeg));
		if (model instanceof ModelPlayer player) {
			parts.addAll(java.util.Arrays.asList(player.bipedBodyWear, player.bipedRightArmwear, player.bipedLeftArmwear, player.bipedRightLegwear,
					player.bipedLeftLegwear));
		}
		float scale = 0.0625F;
		if (model.isChild) {
			GlStateManager.pushMatrix();
			GlStateManager.scale(0.75F, 0.75F, 0.75F);
			GlStateManager.translate(0.0F, 16.0F * scale, 0.0F);
			model.bipedHead.render(scale);
			model.bipedHeadwear.render(scale);
			GlStateManager.popMatrix();
			GlStateManager.pushMatrix();
			GlStateManager.scale(0.5F, 0.5F, 0.5F);
			GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
			for (ModelRenderer part : parts) {
				part.render(scale);
			}
			GlStateManager.popMatrix();
		} else {
			model.bipedHead.render(scale);
			model.bipedHeadwear.render(scale);
			for (ModelRenderer part : parts) {
				part.render(scale);
			}
		}
	}

	/** The parts of a model that aren't children of another part (they draw their children). */
	private static java.util.List<ModelRenderer> topLevel(ModelBase model) {
		var children = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<ModelRenderer, Boolean>());
		for (ModelRenderer part : model.boxList) {
			if (part.childModels != null) {
				children.addAll(part.childModels);
			}
		}
		var top = new java.util.ArrayList<ModelRenderer>();
		for (ModelRenderer part : model.boxList) {
			if (!children.contains(part)) {
				top.add(part);
			}
		}
		return top;
	}

	private static org.lwjgl.util.vector.Vector3f lwjgl(org.joml.Vector3f vector) {
		return vector == null ? new org.lwjgl.util.vector.Vector3f() : new org.lwjgl.util.vector.Vector3f(vector.x, vector.y, vector.z);
	}

	private static org.joml.Vector3f joml(org.lwjgl.util.vector.Vector3f vector) {
		return vector == null ? new org.joml.Vector3f() : new org.joml.Vector3f(vector.x, vector.y, vector.z);
	}
}
