/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.client.entity;

import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 {@code ItemEntityRenderer}, made from a renderer context ({@code registerEntityRenderer(type,
 * ItemEntityRenderer::new)}: a mod's own item entity, a backpack that doesn't burn): 1.12.2's item entity renderer.
 */
@SideOnly(Side.CLIENT)
public class ItemEntityRenderer extends RenderEntityItem {
	public ItemEntityRenderer(RendererContext context) {
		super(context.getEntityRenderDispatcher(), context.getItemRenderer());
	}
}
