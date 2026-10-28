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
