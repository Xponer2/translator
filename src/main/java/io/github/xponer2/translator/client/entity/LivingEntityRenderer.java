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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.joml.Quaternionf;

import io.github.xponer2.translator.client.MultiBufferSource;
import io.github.xponer2.translator.client.OverlayTexture;
import io.github.xponer2.translator.client.PoseStack;
import io.github.xponer2.translator.client.RenderType;
import io.github.xponer2.translator.client.VertexConsumer;
import io.github.xponer2.translator.client.model.EntityModel;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 {@code LivingEntityRenderer}: a living entity drawn as 1.21 does it, turned to its body's yaw, flipped into
 * model space, its model posed ({@code prepareMobModel}, {@code setupAnim}) and drawn with the render type of its
 * texture, then its layers.
 */
@SideOnly(Side.CLIENT)
public abstract class LivingEntityRenderer<T extends EntityLivingBase, M extends EntityModel<T>> extends EntityRenderer<T>
		implements RenderLayerParent<T, M> {
	protected M model;
	protected final List<RenderLayer<T, M>> layers = new ArrayList<>();

	public LivingEntityRenderer(RendererContext context, M model, float shadowRadius) {
		super(context);
		this.model = model;
		this.shadowSize = shadowRadius;
	}

	public final boolean addLayer(RenderLayer<T, M> layer) {
		return this.layers.add(layer);
	}

	@Override
	public M getModel() {
		return this.model;
	}

	@Override
	public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
		poseStack.pushPose();
		try {
			this.model.attackTime = this.getAttackAnim(entity, partialTick);
			boolean sitting = entity.isRiding() && entity.getRidingEntity() != null && entity.getRidingEntity().shouldRiderSit();
			this.model.riding = sitting;
			this.model.young = entity.isChild();
			float bodyYaw = rotLerp(partialTick, entity.prevRenderYawOffset, entity.renderYawOffset);
			float headYaw = rotLerp(partialTick, entity.prevRotationYawHead, entity.rotationYawHead);
			float netHeadYaw = MathHelper.wrapDegrees(headYaw - bodyYaw);
			float headPitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTick;
			float bob = this.getBob(entity, partialTick);
			this.setupRotations(entity, poseStack, bob, bodyYaw, partialTick, 1.0F);
			poseStack.scale(-1.0F, -1.0F, 1.0F);
			this.scale(entity, poseStack, partialTick);
			poseStack.translate(0.0F, -1.501F, 0.0F);
			float speed = 0.0F;
			float position = 0.0F;
			if (!sitting && entity.isEntityAlive()) {
				speed = Math.min(1.0F, entity.prevLimbSwingAmount + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTick);
				position = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTick);
				if (entity.isChild()) {
					position *= 3.0F;
				}
			}
			this.model.prepareMobModel(entity, position, speed, partialTick);
			this.model.setupAnim(entity, position, speed, bob, netHeadYaw, headPitch);
			Minecraft minecraft = Minecraft.getMinecraft();
			boolean visible = this.isBodyVisible(entity);
			boolean translucent = !visible && minecraft.player != null && !entity.isInvisibleToPlayer(minecraft.player);
			RenderType type = this.getRenderType(entity, visible, translucent, false);
			if (type != null) {
				VertexConsumer buffer = buffers.getBuffer(type);
				int overlay = getOverlayCoords(entity, this.getWhiteOverlayProgress(entity, partialTick));
				this.model.renderToBuffer(poseStack, buffer, light, overlay, translucent ? 0x26FFFFFF : -1);
				buffers.endBatch();
			}
			if (!(entity instanceof EntityPlayer player && player.isSpectator())) {
				for (RenderLayer<T, M> layer : this.layers) {
					layer.render(poseStack, buffers, light, entity, position, speed, partialTick, bob, netHeadYaw, headPitch);
					buffers.endBatch();
				}
			}
		} finally {
			poseStack.popPose();
		}
		super.render(entity, entityYaw, partialTick, poseStack, buffers, light);
	}

	@Nullable
	protected RenderType getRenderType(T entity, boolean bodyVisible, boolean translucent, boolean glowing) {
		ResourceLocation texture = this.getTextureLocation(entity);
		if (translucent) {
			return RenderType.itemEntityTranslucentCull(texture);
		}
		return bodyVisible ? this.model.renderType(texture) : null;
	}

	public static int getOverlayCoords(EntityLivingBase entity, float whiteOverlay) {
		return OverlayTexture.pack((int) (whiteOverlay * 15.0F), entity.hurtTime > 0 || entity.deathTime > 0 ? 3 : 10);
	}

	protected boolean isBodyVisible(T entity) {
		return !entity.isInvisible();
	}

	protected float getWhiteOverlayProgress(T entity, float partialTick) {
		return 0.0F;
	}

	protected float getFlipDegrees(T entity) {
		return 90.0F;
	}

	protected boolean isShaking(T entity) {
		return false;
	}

	/** 1.21: turned to the body's yaw, and over on its side while dying. */
	protected void setupRotations(T entity, PoseStack poseStack, float bob, float bodyYaw, float partialTick, float scale) {
		if (this.isShaking(entity)) {
			bodyYaw += (float) (Math.cos(entity.ticksExisted * 3.25D) * Math.PI * 0.4F);
		}
		poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(180.0F - bodyYaw)));
		if (entity.deathTime > 0) {
			float progress = (entity.deathTime + partialTick - 1.0F) / 20.0F * 1.6F;
			progress = Math.min(1.0F, MathHelper.sqrt(progress));
			poseStack.mulPose(new Quaternionf().rotationZ((float) Math.toRadians(progress * this.getFlipDegrees(entity))));
		}
	}

	protected void scale(T entity, PoseStack poseStack, float partialTick) {
	}

	protected float getAttackAnim(T entity, float partialTick) {
		return entity.getSwingProgress(partialTick);
	}

	protected float getBob(T entity, float partialTick) {
		return entity.ticksExisted + partialTick;
	}

	private static float rotLerp(float partialTick, float from, float to) {
		return from + MathHelper.wrapDegrees(to - from) * partialTick;
	}
}
