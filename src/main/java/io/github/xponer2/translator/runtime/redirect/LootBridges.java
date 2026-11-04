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

import java.util.Random;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import io.github.xponer2.translator.block.LootContextParams;
import io.github.xponer2.translator.block.LootParams;
import io.github.xponer2.translator.registry.ResourceKey;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;

/**
 * 1.21 loot on the 1.12.2 loot tables: the loot table key of a block, the parameters of a 1.12.2 loot context
 * (a global loot modifier asks it for the entity, the killer, ...), and a table's items for 1.21 loot parameters.
 */
public final class LootBridges {
	/** 1.21 {@code Registries.LOOT_TABLE}. */
	public static final ResourceKey<io.github.xponer2.translator.registry.Registry<LootTable>> LOOT_TABLE = ResourceKey
			.createRegistryKey(new ResourceLocation("minecraft", "loot_table"));

	private LootBridges() {
	}

	// --- Block --------------------------------------------------------------------------------------------------------

	/** 1.21 {@code block.getLootTable()}: {@code <namespace>:blocks/<path>}, the table 1.21 gives a block by default. */
	public static ResourceKey<LootTable> getLootTable(Block block) {
		ResourceLocation id = block.getRegistryName();
		return ResourceKey.create(LOOT_TABLE, id == null ? new ResourceLocation("minecraft", "empty")
				: new ResourceLocation(id.getNamespace(), "blocks/" + id.getPath()));
	}

	// --- LootContext --------------------------------------------------------------------------------------------------

	/** A 1.21 loot parameter from what a 1.12.2 loot context knows (entity, killer, player), else null. */
	@Nullable
	public static Object getParamOrNull(LootContext context, LootContextParams.Param<?> param) {
		Entity looted = context.getLootedEntity();
		switch (param.toString()) {
			case "this_entity":
				return looted;
			case "attacking_entity":
			case "direct_attacking_entity":
				return context.getKiller();
			case "last_damage_player":
				return context.getKillerPlayer();
			case "origin":
				return looted == null ? null : new Vec3d(looted.posX, looted.posY, looted.posZ);
			default:
				return null;
		}
	}

	public static Object getParam(LootContext context, LootContextParams.Param<?> param) {
		Object value = getParamOrNull(context, param);
		if (value == null) {
			throw new java.util.NoSuchElementException(String.valueOf(param));
		}
		return value;
	}

	public static boolean hasParam(LootContext context, LootContextParams.Param<?> param) {
		return getParamOrNull(context, param) != null;
	}

	// --- LootTable ----------------------------------------------------------------------------------------------------

	/** 1.21 {@code table.getRandomItems(params, consumer)}: the 1.12.2 table's items for a context made of the params. */
	public static void getRandomItems(LootTable table, LootParams params, Consumer<ItemStack> consumer) {
		getRandomItems(table, params, new Random().nextLong(), consumer);
	}

	public static void getRandomItems(LootTable table, LootParams params, long seed, Consumer<ItemStack> consumer) {
		LootContext context = context(params);
		if (context != null) {
			table.generateLootForPools(new Random(seed), context).forEach(consumer);
		}
	}

	public static void getRandomItems(LootTable table, LootContext context, Consumer<ItemStack> consumer) {
		table.generateLootForPools(new Random(), context).forEach(consumer);
	}

	public static it.unimi.dsi.fastutil.objects.ObjectArrayList<ItemStack> getRandomItems(LootTable table, LootParams params) {
		return getRandomItems(table, params, new Random().nextLong());
	}

	public static it.unimi.dsi.fastutil.objects.ObjectArrayList<ItemStack> getRandomItems(LootTable table, LootParams params, long seed) {
		var items = new it.unimi.dsi.fastutil.objects.ObjectArrayList<ItemStack>();
		getRandomItems(table, params, seed, items::add);
		return items;
	}

	public static void getRandomItemsRaw(LootTable table, LootParams params, Consumer<ItemStack> consumer) {
		getRandomItems(table, params, consumer);
	}

	public static void getRandomItemsRaw(LootTable table, LootContext context, Consumer<ItemStack> consumer) {
		getRandomItems(table, context, consumer);
	}

	/** A 1.12.2 loot context for 1.21 loot parameters (the server world, the entity, the player). */
	@Nullable
	private static LootContext context(LootParams params) {
		if (!(params.getLevel() instanceof WorldServer level)) {
			return null;
		}
		var builder = new LootContext.Builder(level).withLuck(params.getLuck());
		Entity entity = params.getOptionalParameter(LootContextParams.THIS_ENTITY);
		if (entity != null) {
			builder.withLootedEntity(entity);
		}
		EntityPlayer player = params.getOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER);
		if (player == null && entity instanceof EntityPlayer self) {
			player = self;
		}
		if (player != null) {
			builder.withPlayer(player);
		}
		net.minecraft.util.DamageSource source = params.getOptionalParameter(LootContextParams.DAMAGE_SOURCE);
		if (source != null) {
			builder.withDamageSource(source);
		}
		return builder.build();
	}
}
