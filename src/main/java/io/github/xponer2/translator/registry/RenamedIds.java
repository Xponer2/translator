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

import io.github.xponer2.translator.runtime.RuntimeMods;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * Ids of the 1.21.1 mods' blocks and items that earlier Translator versions registered under another name, kept in
 * the worlds made with them: a missing old id is remapped to the new one. Older versions named the light gray ones
 * by 1.12.2's dye name ({@code create:silver_postbox}); they are {@code light_gray_} now, as in 1.21.
 */
public final class RenamedIds {
	@SubscribeEvent
	public void onMissingBlocks(RegistryEvent.MissingMappings<Block> event) {
		remap(event, ForgeRegistries.BLOCKS);
	}

	@SubscribeEvent
	public void onMissingItems(RegistryEvent.MissingMappings<Item> event) {
		remap(event, ForgeRegistries.ITEMS);
	}

	private static <T extends IForgeRegistryEntry<T>> void remap(RegistryEvent.MissingMappings<T> event, IForgeRegistry<T> registry) {
		for (RegistryEvent.MissingMappings.Mapping<T> mapping : event.getAllMappings()) {
			ResourceLocation old = mapping.key;
			if (!RuntimeMods.isRuntimeNamespace(old.getNamespace()) || !old.getPath().contains("silver")) {
				continue;
			}
			// only the dye word: silver_postbox, silver_seat... (a mod's silver ore or ingot is still registered, so never missing)
			var renamed = new ResourceLocation(old.getNamespace(), old.getPath().replace("silver", "light_gray"));
			T value = registry.getValue(renamed);
			if (value != null && renamed.equals(value.getRegistryName())) {
				mapping.remap(value);
			}
		}
	}
}
