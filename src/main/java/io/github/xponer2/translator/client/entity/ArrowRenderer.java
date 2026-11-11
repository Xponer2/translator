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
import io.github.xponer2.translator.client.PoseStack;

import net.minecraft.client.renderer.entity.RenderArrow;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Facade of the 1.21 {@code ArrowRenderer} (neoforge-1.21.1-redirects.tsv) for the mods' arrow and dart renderers
 * (the Aether's darts and needles, Iron's Spells, Twilight Forest): 1.12.2's arrow drawing with the mod's texture.
 * Their {@code super(context)} had no 1.12.2 constructor to call, so the renderers weren't made and the arrows weren't
 * drawn.
 */
@SideOnly(Side.CLIENT)
public abstract class ArrowRenderer<T extends EntityArrow> extends RenderArrow<T> {
	private double x;
	private double y;
	private double z;

	protected ArrowRenderer(RendererContext context) {
		super(context.getEntityRenderDispatcher());
	}

	/** 1.21: the texture of the arrow. */
	public abstract ResourceLocation getTextureLocation(T entity);

	@Override
	protected ResourceLocation getEntityTexture(T entity) {
		return this.getTextureLocation(entity);
	}

	@Override
	public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.render(entity, entityYaw, partialTicks, PoseStack.INSTANCE, MultiBufferSource.INSTANCE, entity.getBrightnessForRender());
	}

	/** 1.21 {@code render(...)}: the 1.12.2 arrow at the entity's place (a mod's override calls it as super). */
	public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
		MultiBufferSource.INSTANCE.endBatch();
		super.doRender(entity, this.x, this.y, this.z, entityYaw, partialTick);
	}
}
