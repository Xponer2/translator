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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A biome of a 1.21.1 mod (its data/<mod>/worldgen/biome JSON) on 1.12.2: temperature, rain or snow, the grass, leaf,
 * sky and water colours, the mobs that spawn in it, a height from the kind of biome its name says, grass and flower
 * density and the trees its features name ({@link DataBiomes} reads those).
 */
final class DataBiome extends Biome {
	private final ResourceLocation id;
	private final JsonObject json;
	@Nullable
	private final Integer grass;
	@Nullable
	private final Integer foliage;
	@Nullable
	private final Integer sky;
	private final List<WorldGenAbstractTree> trees = new ArrayList<>();
	private final List<Double> treeWeights = new ArrayList<>();
	private volatile boolean spawnsRead;

	DataBiome(ResourceLocation id, JsonObject json) {
		super(properties(id, json));
		this.id = id;
		this.json = json;
		JsonObject effects = json.has("effects") ? json.getAsJsonObject("effects") : new JsonObject();
		this.grass = color(effects, "grass_color");
		this.foliage = color(effects, "foliage_color");
		this.sky = color(effects, "sky_color");
		String path = id.getPath();
		if (has(path, "desert", "dune", "beach", "sand")) {
			this.topBlock = Blocks.SAND.getDefaultState();
			this.fillerBlock = Blocks.SAND.getDefaultState();
		}
		if (has(path, "gravel")) {
			this.topBlock = Blocks.GRAVEL.getDefaultState();
			this.fillerBlock = Blocks.GRAVEL.getDefaultState();
		}
		if (has(path, "crag", "cliff", "peak", "volcan", "rocky_")) {
			this.topBlock = Blocks.STONE.getDefaultState();
		}
		if (has(path, "wasteland", "dryland", "badland", "scrub")) {
			this.topBlock = Blocks.DIRT.getStateFromMeta(1);
		}
	}

	/**
	 * 1.12.2's decoration (the trees from the data, the vanilla plants of vanilla features), then the mod's own placed
	 * features by the 1.21.1 code ({@link DataFeatures#decorate}: its flowers, grass, mushrooms, rocks).
	 */
	@Override
	public void decorate(net.minecraft.world.World world, Random random, BlockPos pos) {
		super.decorate(world, random, pos);
		DataFeatures.decorate(this, world, random, pos);
	}

	/** The tree features of the biome, with how many of each grow in a chunk (DataBiomes reads them). */
	/** The trees of its data (DataTree, or 1.12.2's for vanilla ones). */
	List<WorldGenAbstractTree> trees() {
		return java.util.Collections.unmodifiableList(this.trees);
	}

	void addTree(WorldGenAbstractTree tree, double weight) {
		this.trees.add(tree);
		this.treeWeights.add(weight);
	}

