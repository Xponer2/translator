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
package io.github.xponer2.translator.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.gson.JsonObject;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;

/**
 * A 1.21 loot table of a mod's data as a 1.12.2 LootTable: chests of the mods' structures, the tables mods roll
 * through {@code reloadableRegistries().getLootTable(key)} (1.12.2's manager reads only its own format and gave empty
 * tables for them).
 */
public final class JsonLootTable extends LootTable {
	private final ResourceLocation id;
	private final JsonObject json;

	private JsonLootTable(ResourceLocation id, JsonObject json) {
		super(new LootPool[0]);
		this.id = id;
		this.json = json;
	}

	/** The mod's 1.21 table of that id, else the one 1.12.2 has. */
	public static LootTable of(ResourceLocation id, LootTable fallback) {
		JsonObject json = BlockLoot.table(id);
		return json != null ? new JsonLootTable(id, json) : fallback;
	}

	/**
	 * What 1.12.2's {@code LootTableManager.getLootTableFromLocation} gives (ModLootTables): a table it found, else the
	 * mod's 1.21 table of that id (a vanilla chest the 1.21.1 mods' structures fill: Twilight Forest's, the Aether's).
	 */
	public static LootTable orModTable(LootTable found, ResourceLocation id) {
		if (found != LootTable.EMPTY_LOOT_TABLE || id == null) {
			return found;
		}
		try {
			return of(id, found);
		} catch (RuntimeException e) {
			return found;
		}
	}

	@Override
	public List<ItemStack> generateLootForPools(Random random, LootContext context) {
		var out = new ArrayList<ItemStack>();
		EntityLivingBase entity = context.getLootedEntity() instanceof EntityLivingBase living ? living : null;
		BlockLoot.generate(this.json, random, entity, context.getKillerPlayer() != null, out::add);
		return out;
	}

	@Override
	public String toString() {
		return "JsonLootTable{" + this.id + "}";
	}
}
