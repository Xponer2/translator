/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.client.model;

import io.github.xponer2.translator.client.ModelData;

import net.minecraftforge.common.property.IUnlistedProperty;

/** The unlisted property a block's extended state carries its NeoForge model data in, for the model's getQuads. */
public final class ModelDataProperty implements IUnlistedProperty<ModelData> {
	public static final ModelDataProperty INSTANCE = new ModelDataProperty();

	private ModelDataProperty() {
	}

	@Override
	public String getName() {
		return "translator_model_data";
	}

	@Override
	public boolean isValid(ModelData value) {
		return true;
	}

	@Override
	public Class<ModelData> getType() {
		return ModelData.class;
	}

	@Override
	public String valueToString(ModelData value) {
		return String.valueOf(value);
	}
}
