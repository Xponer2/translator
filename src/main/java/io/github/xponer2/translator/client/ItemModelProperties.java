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
		item.addPropertyOverride(name, (stack, world, entity) -> {
			try {
				return function.call(stack, world, entity, 0);
			} catch (Throwable t) {
				// asked for every frame the item is drawn: a failing property (a 1.21.1 mod's code the Translator can't
				// run yet) draws the base model instead of crashing the game
				if (FAILED.add(name)) {
					org.apache.logging.log4j.LogManager.getLogger("Translator").error("The model property {} of {} failed", name,
							item.getRegistryName(), t);
				}
				return 0.0F;
			}
		});
	}

	private static final java.util.Set<ResourceLocation> FAILED = java.util.concurrent.ConcurrentHashMap.newKeySet();
}
