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
package io.github.xponer2.translator.registry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.xponer2.translator.runtime.RuntimeMods;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * Block items of the 1.21.1 mods that were registered without their block (a mod's block couldn't be made: a missing
 * part, a failed accessor mixin) take air as their block once the items are registered. The game calls every item
 * while it starts (the creative search tree) and a block item without a block stopped it there (Rechiseled: 3628 of
 * them when its blocks failed). They don't show anywhere and place nothing.
 */
public final class BrokenBlockItems {
	private static final Logger LOGGER = LogManager.getLogger("Translator Runtime");

	private BrokenBlockItems() {
	}

	/** After the item registry event: the block items of the runtime mods with no block get air. */
	public static void fix(IForgeRegistry<Item> items) {
		Field field = blockField();
		if (field == null) {
			return;
		}
		Map<String, Integer> byMod = new TreeMap<>();
		for (Item item : items.getValuesCollection()) {
			if (!(item instanceof ItemBlock blockItem) || blockItem.getBlock() != null || item.getRegistryName() == null) {
				continue;
			}
			String namespace = item.getRegistryName().getNamespace();
			if (!RuntimeMods.isRuntimeNamespace(namespace)) {
				continue;
			}
			try {
				field.set(blockItem, Blocks.AIR);
				byMod.merge(namespace, 1, Integer::sum);
			} catch (IllegalAccessException e) {
				LOGGER.warn("Can't give the block item {} a block", item.getRegistryName(), e);
			}
		}
		byMod.forEach((mod, count) -> LOGGER.warn("{}: {} block items have no block (their blocks couldn't be made), they place nothing", mod, count));
	}

	/** ItemBlock's block field (final), made writable; null when it isn't found. */
	private static Field blockField() {
		for (Field field : ItemBlock.class.getDeclaredFields()) {
			if (field.getType() == Block.class && !Modifier.isStatic(field.getModifiers())) {
				try {
					field.setAccessible(true);
					Field modifiers = Field.class.getDeclaredField("modifiers");
					modifiers.setAccessible(true);
					modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
					return field;
				} catch (ReflectiveOperationException e) {
					LOGGER.warn("Can't make ItemBlock's block writable", e);
					return null;
				}
			}
		}
		return null;
	}
}
