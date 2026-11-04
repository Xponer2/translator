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

import io.github.xponer2.translator.client.model.EntityModel;

import net.minecraft.entity.EntityLiving;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 1.21 {@code MobRenderer}: a living renderer of a mob, its name shown when it has a custom one and is looked at. */
@SideOnly(Side.CLIENT)
public abstract class MobRenderer<T extends EntityLiving, M extends EntityModel<T>> extends LivingEntityRenderer<T, M> {
	public MobRenderer(RendererContext context, M model, float shadowRadius) {
		super(context, model, shadowRadius);
	}

	@Override
	public boolean shouldShowName(T entity) {
		return super.shouldShowName(entity) && (entity.getAlwaysRenderNameTagForRender()
				|| entity.hasCustomName() && entity == this.renderManager.pointedEntity);
	}
}
