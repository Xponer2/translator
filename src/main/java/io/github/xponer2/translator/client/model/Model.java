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

import io.github.xponer2.translator.client.PoseStack;
import io.github.xponer2.translator.client.RenderType;
import io.github.xponer2.translator.client.VertexConsumer;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 {@code Model}: something drawn into a vertex consumer, with the render type of its texture. It is a 1.12.2
 * {@code ModelBase}, as the tables map 1.21 models there: a mod's model and a 1.12.2 biped are then one kind of model
 * (a mod's humanoid mob renderer took a biped where 1.21 takes an EntityModel, and didn't verify).
 */
@SideOnly(Side.CLIENT)
public abstract class Model extends net.minecraft.client.model.ModelBase {
	protected final Function<ResourceLocation, RenderType> renderType;

	public Model(Function<ResourceLocation, RenderType> renderType) {
		this.renderType = renderType;
	}

	public final RenderType renderType(ResourceLocation texture) {
		return this.renderType.apply(texture);
	}

	public abstract void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color);

	public final void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay) {
		this.renderToBuffer(poseStack, buffer, light, overlay, -1);
	}
}
