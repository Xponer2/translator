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

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/** 1.21 {@code RenderLayerParent}: the renderer a layer belongs to, its model and texture. */
public interface RenderLayerParent<T extends Entity, M extends net.minecraft.client.model.ModelBase> {
	M getModel();

	ResourceLocation getTextureLocation(T entity);
}
