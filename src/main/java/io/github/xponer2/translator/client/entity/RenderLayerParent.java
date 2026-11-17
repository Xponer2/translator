/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.client.entity;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/** 1.21 {@code RenderLayerParent}: the renderer a layer belongs to, its model and texture. */
public interface RenderLayerParent<T extends Entity, M extends net.minecraft.client.model.ModelBase> {
	M getModel();

	ResourceLocation getTextureLocation(T entity);
}
