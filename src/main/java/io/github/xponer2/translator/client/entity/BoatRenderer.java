/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.client.entity;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Pair;

import io.github.xponer2.translator.client.MultiBufferSource;
import io.github.xponer2.translator.client.PoseStack;

import net.minecraft.client.renderer.entity.RenderBoat;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Facade of the 1.21 {@code BoatRenderer} (S row) for the boat renderers of the wood mods (Regions Unexplored,
 * Nature's Spirit, ...): 1.12.2's boat drawn with the texture the mod's renderer gives for the boat
 * ({@code getModelWithLocation}; 1.21's boat textures have 1.12.2's layout). Their {@code super(context, chest)} had
 * no 1.12.2 constructor to call, so the renderers weren't made. A chest boat is drawn as a boat.
 */
@SideOnly(Side.CLIENT)
public class BoatRenderer extends RenderBoat {
	private double x;
	private double y;
	private double z;

	public BoatRenderer(RendererContext context, boolean chestBoat) {
		super(context.getEntityRenderDispatcher());
	}

	/** NeoForge's {@code getModelWithLocation(boat)}: the texture (and 1.21 model) of the boat; a mod's renderer gives its own. */
	@Nullable
	public Pair<ResourceLocation, Object> getModelWithLocation(EntityBoat boat) {
		return null;
	}

	/** 1.21 {@code getTextureLocation(boat)}. */
	public ResourceLocation getTextureLocation(EntityBoat boat) {
		Pair<ResourceLocation, Object> model = this.getModelWithLocation(boat);
		return model != null && model.getFirst() != null ? model.getFirst() : super.getEntityTexture(boat);
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityBoat boat) {
		return this.getTextureLocation(boat);
	}

	@Override
	public void doRender(EntityBoat entity, double x, double y, double z, float entityYaw, float partialTicks) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.render(entity, entityYaw, partialTicks, PoseStack.INSTANCE, MultiBufferSource.INSTANCE, entity.getBrightnessForRender());
	}

	/** 1.21 {@code render(...)}: the 1.12.2 boat at the entity's place (a mod's override calls it as super). */
	public void render(EntityBoat entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
		MultiBufferSource.INSTANCE.endBatch();
		super.doRender(entity, this.x, this.y, this.z, entityYaw, partialTick);
	}
}
