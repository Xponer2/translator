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

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.util.ResourceLocation;

/**
 * The vanilla resource keys of the 1.21 key holders (Biomes.PLAINS, BuiltInLootTables.SIMPLE_DUNGEON, ...): made by
 * the converter for the fields 1.12.2 doesn't have, from the field's name (MethodGuard).
 */
public final class VanillaKeys {
	private static final Map<String, ResourceKey<?>> KEYS = new ConcurrentHashMap<>();

	private VanillaKeys() {
	}

	/** The key of a holder class's field: the class (1.12.2 name of the 1.21 holder) gives the registry. */
	public static ResourceKey<?> of(String owner, String field) {
		return KEYS.computeIfAbsent(owner + "." + field, k -> create(owner, field));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static ResourceKey<?> create(String owner, String field) {
		String name = field.toLowerCase(Locale.ROOT);
		String registry;
		String path;
		switch (owner) {
		case "net/minecraft/init/Biomes":
			registry = "worldgen/biome";
			path = name;
			break;
		case "net/minecraft/world/storage/loot/LootTableList":
			registry = "loot_table";
			path = lootTable(name);
			break;
		default:
			registry = owner.substring(owner.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
			path = name;
			break;
		}
		ResourceKey registryKey = ResourceKey.createRegistryKey(new ResourceLocation("minecraft", registry));
		return ResourceKey.create(registryKey, new ResourceLocation("minecraft", path));
	}

	/** The path of a vanilla loot table by its field name: chests unless the name says otherwise. */
	private static String lootTable(String name) {
		if (name.startsWith("fishing")) {
			return "gameplay/" + name;
		}
		if (name.endsWith("_gift")) {
			return "gameplay/hero_of_the_village/" + name;
		}
		if (name.startsWith("piglin_bartering")) {
			return "gameplay/piglin_bartering";
		}
		if (name.startsWith("cat_morning_gift")) {
			return "gameplay/cat_morning_gift";
		}
		if (name.startsWith("archaeology") || name.startsWith("desert_well") || name.startsWith("trail_ruins")) {
			return "archaeology/" + name;
		}
		if (name.startsWith("shearing")) {
			return "shearing/" + name.substring("shearing_".length());
		}
		return "chests/" + name;
	}
}
