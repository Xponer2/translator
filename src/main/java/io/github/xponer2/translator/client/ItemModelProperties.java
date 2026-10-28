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

import io.github.xponer2.translator.item.ItemPropertyFunction;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 client ItemProperties#register: a model property of an item (1.12.2 property override).
 */
@SideOnly(Side.CLIENT)
public final class ItemModelProperties {
	private ItemModelProperties() {
	}

	public static void register(Item item, ResourceLocation name, ItemPropertyFunction function) {
		item.addPropertyOverride(name, (stack, world, entity) -> function.call(stack, world, entity, 0));
	}
}
