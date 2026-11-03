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
package io.github.xponer2.translator.item;

import io.github.xponer2.translator.registry.ResourceKey;
import io.github.xponer2.translator.util.Registries;

import net.minecraft.util.ResourceLocation;

/**
 * Keys of the 1.21 vanilla creative tabs (1.21 {@code CreativeModeTabs}), for the tab order mods give
 * ({@code withTabsBefore}); 1.12.2 orders the tabs by registration, so they only name the tabs.
 */
public final class CreativeModeTabs {
	public static final ResourceKey<CreativeModeTab> BUILDING_BLOCKS = key("building_blocks");
	public static final ResourceKey<CreativeModeTab> COLORED_BLOCKS = key("colored_blocks");
	public static final ResourceKey<CreativeModeTab> NATURAL_BLOCKS = key("natural_blocks");
	public static final ResourceKey<CreativeModeTab> FUNCTIONAL_BLOCKS = key("functional_blocks");
	public static final ResourceKey<CreativeModeTab> REDSTONE_BLOCKS = key("redstone_blocks");
	public static final ResourceKey<CreativeModeTab> HOTBAR = key("hotbar");
	public static final ResourceKey<CreativeModeTab> SEARCH = key("search");
	public static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES = key("tools_and_utilities");
	public static final ResourceKey<CreativeModeTab> COMBAT = key("combat");
	public static final ResourceKey<CreativeModeTab> FOOD_AND_DRINKS = key("food_and_drinks");
	public static final ResourceKey<CreativeModeTab> INGREDIENTS = key("ingredients");
	public static final ResourceKey<CreativeModeTab> SPAWN_EGGS = key("spawn_eggs");
	public static final ResourceKey<CreativeModeTab> OP_BLOCKS = key("op_blocks");
	public static final ResourceKey<CreativeModeTab> INVENTORY = key("inventory");

	private CreativeModeTabs() {
	}

	private static ResourceKey<CreativeModeTab> key(String name) {
		return ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation("minecraft", name));
	}
}
