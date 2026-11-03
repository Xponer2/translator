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
package io.github.xponer2.translator.runtime.redirect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.init.Items;
import net.minecraft.item.Item;

/**
 * The 1.21 {@code Villager} statics 1.12.2's EntityVillager lacks: the items villagers pick up and what they are worth
 * as food. Mods add their crops to them (Farmer's Delight); 1.12.2 villagers don't read them, so they only have to be
 * there.
 */
public final class VillagerStatics {
	public static Set<Item> WANTED_ITEMS = new HashSet<>(java.util.Arrays.asList(Items.BREAD, Items.POTATO, Items.CARROT, Items.WHEAT,
			Items.WHEAT_SEEDS, Items.BEETROOT, Items.BEETROOT_SEEDS));
	public static Map<Item, Integer> FOOD_POINTS = new HashMap<>();

	static {
		FOOD_POINTS.put(Items.BREAD, 4);
		FOOD_POINTS.put(Items.POTATO, 1);
		FOOD_POINTS.put(Items.CARROT, 1);
		FOOD_POINTS.put(Items.BEETROOT, 1);
	}

	private VillagerStatics() {
	}
}
