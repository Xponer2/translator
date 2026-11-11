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
package io.github.xponer2.translator.client.entity;

import io.github.xponer2.translator.client.MultiBufferSource;
import io.github.xponer2.translator.client.OverlayTexture;
import io.github.xponer2.translator.client.PoseStack;
import io.github.xponer2.translator.client.RenderType;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 1.21 {@code RenderLayer}: something a living renderer draws over its model (eyes, armor, held items, glow). */
@SideOnly(Side.CLIENT)
public abstract class RenderLayer<T extends Entity, M extends net.minecraft.client.model.ModelBase> {
	private final RenderLayerParent<T, M> renderer;

	public RenderLayer(RenderLayerParent<T, M> renderer) {
		this.renderer = renderer;
	}

	protected static <T extends EntityLivingBase> void coloredCutoutModelCopyLayerRender(net.minecraft.client.model.ModelBase parent, net.minecraft.client.model.ModelBase model,
			ResourceLocation texture, PoseStack poseStack, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
			float ageInTicks, float netHeadYaw, float headPitch, float partialTick, int color) {
		if (!entity.isInvisible()) {
			io.github.xponer2.translator.runtime.redirect.ModelBridges.copyPropertiesTo(parent, model);
			io.github.xponer2.translator.runtime.redirect.ModelBridges.prepareMobModel(model, entity, limbSwing, limbSwingAmount, partialTick);
			io.github.xponer2.translator.runtime.redirect.ModelBridges.setupAnim(model, entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			renderColoredCutoutModel(model, texture, poseStack, buffers, light, entity, color);
		}
	}

	protected static <T extends EntityLivingBase> void renderColoredCutoutModel(net.minecraft.client.model.ModelBase model, ResourceLocation texture, PoseStack poseStack,
			MultiBufferSource buffers, int light, T entity, int color) {
		io.github.xponer2.translator.runtime.redirect.ModelBridges.renderToBuffer(model, poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light,
				LivingEntityRenderer.getOverlayCoords(entity, 0.0F), color);
		buffers.endBatch();
	}

	public M getParentModel() {
		return this.renderer.getModel();
	}

	protected ResourceLocation getTextureLocation(T entity) {
		return this.renderer.getTextureLocation(entity);
	}

	public abstract void render(PoseStack poseStack, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
			float partialTick, float ageInTicks, float netHeadYaw, float headPitch);

	/** The overlay of no hurt flash, for layers that draw with it. */
	protected static int noOverlay() {
		return OverlayTexture.NO_OVERLAY;
	}
}
