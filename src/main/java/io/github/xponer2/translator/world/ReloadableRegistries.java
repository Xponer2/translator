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
package io.github.xponer2.translator.world;

import java.util.Collection;
import java.util.Collections;

import io.github.xponer2.translator.registry.ResourceKey;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootTable;

/**
 * 1.21 {@code ReloadableServerRegistries.Holder} ({@code server.reloadableRegistries()}): the loot tables of the data
 * packs, which 1.12.2 keeps in the world's LootTableManager (9 mods drop their bosses', blocks' and chests' loot
 * through it: Twilight Forest, Aether, Artifacts, Iron's Spellbooks, Supplementaries).
 */
public class ReloadableRegistries {
	private final MinecraftServer server;

	public ReloadableRegistries(MinecraftServer server) {
		this.server = server;
	}

	/** The table of that id; the empty table when there is none (or no world yet), as in 1.21. */
	public LootTable getLootTable(ResourceKey<?> key) {
		WorldServer world = this.server == null ? null : this.server.getWorld(0);
		if (key == null || world == null) {
			return LootTable.EMPTY_LOOT_TABLE;
		}
		LootTable table = io.github.xponer2.translator.loot.JsonLootTable.of(key.location(),
				world.getLootTableManager().getLootTableFromLocation(key.location()));
		return table == null ? LootTable.EMPTY_LOOT_TABLE : table;
	}

	/** The ids of a reloadable registry's entries: the loot tables 1.12.2 knows by name only, so none listed. */
	public Collection<ResourceLocation> getKeys(ResourceKey<?> registry) {
		return Collections.emptyList();
	}
}
