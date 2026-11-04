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

import io.github.xponer2.translator.client.model.EntityModelSet;
import io.github.xponer2.translator.client.model.ModelPart;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 {@code EntityRendererProvider.Context}: what a mod's renderer is made with, the game's 1.12.2 render managers
 * and the {@link EntityModelSet} of the mods' layer definitions.
 */
@SideOnly(Side.CLIENT)
public class RendererContext {
	public RenderManager getEntityRenderDispatcher() {
		return Minecraft.getMinecraft().getRenderManager();
	}

	public RenderItem getItemRenderer() {
		return Minecraft.getMinecraft().getRenderItem();
	}

	public BlockRendererDispatcher getBlockRenderDispatcher() {
		return Minecraft.getMinecraft().getBlockRendererDispatcher();
	}

	public ItemRenderer getItemInHandRenderer() {
		return Minecraft.getMinecraft().getItemRenderer();
	}

	public IResourceManager getResourceManager() {
		return Minecraft.getMinecraft().getResourceManager();
	}

	public EntityModelSet getModelSet() {
		return EntityModelSet.INSTANCE;
	}

	public ModelManager getModelManager() {
		return Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getModelManager();
	}

	public FontRenderer getFont() {
		return Minecraft.getMinecraft().fontRenderer;
	}

	public ModelPart bakeLayer(Object location) {
		return EntityModelSet.INSTANCE.bakeLayer(location);
	}
}
