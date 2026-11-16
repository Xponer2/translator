/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package io.github.xponer2.ladder.probe;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Records what each block, item, entity and recipe of the loaded mods is and does in the real NeoForge 1.21.1 game: the
 * reference the Translator's own record of the same mods on 1.12.2 is compared with. Every fact is something a player
 * would notice when it differs: a block that can't be placed, drops something else or nothing, opens no menu or a menu
 * with other slots, has no block entity; an item with another stack size or durability, a food that feeds differently;
 * a mob of another size or health; a recipe with another result.
 */
/**
 * The facts of the converted mods' blocks, items, entities and recipes on the real 1.20.4, in the form of the 1.21.1
 * reference (the same probes: D:/translator-reference's Recorder): compare_reference.py diffs the two, so a block that
 * drops, opens or holds something else than in 1.21.1 shows without a test of its own.
 */
public final class LadderFacts {
	private static final Logger LOGGER = LoggerFactory.getLogger("LadderFacts");
	private static final Set<String> SKIPPED = Set.of("minecraft", "neoforge", "ladder_probe", "ladder_adapters", "c");

	private LadderFacts() {
	}

	public static JsonObject record(MinecraftServer server) {
		JsonObject root = new JsonObject();
		root.addProperty("minecraft", "1.20.4");
		try {
			root.add("blocks", blocks(server.overworld()));
			root.add("items", items());
			root.add("entities", entities(server.overworld()));
			root.add("recipes", recipes(server));
		} catch (RuntimeException e) {
			LOGGER.error("Recording stopped", e);
		}
		return root;
	}

	private static boolean tested(ResourceLocation id) {
		return id != null && !SKIPPED.contains(id.getNamespace());
	}

	private static JsonObject blocks(ServerLevel level) {
		JsonObject blocks = new JsonObject();
		BlockPos pos = new BlockPos(0, 120, 0);
		level.getChunk(pos);
		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		Map<String, Block> sorted = new TreeMap<>();
		for (Block block : BuiltInRegistries.BLOCK) {
			ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
			if (tested(id)) {
				sorted.put(id.toString(), block);
			}
		}
		for (Map.Entry<String, Block> entry : sorted.entrySet()) {
			Block block = entry.getValue();
			JsonObject facts = new JsonObject();
			try {
				BlockState state = block.defaultBlockState();
				facts.addProperty("hardness", round(state.getDestroySpeed(level, pos)));
				facts.addProperty("blast", round(block.getExplosionResistance()));
				facts.addProperty("light", state.getLightEmission(level, pos));
				facts.addProperty("item", tested(BuiltInRegistries.ITEM.getKey(block.asItem())) || block.asItem() != net.minecraft.world.item.Items.AIR);
				level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
				level.setBlock(pos, state, 2);
				BlockState placed = level.getBlockState(pos);
				facts.addProperty("placed", placed.getBlock() == block);
				BlockEntity be = level.getBlockEntity(pos);
				facts.addProperty("blockEntity", be == null ? null : String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType())));
				facts.addProperty("collision", !placed.getCollisionShape(level, pos).isEmpty());
				Set<String> drops = new TreeSet<>();
				for (int i = 0; i < 4; i++) {
					for (ItemStack drop : Block.getDrops(placed, level, pos, be, null, ItemStack.EMPTY)) {
						drops.add(String.valueOf(BuiltInRegistries.ITEM.getKey(drop.getItem())));
					}
				}
				facts.add("drops", array(drops));
				MenuProvider provider = placed.getMenuProvider(level, pos);
				if (provider != null) {
					try {
						AbstractContainerMenu menu = provider.createMenu(0, player.getInventory(), player);
						facts.addProperty("menuSlots", menu == null ? -1 : menu.slots.size());
					} catch (RuntimeException e) {
						facts.addProperty("menuSlots", -2);
					}
				}
			} catch (RuntimeException e) {
				facts.addProperty("error", e.toString());
			} finally {
				try {
					level.removeBlockEntity(pos);
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
				} catch (RuntimeException ignored) {
					// next block
				}
			}
			blocks.add(entry.getKey(), facts);
		}
		LOGGER.info("Recorded {} blocks", blocks.size());
		return blocks;
	}

	private static JsonObject items() {
		JsonObject items = new JsonObject();
		Map<String, Item> sorted = new TreeMap<>();
		for (Item item : BuiltInRegistries.ITEM) {
			ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
			if (tested(id)) {
				sorted.put(id.toString(), item);
			}
		}
		for (Map.Entry<String, Item> entry : sorted.entrySet()) {
			JsonObject facts = new JsonObject();
			try {
				ItemStack stack = new ItemStack(entry.getValue());
				facts.addProperty("stack", stack.getMaxStackSize());
				facts.addProperty("durability", stack.getMaxDamage());
				FoodProperties food = entry.getValue().getFoodProperties(stack, null);
				if (food != null) {
					facts.addProperty("nutrition", food.getNutrition());
				}
				if (entry.getValue() instanceof BlockItem blockItem) {
					facts.addProperty("block", String.valueOf(BuiltInRegistries.BLOCK.getKey(blockItem.getBlock())));
				}
			} catch (RuntimeException e) {
				facts.addProperty("error", e.toString());
			}
			items.add(entry.getKey(), facts);
		}
		LOGGER.info("Recorded {} items", items.size());
		return items;
	}

	private static JsonObject entities(ServerLevel level) {
		JsonObject entities = new JsonObject();
		Map<String, EntityType<?>> sorted = new TreeMap<>();
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
			if (tested(id)) {
				sorted.put(id.toString(), type);
			}
		}
		for (Map.Entry<String, EntityType<?>> entry : sorted.entrySet()) {
			EntityType<?> type = entry.getValue();
			JsonObject facts = new JsonObject();
			facts.addProperty("category", type.getCategory().getName());
			facts.addProperty("width", round(type.getWidth()));
			facts.addProperty("height", round(type.getHeight()));
			try {
				Entity entity = type.create(level);
				facts.addProperty("created", entity != null);
				if (entity instanceof LivingEntity living) {
					facts.addProperty("health", round(living.getMaxHealth()));
				}
				if (entity != null) {
					entity.discard();
				}
			} catch (RuntimeException e) {
				facts.addProperty("error", e.toString());
			}
			entities.add(entry.getKey(), facts);
		}
		LOGGER.info("Recorded {} entities", entities.size());
		return entities;
	}

	private static JsonObject recipes(MinecraftServer server) {
		JsonObject recipes = new JsonObject();
		Map<String, JsonObject> sorted = new TreeMap<>();
		for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
			if (!tested(holder.id())) {
				continue;
			}
			JsonObject facts = new JsonObject();
			facts.addProperty("type", String.valueOf(BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType())));
			try {
				ItemStack result = holder.value().getResultItem(server.registryAccess());
				facts.addProperty("result", result.isEmpty() ? null : String.valueOf(BuiltInRegistries.ITEM.getKey(result.getItem())));
				facts.addProperty("count", result.getCount());
			} catch (RuntimeException e) {
				facts.addProperty("error", e.toString());
			}
			sorted.put(holder.id().toString(), facts);
		}
		sorted.forEach(recipes::add);
		LOGGER.info("Recorded {} recipes", recipes.size());
		return recipes;
	}

	private static JsonArray array(Set<String> values) {
		JsonArray array = new JsonArray();
		values.forEach(array::add);
		return array;
	}

	private static double round(float value) {
		return Math.round(value * 100.0D) / 100.0D;
	}
}
