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

import javax.annotation.Nullable;

import io.github.xponer2.translator.client.MultiBufferSource;
import io.github.xponer2.translator.client.PoseStack;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Facade of the 1.21 {@code EntityRenderer} (the class a mod's renderer extends): a 1.12.2 {@link Render} whose
 * {@code doRender} runs the 1.21 {@link #render} in a pose stack at the entity, with the entity's light, and draws what
 * it put in the buffers; the 1.12.2 name tag follows.
 */
@SideOnly(Side.CLIENT)
public abstract class EntityRenderer<T extends Entity> extends Render<T> {
	protected EntityRenderer(RendererContext context) {
		super(context.getEntityRenderDispatcher());
	}

	/** 1.21: what the entity draws (its model, in a subclass); here nothing but what 1.12.2 adds after. */
	public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
	}

	public abstract ResourceLocation getTextureLocation(T entity);

	@Nullable
	@Override
	protected ResourceLocation getEntityTexture(T entity) {
		return this.getTextureLocation(entity);
	}

	@Override
	public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks) {
		int depth = PoseStack.depth();
		GlStateManager.pushMatrix();
		try {
			GlStateManager.translate(x, y, z);
			Vec3d offset = this.getRenderOffset(entity, partialTicks);
			GlStateManager.translate(offset.x, offset.y, offset.z);
			this.render(entity, entityYaw, partialTicks, PoseStack.INSTANCE, MultiBufferSource.INSTANCE, entity.getBrightnessForRender());
			MultiBufferSource.INSTANCE.endBatch();
		} catch (RuntimeException | LinkageError e) {
			// a renderer the Translator can't run all of: logged once, the entity is left undrawn this frame
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.getClass().getName() + ".render", e);
			PoseStack.restore(depth);
		} finally {
			GlStateManager.popMatrix();
		}
		super.doRender(entity, x, y, z, entityYaw, partialTicks);
	}

	public Vec3d getRenderOffset(T entity, float partialTick) {
		return Vec3d.ZERO;
	}

	public FontRenderer getFont() {
		return this.getFontRendererFromRenderManager();
	}

	/** 1.21 {@code shouldShowName}: 1.12.2's name tag rule. */
	public boolean shouldShowName(T entity) {
		return super.canRenderName(entity);
	}

	@Override
	protected boolean canRenderName(T entity) {
		return this.shouldShowName(entity);
	}

	protected int getBlockLightLevel(T entity, net.minecraft.util.math.BlockPos pos) {
		return entity.isBurning() ? 15 : entity.world.getLightFor(net.minecraft.world.EnumSkyBlock.BLOCK, pos);
	}

	protected int getSkyLightLevel(T entity, net.minecraft.util.math.BlockPos pos) {
		return entity.world.getLightFor(net.minecraft.world.EnumSkyBlock.SKY, pos);
	}

	public final int getPackedLightCoords(T entity, float partialTick) {
		return entity.getBrightnessForRender();
	}
}
