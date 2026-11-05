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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.xponer2.translator.runtime.RuntimeMods;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * What a 1.21.1 mod is besides its blocks and items, each checked against its own data: its recipes (in the crafting
 * table, the furnace), its tags, its creative tab, its sounds, its advancements, the drops of its blocks, and what its
 * biomes generate.
 */
final class ModChecks {
	private static final Pattern RECIPE = Pattern.compile("([a-z0-9_.-]+)/recipes?/([a-z0-9_./-]+)\\.json");
	private static final Pattern TAG = Pattern.compile("([a-z0-9_.-]+)/tags/(blocks?|items?)/([a-z0-9_./-]+)\\.json");
	private static final Pattern ADVANCEMENT = Pattern.compile("([a-z0-9_.-]+)/advancements?/([a-z0-9_./-]+)\\.json");
	private static final Pattern BLOCK_LOOT = Pattern.compile("([a-z0-9_.-]+)/loot_tables?/blocks/([a-z0-9_./-]+)\\.json");

	private ModChecks() {
	}

	@Nullable
	private static JsonObject json(java.util.function.Supplier<byte[]> bytes) {
		try {
			JsonElement element = new JsonParser().parse(new String(bytes.get(), StandardCharsets.UTF_8));
			return element.isJsonObject() ? element.getAsJsonObject() : null;
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static void list(List<String> problems, String what, List<String> ids) {
		if (!ids.isEmpty()) {
			problems.add(what + " (" + ids.size() + "): " + String.join(", ", ids.subList(0, Math.min(8, ids.size()))) + (ids.size() > 8 ? ", ..." : ""));
		}
	}

	/**
	 * The mod's recipe files against what 1.12.2 has: crafting ones in the recipe registry under their id (with a
	 * result and ingredients that match something), smelting ones in the furnace; the kinds 1.12.2 has no block for.
	 */
	static void recipes(String namespace, List<String> problems, List<String> notes) {
		var missing = new ArrayList<String>();
		var broken = new ArrayList<String>();
		var smeltingMissing = new ArrayList<String>();
		var other = new TreeMap<String, Integer>();
		int crafting = 0;
		int smelting = 0;
		var files = new LinkedHashMap<ResourceLocation, JsonObject>();
		RuntimeMods.forEachData((path, bytes) -> {
			var m = RECIPE.matcher(path);
			if (m.matches() && namespace.equals(m.group(1))) {
				JsonObject json = json(bytes);
				if (json != null) {
					files.put(new ResourceLocation(m.group(1), m.group(2)), json);
				}
			}
		});
		for (var entry : files.entrySet()) {
			JsonObject json = entry.getValue();
			String type = json.has("type") ? json.get("type").getAsString() : "";
			boolean conditional = json.has("neoforge:conditions") || json.has("conditions");
			if (type.startsWith("minecraft:crafting")) {
				crafting++;
				IRecipe recipe = ForgeRegistries.RECIPES.getValue(entry.getKey());
				if (recipe == null) {
					if (!conditional) {
						missing.add(entry.getKey().getPath());
					}
					continue;
				}
				boolean bad = recipe.getRecipeOutput().isEmpty() && !type.contains("special");
				for (Ingredient ingredient : recipe.getIngredients()) {
					bad |= ingredient != Ingredient.EMPTY && ingredient.getMatchingStacks().length == 0;
				}
				if (bad) {
					broken.add(entry.getKey().getPath());
				}
			} else if ("minecraft:smelting".equals(type)) {
				smelting++;
				Item input = itemOf(json.get("ingredient"));
				Item result = itemOf(json.get("result"));
				if (input != null && result != null) {
					ItemStack out = FurnaceRecipes.instance().getSmeltingResult(new ItemStack(input));
					if (out.isEmpty() || out.getItem() != result) {
						smeltingMissing.add(entry.getKey().getPath());
					}
				}
			} else {
				other.merge(type, 1, Integer::sum);
			}
		}
		list(problems, "recipes: not in the crafting table", missing);
		list(problems, "recipes: a result or an ingredient is empty", broken);
		list(problems, "recipes: not in the furnace", smeltingMissing);
		notes.add(crafting + " crafting and " + smelting + " smelting recipes" + (other.isEmpty() ? "" : "; kinds 1.12.2 has no block for: " + other));
	}

	@Nullable
	private static Item itemOf(@Nullable JsonElement element) {
		if (element == null) {
			return null;
		}
		if (element.isJsonArray() && element.getAsJsonArray().size() > 0) {
			return itemOf(element.getAsJsonArray().get(0));
		}
		if (element.isJsonPrimitive()) {
			return ForgeRegistries.ITEMS.getValue(new ResourceLocation(element.getAsString()));
		}
		JsonObject object = element.getAsJsonObject();
		String name = object.has("item") ? object.get("item").getAsString() : object.has("id") ? object.get("id").getAsString() : null;
		return name == null ? null : ForgeRegistries.ITEMS.getValue(new ResourceLocation(name));
	}

	/** Every block and item of the mod a tag file lists is in that tag (the mod's tags and its entries in others'). */
	static void tags(String namespace, List<String> problems, List<String> notes) {
		var missing = new ArrayList<String>();
		int[] checked = new int[1];
		RuntimeMods.forEachData((path, bytes) -> {
			var m = TAG.matcher(path);
			if (!m.matches()) {
				return;
			}
			JsonObject json = json(bytes);
			if (json == null || !json.has("values") || !json.get("values").isJsonArray()) {
				return;
			}
			boolean blocks = m.group(2).startsWith("block");
			var tag = new ResourceLocation(m.group(1), m.group(3));
			for (JsonElement value : json.getAsJsonArray("values")) {
				String name = value.isJsonObject() ? (value.getAsJsonObject().has("id") ? value.getAsJsonObject().get("id").getAsString() : "")
						: value.getAsString();
				if (name.startsWith("#") || !name.startsWith(namespace + ":")) {
					continue;
				}
				var id = new ResourceLocation(name);
				try {
					if (blocks) {
						Block block = ForgeRegistries.BLOCKS.getValue(id);
						if (block == null || block == Blocks.AIR) {
							continue;
						}
						checked[0]++;
						if (!io.github.xponer2.translator.tags.TagManager.is(block, io.github.xponer2.translator.util.TagKey.create(
								io.github.xponer2.translator.util.Registries.BLOCK, tag))) {
							missing.add(name + " in #" + tag);
						}
					} else {
						Item item = ForgeRegistries.ITEMS.getValue(id);
						if (item == null) {
							continue;
						}
						checked[0]++;
						if (!io.github.xponer2.translator.tags.TagManager.is(item, io.github.xponer2.translator.util.TagKey.create(
								io.github.xponer2.translator.util.Registries.ITEM, tag))) {
							missing.add(name + " in #" + tag);
						}
					}
				} catch (RuntimeException e) {
					missing.add(name + " in #" + tag + " (" + e + ")");
				}
			}
		});
		list(problems, "tags: an entry isn't in its tag", missing);
		notes.add(checked[0] + " tag entries of the mod");
	}

	/** On the client: the creative tabs show the mod's items (some a mod keeps out of them: a note). */
	static void creativeTabs(String namespace, List<String> problems, List<String> notes) {
		if (!FMLCommonHandler.instance().getSide().isClient()) {
			return;
		}
		Set<Item> shown = new HashSet<>();
		for (net.minecraft.creativetab.CreativeTabs tab : net.minecraft.creativetab.CreativeTabs.CREATIVE_TAB_ARRAY) {
			if (tab == null || tab == net.minecraft.creativetab.CreativeTabs.SEARCH) {
				continue;
			}
			var stacks = net.minecraft.util.NonNullList.<ItemStack>create();
			try {
				tab.displayAllRelevantItems(stacks);
			} catch (RuntimeException e) {
				problems.add("creative tab " + tab.getTabLabel() + ": " + e);
				continue;
			}
			for (ItemStack stack : stacks) {
				shown.add(stack.getItem());
			}
		}
		var hidden = new ArrayList<String>();
		int all = 0;
		for (Item item : ForgeRegistries.ITEMS) {
			if (item.getRegistryName() != null && namespace.equals(item.getRegistryName().getNamespace())) {
				all++;
				if (!shown.contains(item)) {
					hidden.add(item.getRegistryName().getPath());
				}
			}
		}
		if (all > 0 && hidden.size() == all) {
			problems.add("creative tabs: none of the mod's " + all + " items is in a tab");
		} else if (!hidden.isEmpty()) {
			notes.add(hidden.size() + " of " + all + " items in no tab: " + String.join(", ", hidden.subList(0, Math.min(10, hidden.size()))));
		} else {
			notes.add("all " + all + " items in the creative tabs");
		}
	}

	/** On the client: each sound event of the mod has its sounds (sounds.json read). */
	static void sounds(String namespace, List<String> problems, List<String> notes) {
		if (!FMLCommonHandler.instance().getSide().isClient()) {
			return;
		}
		var silent = new ArrayList<String>();
		int all = 0;
		var handler = net.minecraft.client.Minecraft.getMinecraft().getSoundHandler();
		for (net.minecraft.util.SoundEvent sound : ForgeRegistries.SOUND_EVENTS) {
			ResourceLocation id = sound.getRegistryName();
			if (id != null && namespace.equals(id.getNamespace())) {
				all++;
				if (handler.getAccessor(sound.getSoundName()) == null) {
					silent.add(id.getPath());
				}
			}
		}
		list(problems, "sounds: no sounds.json entry", silent);
		notes.add(all + " sound events");
	}

	/** The mod's advancement files against the server's advancements. */
	static void advancements(net.minecraft.server.MinecraftServer server, String namespace, List<String> problems, List<String> notes) {
		int[] files = new int[1];
		RuntimeMods.forEachData((path, bytes) -> {
			var m = ADVANCEMENT.matcher(path);
			if (m.matches() && namespace.equals(m.group(1))) {
				files[0]++;
			}
		});
		int loaded = 0;
		for (net.minecraft.advancements.Advancement advancement : server.getAdvancementManager().getAdvancements()) {
			if (namespace.equals(advancement.getId().getNamespace())) {
				loaded++;
			}
		}
		if (files[0] > 0 && loaded == 0) {
			problems.add("advancements: none of the mod's " + files[0] + " loaded");
		} else {
			notes.add(loaded + " of " + files[0] + " advancements loaded");
		}
	}

	/**
	 * The blocks whose loot table drops an item plainly (no tool, chance or condition but surviving an explosion): one
	 * broken must drop something.
	 */
	static Set<ResourceLocation> plainDrops(String namespace) {
		var plain = new HashSet<ResourceLocation>();
		RuntimeMods.forEachData((path, bytes) -> {
			var m = BLOCK_LOOT.matcher(path);
			if (!m.matches() || !namespace.equals(m.group(1))) {
				return;
			}
			JsonObject json = json(bytes);
			if (json == null || !json.has("pools") || json.getAsJsonArray("pools").size() == 0) {
				return;
			}
			JsonObject pool = json.getAsJsonArray("pools").get(0).getAsJsonObject();
			if (!plainConditions(pool) || !pool.has("entries") || pool.getAsJsonArray("entries").size() != 1) {
				return;
			}
			JsonObject entry = pool.getAsJsonArray("entries").get(0).getAsJsonObject();
			if ("minecraft:item".equals(entry.has("type") ? entry.get("type").getAsString() : "") && plainConditions(entry)
					&& !entry.has("functions")) {
				plain.add(new ResourceLocation(m.group(1), m.group(2)));
			}
		});
		return plain;
	}

	private static boolean plainConditions(JsonObject object) {
		if (!object.has("conditions")) {
			return true;
		}
		for (JsonElement condition : object.getAsJsonArray("conditions")) {
			String name = condition.getAsJsonObject().has("condition") ? condition.getAsJsonObject().get("condition").getAsString() : "";
			if (!"minecraft:survives_explosion".equals(name)) {
				return false;
			}
		}
		return true;
	}

	/** A trunk or branch: a log of 1.12.2 or of a mod (a pillar of wood). */
	static boolean isLog(net.minecraft.block.state.IBlockState state) {
		Block block = state.getBlock();
		return block instanceof net.minecraft.block.BlockLog
				|| block instanceof io.github.xponer2.translator.runtime.mc.RotatedPillarBlock && state.getMaterial() == net.minecraft.block.material.Material.WOOD;
	}

	private static boolean isLeaves(net.minecraft.block.state.IBlockState state) {
		return state.getMaterial() == net.minecraft.block.material.Material.LEAVES;
	}

	/**
	 * The tree whose trunk starts at {@code base}: an upright trunk (axis y), leaves (each joined to a log through
	 * leaves within 6, else it would fall), and the log and leaves expected ({log, leaves}, when given). Returns its log.
	 */
	@Nullable
	static Block checkTree(net.minecraft.world.World world, BlockPos base, @Nullable Block[] expected, List<String> problems, List<String> notes,
			String what) {
		net.minecraft.block.state.IBlockState bottom = world.getBlockState(base);
		if (!isLog(bottom)) {
			problems.add(what + ": no trunk (" + bottom.getBlock().getRegistryName() + ")");
			return null;
		}
		Block log = bottom.getBlock();
		int height = 0;
		int lying = 0;
		for (BlockPos pos = base; isLog(world.getBlockState(pos)) && height < 64; pos = pos.up()) {
			net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
			for (var property : state.getPropertyKeys()) {
				if ("axis".equals(property.getName()) && !"y".equals(String.valueOf(state.getValue(property)))) {
					lying++;
				}
			}
			height++;
		}
		if (lying > 0) {
			problems.add(what + ": " + lying + " of " + height + " trunk logs lie on their side");
		}
		// the tree's logs and leaves (joined to the trunk), the leaves' distance to a log through leaves
		var distance = new java.util.HashMap<BlockPos, Integer>();
		var logs = new java.util.ArrayDeque<BlockPos>();
		var seen = new HashSet<BlockPos>();
		logs.add(base);
		seen.add(base);
		var leaves = new ArrayList<BlockPos>();
		Block leafBlock = null;
		while (!logs.isEmpty() && seen.size() < 6000) {
			BlockPos pos = logs.poll();
			for (net.minecraft.util.EnumFacing facing : net.minecraft.util.EnumFacing.values()) {
				BlockPos side = pos.offset(facing);
				if (Math.abs(side.getX() - base.getX()) > 10 || Math.abs(side.getZ() - base.getZ()) > 10 || side.getY() < base.getY()
						|| !seen.add(side)) {
					continue;
				}
				net.minecraft.block.state.IBlockState state = world.getBlockState(side);
				if (isLog(state)) {
					logs.add(side);
				} else if (isLeaves(state)) {
					leaves.add(side);
					if (leafBlock == null) {
						leafBlock = state.getBlock();
					}
				}
			}
		}
		// leaves reached from the logs: breadth first through leaves
		var queue = new java.util.ArrayDeque<BlockPos>();
		for (BlockPos leaf : leaves) {
			distance.put(leaf, 1);
			queue.add(leaf);
		}
		var all = new HashSet<BlockPos>(leaves);
		while (!queue.isEmpty()) {
			BlockPos pos = queue.poll();
			int next = distance.get(pos) + 1;
			if (next > 7) {
				continue;
			}
			for (net.minecraft.util.EnumFacing facing : net.minecraft.util.EnumFacing.values()) {
				BlockPos side = pos.offset(facing);
				if (!distance.containsKey(side) && Math.abs(side.getX() - base.getX()) <= 10 && Math.abs(side.getZ() - base.getZ()) <= 10
						&& side.getY() >= base.getY() && isLeaves(world.getBlockState(side))) {
					distance.put(side, next);
					all.add(side);
					queue.add(side);
				}
			}
		}
		int far = 0;
		for (BlockPos leaf : all) {
			if (distance.get(leaf) > 6) {
				far++;
			}
		}
		if (all.isEmpty()) {
			problems.add(what + ": no leaves on " + log.getRegistryName());
		} else if (far * 10 > all.size()) {
			problems.add(what + ": " + far + " of " + all.size() + " leaves too far from a log (they fall)");
		}
		if (expected != null) {
			if (log != expected[0]) {
				problems.add(what + ": trunk of " + log.getRegistryName() + ", its data has " + expected[0].getRegistryName());
			}
			if (leafBlock != null && leafBlock != expected[1]) {
				problems.add(what + ": leaves " + leafBlock.getRegistryName() + ", its data has " + expected[1].getRegistryName());
			}
		}
		notes.add(what + ": " + log.getRegistryName() + " " + height + " high, " + all.size() + " leaves");
		return log;
	}

	/**
	 * What a biome puts on its ground: on a patch of the flat test world far away (its top and filler blocks, the
	 * biome set in the chunks), the biome decorates its chunk as world generation does (trees, plants, the mod's own
	 * features), three times; the blocks that appear are counted. Nothing at all while its data has features: a problem.
	 */
	static void decorate(WorldServer world, Biome biome, int index, boolean hasFeatures, List<String> problems, List<String> notes) {
		int chunkX = 4000 + index * 4;
		int chunkZ = 4000;
		byte id = (byte) (Biome.getIdForBiome(biome) & 255);
		for (int dx = 0; dx < 3; dx++) {
			for (int dz = 0; dz < 3; dz++) {
				Chunk chunk = world.getChunk(chunkX + dx, chunkZ + dz);
				java.util.Arrays.fill(chunk.getBiomeArray(), id);
			}
		}
		int base = 3;
		BlockPos corner = new BlockPos(chunkX * 16, 0, chunkZ * 16);
		for (BlockPos pos : BlockPos.getAllInBoxMutable(corner, corner.add(47, 0, 47))) {
			world.setBlockState(pos.up(base), biome.topBlock, 2 | 16);
			world.setBlockState(pos.up(base - 1), biome.fillerBlock, 2 | 16);
			world.setBlockState(pos.up(base - 2), biome.fillerBlock, 2 | 16);
		}
		var counts = new TreeMap<String, Integer>();
		Random random = new Random(Biome.getIdForBiome(biome) * 31L);
		try {
			for (int i = 0; i < 3; i++) {
				biome.decorate(world, random, corner.add(8, 0, 8));
			}
		} catch (Throwable t) {
			problems.add("worldgen: decorating failed: " + SelfTest.describe(t));
		}
		int placed = 0;
		var bases = new ArrayList<BlockPos>();
		for (BlockPos pos : BlockPos.getAllInBoxMutable(corner.add(8, base + 1, 8), corner.add(39, 120, 39))) {
			net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
			Block block = state.getBlock();
			if (block != Blocks.AIR) {
				placed++;
				ResourceLocation name = block.getRegistryName();
				counts.merge(name == null ? "?" : name.toString(), 1, Integer::sum);
				if (isLog(state) && !isLog(world.getBlockState(pos.down())) && bases.size() < 12) {
					bases.add(pos.toImmutable());
				}
			}
		}
		// the trees: each checked (upright trunk, leaves joined to it), and against the trees of the biome's data
		var grownLogs = new HashSet<Block>();
		var treeProblems = new TreeMap<String, Integer>();
		for (BlockPos treeBase : bases) {
			var found = new ArrayList<String>();
			Block log = checkTree(world, treeBase, null, found, new ArrayList<>(), "tree");
			if (log != null) {
				grownLogs.add(log);
			}
			for (String problem : found) {
				treeProblems.merge(problem, 1, Integer::sum);
			}
		}
		treeProblems.forEach((problem, times) -> problems.add("worldgen: " + problem + (times > 1 ? " (" + times + " trees)" : "")));
		var expected = io.github.xponer2.translator.world.TreeInfo.trees(biome);
		if (!expected.isEmpty()) {
			var expectedLogs = new java.util.LinkedHashSet<String>();
			boolean any = false;
			for (Block[] tree : expected) {
				expectedLogs.add(String.valueOf(tree[0].getRegistryName()));
				any |= grownLogs.contains(tree[0]);
			}
			if (bases.isEmpty() && biome.decorator.treesPerChunk >= 1) {
				problems.add("worldgen: no tree grew (its data has " + expectedLogs + ")");
			} else if (!any && !bases.isEmpty()) {
				problems.add("worldgen: trees of other logs grew, not its own " + expectedLogs);
			} else if (!bases.isEmpty()) {
				notes.add("worldgen: " + bases.size() + " trees checked, of its data's " + expectedLogs);
			}
		}
		if (placed == 0 && hasFeatures) {
			problems.add("worldgen: nothing grows on its ground (its data has features)");
		} else {
			var top = new ArrayList<Map.Entry<String, Integer>>(counts.entrySet());
			top.sort((a, b) -> b.getValue() - a.getValue());
			var shown = new ArrayList<String>();
			for (var entry : top.subList(0, Math.min(6, top.size()))) {
				shown.add(entry.getKey().replace("minecraft:", "") + " " + entry.getValue());
			}
			notes.add("worldgen: " + placed + " blocks grown on 32x32 (" + String.join(", ", shown) + ")");
		}
	}
}
