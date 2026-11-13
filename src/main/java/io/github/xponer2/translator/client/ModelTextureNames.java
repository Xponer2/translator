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
package io.github.xponer2.translator.client;

import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A block model's texture name resolved as 1.21 does (core.ModelTextureRefs puts this in ModelBlock's place): "#name"
 * is looked up from the model through its parents, again for as long as the answer is another "#name" (a few hops: a
 * model naming itself ends there). 1.12.2 refused a reference to a reference of the same model.
 */
@SideOnly(Side.CLIENT)
public final class ModelTextureNames {
	private static final String MISSING = "missingno";

	private ModelTextureNames() {
	}

	/** The texture of the name ("#side" or "side"), "missingno" when none. Object: the hook's descriptor names no game class. */
	public static String resolve(Object self, String name) {
		ModelBlock model = (ModelBlock) self;
		String current = name == null ? null : name.startsWith("#") ? name : "#" + name;
		for (int hop = 0; hop < 16 && current != null && current.startsWith("#"); hop++) {
			String key = current.substring(1);
			String found = null;
			for (ModelBlock m = model; m != null && found == null; m = m.parent) {
				found = m.textures.get(key);
			}
			current = found;
		}
		return current != null && !current.startsWith("#") ? current : MISSING;
	}
}
