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

import io.github.xponer2.translator.client.RenderType;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 {@code EntityModel}: the model of an entity, posed each frame ({@code setupAnim}) before it is drawn. The 1.21
 * models built on it (HierarchicalModel, ListModel, the mods' own) are the 1.21 classes as they are.
 */
@SideOnly(Side.CLIENT)
public abstract class EntityModel<T extends Entity> extends Model {
	// attackTime, riding and young are 1.12.2's swingProgress, isRiding and isChild (field rows of the tables)

	protected EntityModel() {
		this(RenderType::entityCutoutNoCull);
	}

	protected EntityModel(Function<ResourceLocation, RenderType> renderType) {
		super(renderType);
	}

	public abstract void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch);

	public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
	}

	public void copyPropertiesTo(net.minecraft.client.model.ModelBase other) {
		other.setModelAttributes(this);
	}
}
