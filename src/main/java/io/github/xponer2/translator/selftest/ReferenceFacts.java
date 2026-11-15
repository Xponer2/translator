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
package io.github.xponer2.translator.selftest;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * The facts of the 1.21.1 mods' blocks, items, entities and recipes as they are on 1.12.2, in the form of the reference
 * recorded in the real NeoForge 1.21.1 game (D:/translator-reference, its Recorder): tools/compare_reference.py diffs
 * the two, so every block that drops, opens or holds something else than in 1.21.1 shows without a test of its own.
 * {@code -Dtranslator.facts=<file>}: written once the server has started.
 */
public final class ReferenceFacts {
	private static final Logger LOGGER = LogManager.getLogger("Translator Self-Test");
	private static final Set<String> SKIPPED = new java.util.HashSet<>(java.util.Arrays.asList("minecraft", "forge", "translator", "fml", "mixinbooter", "c"));
	private static boolean written;

	private ReferenceFacts() {
	}

	/** At the server's start: written when the property names a file. */
	public static synchronized void recordIfAsked(net.minecraft.server.MinecraftServer server) {
		String out = System.getProperty("translator.facts");
		if (out == null || written || server == null) {
			return;
		}
		written = true;
		WorldServer world = server.getWorld(0);
		JsonObject root = new JsonObject();
		root.addProperty("minecraft", "1.12.2");
		try {
			root.add("blocks", blocks(world));
			root.add("items", items());
			root.add("entities", entities(world));
			root.add("recipes", recipes());
		} catch (RuntimeException | LinkageError e) {
			LOGGER.error("Recording the facts stopped", e);
		}
		try (Writer writer = Files.newBufferedWriter(new File(out).toPath(), StandardCharsets.UTF_8)) {
			new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root, writer);
			LOGGER.info("Facts written to {}", out);
		} catch (IOException e) {
			LOGGER.error("Couldn't write the facts", e);
		}
	}

	private static boolean tested(ResourceLocation id) {
		return id != null && !SKIPPED.contains(id.getNamespace());
	}

	private static String id(Item item) {
		var name = io.github.xponer2.translator.util.ItemIds.id(new ItemStack(item));
		return String.valueOf(name);
	}

	private static JsonObject blocks(WorldServer world) {
		JsonObject blocks = new JsonObject();
		BlockPos pos = new BlockPos(0, 120, 0);
		world.getChunk(pos);
		FakePlayer player = FakePlayerFactory.getMinecraft(world);
		Map<String, Block> sorted = new TreeMap<>();
		for (Block block : ForgeRegistries.BLOCKS) {
			if (tested(block.getRegistryName())) {
				sorted.put(block.getRegistryName().toString(), block);
			}
		}
		for (Map.Entry<String, Block> entry : sorted.entrySet()) {
			Block block = entry.getValue();
			JsonObject facts = new JsonObject();
			try {
				IBlockState state = block.getDefaultState();
				world.setBlockState(pos.down(), Blocks.STONE.getDefaultState(), 2);
				world.setBlockState(pos, state, 2);
				IBlockState placed = world.getBlockState(pos);
				facts.addProperty("hardness", round(placed.getBlockHardness(world, pos)));
				facts.addProperty("blast", round(block.getExplosionResistance(null)));
				facts.addProperty("light", placed.getLightValue(world, pos));
				// 1.21 asItem: a wall sign or wall torch gives its standing one's item
				facts.addProperty("item", io.github.xponer2.translator.util.ItemLike.itemOfBlock(block) != net.minecraft.init.Items.AIR);
				facts.addProperty("placed", placed.getBlock() == block);
				TileEntity tile = world.getTileEntity(pos);
				ResourceLocation tileId = tile == null ? null : TileEntity.getKey(tile.getClass());
				facts.addProperty("blockEntity", tile == null ? null : String.valueOf(tileId));
				facts.addProperty("collision", placed.getCollisionBoundingBox(world, pos) != null);
				Set<String> drops = new TreeSet<>();
				for (int i = 0; i < 4; i++) {
					NonNullList<ItemStack> list = NonNullList.create();
					block.getDrops(list, world, pos, placed, 0);
					// what a harvest gives: the 1.21 table of a mod block on a 1.12.2 class comes through the harvest event
					NonNullList<ItemStack> table = io.github.xponer2.translator.loot.HarvestLoot.drops(block, world, pos, placed, 0, null, world.rand);
					if (table != null) {
						list = table;
					}
					for (ItemStack drop : list) {
						if (!drop.isEmpty()) {
							drops.add(String.valueOf(io.github.xponer2.translator.util.ItemIds.id(drop)));
						}
					}
				}
				facts.add("drops", array(drops));
				var provider = io.github.xponer2.translator.runtime.redirect.ApiBridges.getMenuProvider(placed, world, pos);
				if (provider != null) {
					try {
						Container menu = provider.createMenu(0, player.inventory, player);
						facts.addProperty("menuSlots", menu == null ? -1 : menu.inventorySlots.size());
					} catch (RuntimeException | LinkageError e) {
						facts.addProperty("menuSlots", -2);
					}
				}
			} catch (RuntimeException | LinkageError e) {
				facts.addProperty("error", e.toString());
			} finally {
				try {
					world.removeTileEntity(pos);
					world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2 | 16);
				} catch (RuntimeException ignored) {
					// next block
				}
			}
			blocks.add(entry.getKey(), facts);
		}
		return blocks;
	}

	private static JsonObject items() {
		JsonObject items = new JsonObject();
		Map<String, Item> sorted = new TreeMap<>();
		for (Item item : ForgeRegistries.ITEMS) {
			if (tested(item.getRegistryName())) {
				sorted.put(item.getRegistryName().toString(), item);
			}
		}
		for (Map.Entry<String, Item> entry : sorted.entrySet()) {
			JsonObject facts = new JsonObject();
			try {
				ItemStack stack = new ItemStack(entry.getValue());
				facts.addProperty("stack", stack.getMaxStackSize());
				facts.addProperty("durability", stack.getMaxDamage());
				var properties = io.github.xponer2.translator.item.ItemProperties.of(entry.getValue());
				var food = properties == null ? null : properties.getFood();
				if (food != null) {
					facts.addProperty("nutrition", food.nutrition());
				} else if (entry.getValue() instanceof net.minecraft.item.ItemFood vanillaFood) {
					facts.addProperty("nutrition", vanillaFood.getHealAmount(stack));
				}
				if (entry.getValue() instanceof ItemBlock blockItem) {
					facts.addProperty("block", String.valueOf(blockItem.getBlock().getRegistryName()));
				}
			} catch (RuntimeException | LinkageError e) {
				facts.addProperty("error", e.toString());
			}
			items.add(entry.getKey(), facts);
		}
		return items;
	}

	private static JsonObject entities(WorldServer world) {
		JsonObject entities = new JsonObject();
		Map<String, EntityEntry> sorted = new TreeMap<>();
		for (EntityEntry entry : ForgeRegistries.ENTITIES) {
			if (tested(entry.getRegistryName())) {
				sorted.put(entry.getRegistryName().toString(), entry);
			}
		}
		for (Map.Entry<String, EntityEntry> entry : sorted.entrySet()) {
			JsonObject facts = new JsonObject();
			try {
				Entity entity = EntityList.newEntity(entry.getValue().getEntityClass(), world);
				facts.addProperty("created", entity != null);
				if (entity != null) {
					facts.addProperty("width", round(entity.width));
					facts.addProperty("height", round(entity.height));
					if (entity instanceof EntityLivingBase living) {
						facts.addProperty("health", round(living.getMaxHealth()));
					}
					entity.setDead();
				}
			} catch (RuntimeException | LinkageError e) {
				facts.addProperty("error", e.toString());
			}
			entities.add(entry.getKey(), facts);
		}
		return entities;
	}

	/** The 1.21.1 recipes of the mods as the Translator's recipe manager holds them (result item and count). */
	private static JsonObject recipes() {
		JsonObject recipes = new JsonObject();
		Map<String, JsonObject> sorted = new TreeMap<>();
		for (var holder : io.github.xponer2.translator.recipe.RecipeManager.get().getRecipes()) {
			if (!tested(holder.id())) {
				continue;
			}
			JsonObject facts = new JsonObject();
			try {
				Object recipe = holder.value();
				java.lang.reflect.Method result = null;
				for (java.lang.reflect.Method method : recipe.getClass().getMethods()) {
					if ("getResultItem".equals(method.getName()) && method.getParameterCount() == 1) {
						result = method;
						break;
					}
				}
				ItemStack stack = result == null ? ItemStack.EMPTY : (ItemStack) result.invoke(recipe, (Object) null);
				facts.addProperty("result", stack == null || stack.isEmpty() ? null : String.valueOf(io.github.xponer2.translator.util.ItemIds.id(stack)));
				facts.addProperty("count", stack == null ? 0 : stack.getCount());
			} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
				facts.addProperty("error", e.toString());
			}
			sorted.put(holder.id().toString(), facts);
		}
		sorted.forEach(recipes::add);
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