	@Override
	public WorldGenAbstractTree getRandomTreeFeature(Random random) {
		if (this.trees.isEmpty()) {
			return super.getRandomTreeFeature(random);
		}
		double total = 0;
		for (double weight : this.treeWeights) {
			total += weight;
		}
		double pick = random.nextDouble() * total;
		for (int i = 0; i < this.trees.size(); i++) {
			pick -= this.treeWeights.get(i);
			if (pick <= 0) {
				return this.trees.get(i);
			}
		}
		return this.trees.get(this.trees.size() - 1);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getGrassColorAtPos(BlockPos pos) {
		return this.grass != null ? this.getModdedBiomeGrassColor(this.grass) : super.getGrassColorAtPos(pos);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getFoliageColorAtPos(BlockPos pos) {
		return this.foliage != null ? this.getModdedBiomeFoliageColor(this.foliage) : super.getFoliageColorAtPos(pos);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getSkyColorByTemp(float temperature) {
		return this.sky != null ? this.sky : super.getSkyColorByTemp(temperature);
	}

	/** The mobs of the JSON, read the first time they're asked for (the entity registry comes after the biomes). */
	@Override
	public List<SpawnListEntry> getSpawnableList(EnumCreatureType type) {
		if (!this.spawnsRead) {
			this.readSpawns();
		}
		return super.getSpawnableList(type);
	}

	private synchronized void readSpawns() {
		if (this.spawnsRead) {
			return;
		}
		this.spawnableMonsterList.clear();
		this.spawnableCreatureList.clear();
		this.spawnableWaterCreatureList.clear();
		this.spawnableCaveCreatureList.clear();
		JsonObject spawners = this.json.has("spawners") ? this.json.getAsJsonObject("spawners") : new JsonObject();
		for (var category : spawners.entrySet()) {
			List<SpawnListEntry> list = switch (category.getKey()) {
				case "monster" -> this.spawnableMonsterList;
				case "creature" -> this.spawnableCreatureList;
				case "ambient" -> this.spawnableCaveCreatureList;
				case "water_creature", "water_ambient" -> this.spawnableWaterCreatureList;
				default -> null;
			};
			if (list == null || !category.getValue().isJsonArray()) {
				continue;
			}
			for (JsonElement element : category.getValue().getAsJsonArray()) {
				JsonObject entry = element.getAsJsonObject();
				Class<? extends EntityLiving> type = living(entry.get("type").getAsString());
				if (type != null) {
					list.add(new SpawnListEntry(type, entry.get("weight").getAsInt(), entry.get("minCount").getAsInt(), entry.get("maxCount").getAsInt()));
				}
			}
		}
		this.spawnsRead = true;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private static Class<? extends EntityLiving> living(String name) {
		var id = new ResourceLocation(name);
		Class<? extends Entity> type = EntityList.getClass(id);
		if (type == null) {
			var entry = ForgeRegistries.ENTITIES.getValue(id);
			type = entry != null ? entry.getEntityClass() : null;
		}
		return type != null && EntityLiving.class.isAssignableFrom(type) ? (Class<? extends EntityLiving>) type : null;
	}

	// --- properties ---------------------------------------------------------------------------------------------------

	private static BiomeProperties properties(ResourceLocation id, JsonObject json) {
		float temperature = json.has("temperature") ? json.get("temperature").getAsFloat() : 0.5F;
		if (temperature > 0.1F && temperature < 0.2F) {
			// 1.12.2 refuses temperatures between 0.1 and 0.2 (snow or rain is ambiguous there)
			temperature = 0.2F;
		}
		float downfall = json.has("downfall") ? Math.max(0.0F, Math.min(1.0F, json.get("downfall").getAsFloat())) : 0.5F;
		var properties = new BiomeProperties(title(id.getPath())).setTemperature(temperature).setRainfall(downfall);
		boolean precipitation = !json.has("has_precipitation") || json.get("has_precipitation").getAsBoolean();
		boolean frozen = json.has("temperature_modifier") && "frozen".equals(json.get("temperature_modifier").getAsString());
		if (!precipitation) {
			properties.setRainDisabled();
		} else if (temperature <= 0.1F || frozen) {
			properties.setSnowEnabled();
		}
		float[] height = height(id.getPath());
		properties.setBaseHeight(height[0]).setHeightVariation(height[1]);
		JsonObject effects = json.has("effects") ? json.getAsJsonObject("effects") : null;
		Integer water = effects != null ? color(effects, "water_color") : null;
		if (water != null && water != 0x3F76E4) {
			properties.setWaterColor(waterTint(water));
		}
		return properties;
	}

	/** Base height and variation of the 1.12.2 terrain, from the kind of biome its name says. */
	private static float[] height(String path) {
		if (has(path, "deep_ocean", "abyss")) {
			return new float[] {-1.8F, 0.1F};
		}
		if (has(path, "ocean", "sea", "reef")) {
			return new float[] {-1.0F, 0.1F};
		}
		if (has(path, "river")) {
			return new float[] {-0.5F, 0.0F};
		}
		if (has(path, "beach", "shore", "dune")) {
			return new float[] {0.0F, 0.025F};
		}
		if (has(path, "swamp", "marsh", "bog", "bayou", "wetland", "fen", "mire", "muskeg", "floodplain")) {
			return new float[] {-0.2F, 0.1F};
		}
		if (has(path, "mountain", "peak", "highland", "crag", "cliff", "alps", "volcano", "summit")) {
			return new float[] {1.0F, 0.5F};
		}
		if (has(path, "hill", "rocky", "plateau", "upland", "moor")) {
			return new float[] {0.45F, 0.3F};
		}
		if (has(path, "plain", "field", "meadow", "prairie", "pasture", "grassland", "steppe", "savanna", "shrubland", "scrubland", "tundra",
				"desert", "wasteland", "garden", "patch", "orchard")) {
			return new float[] {0.125F, 0.05F};
		}
		return new float[] {0.1F, 0.2F};
	}

	/** The 1.12.2 water colour multiplies its blue texture: the tint that gives the 1.21 colour from the default blue. */
	private static int waterTint(int color) {
		int base = 0x3F76E4;
		int tint = 0;
		for (int shift = 16; shift >= 0; shift -= 8) {
			int want = color >> shift & 0xFF;
			int have = base >> shift & 0xFF;
			tint |= Math.min(255, have == 0 ? 255 : want * 255 / have) << shift;
		}
		return tint;
	}

	@Nullable
	private static Integer color(JsonObject object, String key) {
		return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsInt() : null;
	}

	static boolean has(String path, String... words) {
		for (String word : words) {
			if (path.contains(word)) {
				return true;
			}
		}
		return false;
	}

	private static String title(String path) {
		var out = new StringBuilder();
		for (String word : path.substring(path.lastIndexOf('/') + 1).split("_")) {
			if (!word.isEmpty()) {
				out.append(out.length() > 0 ? " " : "").append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
			}
		}
		return out.toString();
	}

	ResourceLocation id() {
		return this.id;
	}

	/** The feature ids of the biome (all generation steps). */
	List<String> features() {
		var out = new ArrayList<String>();
		if (this.json.has("features") && this.json.get("features").isJsonArray()) {
			for (JsonElement step : this.json.getAsJsonArray("features")) {
				if (step.isJsonArray()) {
					for (JsonElement feature : (JsonArray) step) {
						if (feature.isJsonPrimitive()) {
							out.add(feature.getAsString());
						}
					}
				} else if (step.isJsonPrimitive()) {
					out.add(step.getAsString());
				}
			}
		}
		return out;
	}
}
