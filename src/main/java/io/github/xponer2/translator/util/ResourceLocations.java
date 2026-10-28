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
package io.github.xponer2.translator.util;

import net.minecraft.util.ResourceLocation;

/**
 * 1.21 {@code ResourceLocation.parse}: the 1.12.2 constructor {@code new ResourceLocation("c:ingots/iron")} keeps the
 * namespace "minecraft" when the namespace has a single letter, so every 1.21 common tag ({@code c:...}) would be
 * looked up as {@code minecraft:...}. The converter writes this instead of the one argument constructor.
 */
public final class ResourceLocations {
	private ResourceLocations() { }

	public static ResourceLocation parse(String id) {
		int colon = id.indexOf(':');
		return colon > 0 ? new ResourceLocation(id.substring(0, colon), id.substring(colon + 1)) : new ResourceLocation(id);
	}
}
