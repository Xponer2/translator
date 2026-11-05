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
