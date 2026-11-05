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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.xponer2.translator.runtime.RuntimeMods;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockNewLeaf;
import net.minecraft.block.BlockNewLog;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * The biomes of the 1.21.1 mods on 1.12.2. In 1.21 a biome is data (data/<mod>/worldgen/biome/<name>.json), placed in
 * the overworld by its noise parameters (TerraBlender regions for Biomes O' Plenty): none of it reached 1.12.2, so the
 * mods' biomes never generated. Here each biome JSON becomes a 1.12.2 biome ({@link DataBiome}) registered under its
 * id (the mods' code finds it by its key), and those in the {@code is_overworld} tags join the 1.12.2 overworld
 * biome lists (BiomeManager) by their temperature, with BiomeDictionary types from their climate and name. Their tree
 * features are read down to the log, leaves, height and shape ({@link DataTree}); the 1.21 feature code doesn't run.
 * Biomes of other dimensions (Twilight Forest, the Aether) are registered for their dimensions only.
 */
public final class DataBiomes {
	private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("Translator Runtime");
	private static final Pattern FILE = Pattern.compile("([a-z0-9_.-]+)/worldgen/(biome|placed_feature|configured_feature)/([a-z0-9_./-]+)\\.json");
	private static final Pattern TAG = Pattern.compile("(minecraft|c)/tags/worldgen/biome/(is_overworld|is_nether|is_end)\\.json");

	private final Map<ResourceLocation, JsonObject> biomes = new LinkedHashMap<>();
	private final Map<ResourceLocation, ModContainer> owners = new HashMap<>();
	private final Map<ResourceLocation, JsonObject> placed = new HashMap<>();
	private final Map<ResourceLocation, JsonObject> configured = new HashMap<>();
	private final Set<ResourceLocation> overworld = new HashSet<>();
	private static final List<Biome> REGISTERED = new ArrayList<>();

	/** The 1.12.2 biomes made from the mods' data. */
	public static List<Biome> registered() {
		return java.util.Collections.unmodifiableList(REGISTERED);
	}

	@SubscribeEvent
	public void onRegister(RegistryEvent.Register<Biome> event) {
		this.scan();
		int added = 0;
		for (var entry : this.biomes.entrySet()) {
			ResourceLocation id = entry.getKey();
			if ("minecraft".equals(id.getNamespace())) {
				// a mod's change to a vanilla biome: the 1.12.2 biome stays
				continue;
			}
			var active = Loader.instance().activeModContainer();
			if (this.owners.containsKey(id)) {
				Loader.instance().setActiveModContainer(this.owners.get(id));
			}
			try {
				var biome = new DataBiome(id, entry.getValue());
				this.decorate(biome);
				biome.setRegistryName(id);
				event.getRegistry().register(biome);
				REGISTERED.add(biome);
				boolean inOverworld = this.overworld.contains(id);
				BiomeDictionary.addTypes(biome, types(biome, inOverworld));
				if (inOverworld) {
					BiomeManager.addBiome(climate(biome), new BiomeManager.BiomeEntry(biome, 10));
					if (!DataBiome.has(id.getPath(), "ocean", "river", "beach", "peak", "mountain", "volcano")) {
						BiomeManager.addSpawnBiome(biome);
					}
					added++;
				}
			} catch (RuntimeException e) {
				LOGGER.error("Couldn't make the biome {} of a 1.21.1 mod", id, e);
			} finally {
				Loader.instance().setActiveModContainer(active);
			}
		}
		LOGGER.info("Biomes of the 1.21.1 mods: {} registered, {} in the overworld", REGISTERED.size(), added);
	}

	/** The mods' biome, feature and tag files. */
	private void scan() {
		var overworldTags = new ArrayList<JsonObject>();
		// from the mods' jars (FML lists the 1.21.1 mods only after post-init: its active mods had none of them here)
		RuntimeMods.forEachData((path, bytes) -> {
			var m = FILE.matcher(path);
			var t = TAG.matcher(path);
			if (!m.matches() && !(t.matches() && "is_overworld".equals(t.group(2)))) {
				return;
			}
			JsonObject json;
			try {
				json = new JsonParser().parse(new String(bytes.get(), StandardCharsets.UTF_8)).getAsJsonObject();
			} catch (RuntimeException e) {
				LOGGER.warn("Couldn't read {}: {}", path, e.toString());
				return;
			}
			if (!m.matches()) {
				overworldTags.add(json);
				return;
			}
			var id = new ResourceLocation(m.group(1), m.group(3));
			switch (m.group(2)) {
				case "biome" -> {
					this.biomes.put(id, json);
					ModContainer owner = Loader.instance().getIndexedModList().get(id.getNamespace());
					if (owner != null) {
						this.owners.put(id, owner);
					}
				}
				case "placed_feature" -> this.placed.put(id, json);
				default -> this.configured.put(id, json);
			}
		});
		for (JsonObject tag : overworldTags) {
			if (tag.has("values")) {
				for (JsonElement value : tag.getAsJsonArray("values")) {
					String name = value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString();
					if (!name.startsWith("#")) {
						this.overworld.add(new ResourceLocation(name));
					}
				}
			}
		}
	}

	// --- decoration -----------------------------------------------------------------------------------------------------

	/** Grass, flowers, cacti, reeds and trees per chunk from the biome's feature names, the trees from their data. */
	private void decorate(DataBiome biome) {
		var decorator = biome.decorator;
		int grass = 0;
		int flowers = 0;
		double trees = 0;
		boolean anyTree = false;
		for (String feature : biome.features()) {
			String path = feature.substring(feature.indexOf(':') + 1);
			if (path.contains("tree") || path.startsWith("forest_") || path.contains("_forest_") || path.contains("bush")) {
				anyTree = true;
				trees += this.addTrees(biome, new ResourceLocation(feature), path);
			} else if (path.contains("flower")) {
				flowers += 2;
			} else if (path.contains("grass") || path.contains("fern")) {
				grass += 3;
			} else if (path.contains("cactus")) {
				decorator.cactiPerChunk = 5;
			} else if (path.contains("sugar_cane")) {
				decorator.reedsPerChunk = 5;
			} else if (path.contains("mushroom")) {
				decorator.mushroomsPerChunk = 1;
			} else if (path.contains("dead_bush")) {
				decorator.deadBushPerChunk = 2;
			} else if (path.contains("lily_pad") || path.contains("waterlily")) {
				decorator.waterlilyPerChunk = 2;
			}
		}
		decorator.grassPerChunk = Math.min(20, grass);
		decorator.flowersPerChunk = Math.min(10, flowers);
		trees = Math.min(trees, 24);
		decorator.treesPerChunk = anyTree ? (int) trees : -999;
		decorator.extraTreeChance = anyTree ? (float) (trees - Math.floor(trees)) : 0.0F;
	}

	/** The trees of a placed tree feature: how many per chunk, and each tree it picks (with its share). */
	private double addTrees(DataBiome biome, ResourceLocation placedId, String path) {
		JsonObject placedFeature = this.placed.get(placedId);
		if (placedFeature == null) {
			// a vanilla feature (its data is in the 1.21.1 game): a tree like the name says
			biome.addTree(new DataTree(log("minecraft:" + vanillaWood(path) + "_log"), leaves("minecraft:" + vanillaWood(path) + "_leaves"),
					4, 7, shapeOf(path, "")), 1.0);
			return vanillaCount(path);
		}
		double count = count(placedFeature);
		var choices = new ArrayList<Object[]>();
		this.choices(placedFeature.get("feature"), false, 1.0, 0, choices);
		for (Object[] choice : choices) {
			biome.addTree((DataTree) choice[0], (Double) choice[1]);
		}
		return choices.isEmpty() ? 0 : count;
	}

	/**
	 * The trees a feature makes (random selectors followed down), each with its probability. {@code placed}: the
	 * reference is to a placed feature (the entries of a selector), else to a configured one (a placed feature's own).
	 */
	private void choices(@Nullable JsonElement feature, boolean placed, double weight, int depth, List<Object[]> out) {
		if (feature == null || depth > 8 || weight <= 0) {
			return;
		}
		JsonObject json = feature.isJsonObject() ? feature.getAsJsonObject() : null;
		if (feature.isJsonPrimitive()) {
			var id = new ResourceLocation(feature.getAsString());
			json = placed ? this.placed.get(id) : this.configured.get(id);
			if (json == null && placed) {
				json = this.configured.get(id);
			}
		}
		if (json != null && json.has("feature") && !json.has("type")) {
			// a placed feature: its configured feature
			this.choices(json.get("feature"), false, weight, depth + 1, out);
			return;
		}
		if (json == null || !json.has("config")) {
			return;
		}
		String type = json.get("type").getAsString();
		JsonObject config = json.getAsJsonObject("config");
		if ("minecraft:random_selector".equals(type)) {
			double rest = weight;
			for (JsonElement element : config.getAsJsonArray("features")) {
				double chance = element.getAsJsonObject().get("chance").getAsDouble();
				this.choices(element.getAsJsonObject().get("feature"), true, rest * chance, depth + 1, out);
				rest *= 1 - chance;
			}
			this.choices(config.get("default"), true, rest, depth + 1, out);
		} else if ("minecraft:simple_random_selector".equals(type)) {
			var features = config.getAsJsonArray("features");
			for (JsonElement element : features) {
				this.choices(element, true, weight / features.size(), depth + 1, out);
			}
		} else if (config.has("trunk_provider") && config.has("foliage_provider")) {
			int[] height = heights(config);
			out.add(new Object[] {new DataTree(withProperties(log(stateName(config.get("trunk_provider"))), config.get("trunk_provider")),
					withProperties(leaves(stateName(config.get("foliage_provider"))), config.get("foliage_provider")),
					height[0], height[1], shapeOf(type, config.has("foliage_placer") ? config.getAsJsonObject("foliage_placer").get("type").getAsString() : "")),
					weight});
		}
	}

	private static int[] heights(JsonObject config) {
		if (config.has("min_height") && config.has("max_height")) {
			return new int[] {config.get("min_height").getAsInt(), config.get("max_height").getAsInt()};
		}
		if (config.has("trunk_placer")) {
			var trunk = config.getAsJsonObject("trunk_placer");
			int base = trunk.has("base_height") ? trunk.get("base_height").getAsInt() : 4;
			int a = trunk.has("height_rand_a") ? trunk.get("height_rand_a").getAsInt() : 2;
			int b = trunk.has("height_rand_b") ? trunk.get("height_rand_b").getAsInt() : 0;
			return new int[] {base, base + a + b};
		}
		return new int[] {4, 7};
	}

	private static DataTree.Shape shapeOf(String type, String foliage) {
		String text = type + " " + foliage;
		if (DataBiome.has(text, "taiga", "redwood", "pine", "spruce", "fir", "cypress", "conifer", "mega_jungle")) {
			return DataTree.Shape.CONE;
		}
		if (DataBiome.has(text, "bush")) {
			return DataTree.Shape.BUSH;
		}
		if (DataBiome.has(text, "twiglet", "dead", "dying")) {
			return DataTree.Shape.TWIGLET;
		}
		if (DataBiome.has(text, "big", "fancy", "mega", "large")) {
			return DataTree.Shape.BIG;
		}
		return DataTree.Shape.ROUND;
	}

	/** Features per chunk of a placed feature (its count modifiers; 1 when it has none). */
	private static double count(JsonObject placedFeature) {
		double count = 1;
		if (!placedFeature.has("placement")) {
			return count;
		}
		for (JsonElement element : placedFeature.getAsJsonArray("placement")) {
			JsonObject modifier = element.getAsJsonObject();
			String type = modifier.get("type").getAsString();
			if ("minecraft:count".equals(type)) {
				count *= average(modifier.get("count"));
			} else if ("minecraft:rarity_filter".equals(type)) {
				count /= Math.max(1, modifier.get("chance").getAsInt());
			} else if ("minecraft:noise_threshold_count".equals(type)) {
				count *= (modifier.get("below_noise").getAsInt() + modifier.get("above_noise").getAsInt()) / 2.0;
			}
		}
		return count;
	}

	/** The mean of an int provider (a number, constant, uniform, weighted list, clamped...). */
	private static double average(JsonElement provider) {
		if (provider.isJsonPrimitive()) {
			return provider.getAsDouble();
		}
		JsonObject object = provider.getAsJsonObject();
		String type = object.has("type") ? object.get("type").getAsString() : "";
		switch (type) {
			case "minecraft:constant":
				return object.get("value").getAsDouble();
			case "minecraft:uniform", "minecraft:biased_to_bottom":
				return (object.get("min_inclusive").getAsDouble() + object.get("max_inclusive").getAsDouble()) / 2;
			case "minecraft:weighted_list": {
				double sum = 0;
				double weights = 0;
				for (JsonElement entry : object.getAsJsonArray("distribution")) {
					double weight = entry.getAsJsonObject().get("weight").getAsDouble();
					sum += average(entry.getAsJsonObject().get("data")) * weight;
					weights += weight;
				}
				return weights > 0 ? sum / weights : 0;
			}
			case "minecraft:clamped":
				return average(object.get("source"));
			default:
				return 1;
		}
	}

	private static double vanillaCount(String path) {
		if (DataBiome.has(path, "jungle")) {
			return 20;
		}
		if (DataBiome.has(path, "plains", "savanna", "sparse", "meadow")) {
			return 0.5;
		}
		if (DataBiome.has(path, "taiga", "birch", "forest", "dark_oak", "old_growth", "grove")) {
			return 8;
		}
		return 3;
	}

	private static String vanillaWood(String path) {
		for (String wood : new String[] {"spruce", "birch", "jungle", "acacia", "dark_oak"}) {
			if (path.contains(wood)) {
				return wood;
			}
		}
		if (DataBiome.has(path, "taiga", "snowy", "pine", "grove")) {
			return "spruce";
		}
		if (path.contains("savanna")) {
			return "acacia";
		}
		return "oak";
	}

	// --- blocks ---------------------------------------------------------------------------------------------------------

	/** The block name of a 1.21 block state provider (simple, rotated, weighted: its first entry). */
	/** The provider's state properties ({@code "Properties": {"axis": "y"}}) on the state, those its block has. */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static IBlockState withProperties(IBlockState state, JsonElement provider) {
		try {
			JsonObject object = provider.getAsJsonObject();
			JsonObject data = object.has("state") ? object.getAsJsonObject("state")
					: object.has("entries") ? object.getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonObject("data") : null;
			if (data == null || !data.has("Properties") || !data.getAsJsonObject("Properties").isJsonObject()) {
				return state;
			}
			for (var entry : data.getAsJsonObject("Properties").entrySet()) {
				for (IProperty property : state.getPropertyKeys()) {
					if (property.getName().equals(entry.getKey())) {
						var value = property.parseValue(entry.getValue().getAsString());
						if (value.isPresent()) {
							state = state.withProperty(property, (Comparable) value.get());
						}
					}
				}
			}
		} catch (RuntimeException e) {
			// the state as it is
		}
		return state;
	}

	private static String stateName(JsonElement provider) {
		JsonObject object = provider.getAsJsonObject();
		if (object.has("state")) {
			return object.getAsJsonObject("state").get("Name").getAsString();
		}
		if (object.has("entries")) {
			var first = object.getAsJsonArray("entries").get(0).getAsJsonObject();
			return first.getAsJsonObject("data").get("Name").getAsString();
		}
		return "minecraft:oak_log";
	}

	static IBlockState log(String name) {
		BlockPlanks.EnumType wood = vanillaType(name);
		if (wood != null) {
			return wood.getMetadata() < 4 ? Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, wood)
					: Blocks.LOG2.getDefaultState().withProperty(BlockNewLog.VARIANT, wood);
		}
		IBlockState state = modState(name);
		return state != null ? state : Blocks.LOG.getDefaultState();
	}

	static IBlockState leaves(String name) {
		BlockPlanks.EnumType wood = vanillaType(name);
		if (wood != null) {
			IBlockState state = wood.getMetadata() < 4 ? Blocks.LEAVES.getDefaultState().withProperty(BlockOldLeaf.VARIANT, wood)
					: Blocks.LEAVES2.getDefaultState().withProperty(BlockNewLeaf.VARIANT, wood);
			return state.withProperty(BlockLeaves.CHECK_DECAY, false);
		}
		IBlockState state = modState(name);
		if (state == null) {
			return Blocks.LEAVES.getDefaultState().withProperty(BlockLeaves.CHECK_DECAY, false);
		}
		// a 1.21 mod's leaves decay at distance 7 with nothing telling them their log is near: they stay (as placed ones)
		for (IProperty<?> property : state.getPropertyKeys()) {
			if ("persistent".equals(property.getName()) && property.getValueClass() == Boolean.class) {
				state = with(state, property, true);
			}
		}
		return state;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> IBlockState with(IBlockState state, IProperty<?> property, Object value) {
		return state.withProperty((IProperty<T>) property, (T) value);
	}

	@Nullable
	private static BlockPlanks.EnumType vanillaType(String name) {
		if (!name.startsWith("minecraft:")) {
			return null;
		}
		String path = name.substring("minecraft:".length());
		for (BlockPlanks.EnumType type : BlockPlanks.EnumType.values()) {
			if (path.startsWith(type.getName() + "_")) {
				return type;
			}
		}
		// the 1.21 woods 1.12.2 lacks: the nearest look
		if (DataBiome.has(path, "cherry", "azalea", "mangrove", "pale_oak")) {
			return path.contains("mangrove") ? BlockPlanks.EnumType.JUNGLE : BlockPlanks.EnumType.OAK;
		}
		return null;
	}

	/** The state of a 1.21 block name (a mod's, or a vanilla one 1.12.2 has): stone when there is none. */
	static IBlockState stateOf(String name) {
		IBlockState state = modState(name);
		return state != null ? state : Blocks.STONE.getDefaultState();
	}

	/** A mod's block (or a vanilla one 1.12.2 has under that name): its default state. */
	@Nullable
	private static IBlockState modState(String name) {
		Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name));
		if (block != null && block != Blocks.AIR) {
			return block.getDefaultState();
		}
		if (name.startsWith("minecraft:")) {
			try {
				var field = io.github.xponer2.translator.runtime.redirect.FlattenedBlocks.class.getField(
						name.substring("minecraft:".length()).toUpperCase(java.util.Locale.ROOT));
				if (field.get(null) instanceof Block flattened && flattened != Blocks.AIR) {
					return flattened.getDefaultState();
				}
			} catch (ReflectiveOperationException | RuntimeException e) {
				// not a 1.21 vanilla block 1.12.2 knows
			}
		}
		return null;
	}

	// --- where it generates ----------------------------------------------------------------------------------------------

	private static BiomeManager.BiomeType climate(Biome biome) {
		float temperature = biome.getDefaultTemperature();
		if (biome.isSnowyBiome() || temperature < 0.2F) {
			return BiomeManager.BiomeType.ICY;
		}
		if (temperature > 1.0F) {
			return BiomeManager.BiomeType.DESERT;
		}
		return temperature > 0.6F ? BiomeManager.BiomeType.WARM : BiomeManager.BiomeType.COOL;
	}

	private static BiomeDictionary.Type[] types(DataBiome biome, boolean overworld) {
		var types = new java.util.LinkedHashSet<BiomeDictionary.Type>();
		String path = biome.id().getPath();
		float temperature = biome.getDefaultTemperature();
		if (temperature > 1.0F) {
			types.add(BiomeDictionary.Type.HOT);
		} else if (temperature < 0.3F) {
			types.add(BiomeDictionary.Type.COLD);
		}
		if (biome.isSnowyBiome()) {
			types.add(BiomeDictionary.Type.SNOWY);
		}
		if (biome.getRainfall() > 0.85F) {
			types.add(BiomeDictionary.Type.WET);
		} else if (biome.getRainfall() < 0.15F) {
			types.add(BiomeDictionary.Type.DRY);
		}
		if (biome.decorator.treesPerChunk >= 5) {
			types.add(BiomeDictionary.Type.FOREST);
		} else if (biome.decorator.treesPerChunk <= 1) {
			types.add(BiomeDictionary.Type.PLAINS);
		}
		String[][] words = {{"ocean", "OCEAN"}, {"sea", "OCEAN"}, {"river", "RIVER"}, {"beach", "BEACH"}, {"swamp", "SWAMP"}, {"marsh", "SWAMP"},
				{"bog", "SWAMP"}, {"bayou", "SWAMP"}, {"mountain", "MOUNTAIN"}, {"peak", "MOUNTAIN"}, {"crag", "MOUNTAIN"}, {"hill", "HILLS"},
				{"highland", "HILLS"}, {"jungle", "JUNGLE"}, {"rainforest", "JUNGLE"}, {"tropic", "JUNGLE"}, {"savanna", "SAVANNA"},
				{"desert", "SANDY"}, {"dune", "SANDY"}, {"conifer", "CONIFEROUS"}, {"fir", "CONIFEROUS"}, {"redwood", "CONIFEROUS"},
				{"taiga", "CONIFEROUS"}, {"dead", "DEAD"}, {"wasteland", "WASTELAND"}, {"mystic", "MAGICAL"}, {"magic", "MAGICAL"},
				{"auroral", "MAGICAL"}, {"ominous", "SPOOKY"}, {"spider", "SPOOKY"}, {"lush", "LUSH"}, {"garden", "LUSH"}, {"mushroom", "MUSHROOM"},
				{"fungal", "MUSHROOM"}};
		for (String[] word : words) {
			if (path.contains(word[0])) {
				types.add(BiomeDictionary.Type.getType(word[1]));
			}
		}
		if (!overworld) {
			types.add(BiomeDictionary.Type.getType("TRANSLATOR_OTHER_DIMENSION"));
		}
		return types.toArray(new BiomeDictionary.Type[0]);
	}
}
