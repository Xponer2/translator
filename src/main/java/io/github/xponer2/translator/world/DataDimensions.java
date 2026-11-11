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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.xponer2.translator.runtime.RuntimeMods;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DimensionType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * The dimensions of the 1.21.1 mods on 1.12.2 (data/<mod>/dimension/<name>.json with its dimension_type): each gets a
 * 1.12.2 DimensionType named by its id (the 1.21 level key finds it by that name, so a mod's portal reaches it) and a
 * {@link DataWorldProvider}: its biomes ({@link DataBiomeProvider}), sky light, fixed time, beds, and terrain from its
 * generator ({@link DataChunkGenerator}: overworld-like terrain of its biomes, floating islands for sky dimensions, an
 * empty world for flat void ones). The 1.21 generator code of the mod (Twilight Forest's) doesn't run.
 */
public final class DataDimensions {
	private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("Translator Runtime");
	private static final Pattern FILE = Pattern.compile("([a-z0-9_.-]+)/(dimension|dimension_type|worldgen/noise_settings)/([a-z0-9_./-]+)\\.json");
	private static final Map<Integer, Data> BY_ID = new HashMap<>();

	/** What a dimension of a mod is, read from its data. */
	static final class Data {
		enum Terrain {
			OVERWORLD, ISLANDS, VOID
		}

		final ResourceLocation id;
		final Terrain terrain;
		final List<Biome> biomes;
		@Nullable
		final IBlockState stone;
		final boolean skylight;
		final boolean ceiling;
		final boolean ultrawarm;
		final boolean beds;
		@Nullable
		final Long fixedTime;
		/** The type's {@code effects}: the client's sky, clouds and fog of the dimension (its DimensionSpecialEffects). */
		final ResourceLocation effects;

		Data(ResourceLocation id, Terrain terrain, List<Biome> biomes, @Nullable IBlockState stone, JsonObject type) {
			this.id = id;
			this.terrain = terrain;
			this.biomes = biomes;
			this.stone = stone;
			this.skylight = !type.has("has_skylight") || type.get("has_skylight").getAsBoolean();
			this.ceiling = type.has("has_ceiling") && type.get("has_ceiling").getAsBoolean();
			this.ultrawarm = type.has("ultrawarm") && type.get("ultrawarm").getAsBoolean();
			this.beds = !type.has("bed_works") || type.get("bed_works").getAsBoolean();
			this.fixedTime = type.has("fixed_time") ? type.get("fixed_time").getAsLong() : null;
			this.effects = new ResourceLocation(type.has("effects") ? type.get("effects").getAsString() : "minecraft:overworld");
		}
	}

	private DataDimensions() {
	}

	@Nullable
	static Data data(int dimension) {
		return BY_ID.get(dimension);
	}

	/** The effects of a mod's dimension (its type's {@code effects}), null for the others. */
	@Nullable
	public static ResourceLocation effects(int dimension) {
		Data data = BY_ID.get(dimension);
		return data != null ? data.effects : null;
	}

	/** Registers the mods' dimensions (init: the biomes are registered by then). */
	public static void registerAll() {
		var dimensions = new LinkedHashMap<ResourceLocation, JsonObject>();
		var types = new HashMap<ResourceLocation, JsonObject>();
		var noise = new HashMap<ResourceLocation, JsonObject>();
		// from the mods' jars (FML lists the 1.21.1 mods only after post-init: none of their dimensions was found here)
		RuntimeMods.forEachData((path, bytes) -> {
			var m = FILE.matcher(path);
			if (!m.matches()) {
				return;
			}
			try {
				var json = new JsonParser().parse(new String(bytes.get(), StandardCharsets.UTF_8)).getAsJsonObject();
				var id = new ResourceLocation(m.group(1), m.group(3));
				switch (m.group(2)) {
					case "dimension" -> dimensions.put(id, json);
					case "dimension_type" -> types.put(id, json);
					default -> noise.put(id, json);
				}
			} catch (RuntimeException e) {
				LOGGER.warn("Couldn't read {}: {}", path, e.toString());
			}
		});
		for (var entry : dimensions.entrySet()) {
			ResourceLocation id = entry.getKey();
			if ("minecraft".equals(id.getNamespace())) {
				continue;
			}
			try {
				JsonObject json = entry.getValue();
				JsonObject type = resolve(json.get("type"), types);
				JsonObject generator = json.has("generator") ? json.getAsJsonObject("generator") : new JsonObject();
				JsonObject settings = generator.has("settings") ? resolve(generator.get("settings"), noise) : new JsonObject();
				String settingsName = generator.has("settings") && generator.get("settings").isJsonPrimitive() ? generator.get("settings").getAsString() : "";
				String generatorType = generator.has("type") ? generator.get("type").getAsString() : "minecraft:noise";
				Data.Terrain terrain = "minecraft:flat".equals(generatorType) || "minecraft:debug".equals(generatorType) ? Data.Terrain.VOID
						: DataBiome.has(settingsName, "sky", "island", "float", "aether") ? Data.Terrain.ISLANDS : Data.Terrain.OVERWORLD;
				IBlockState stone = settings.has("default_block") ? DataBiomes.stateOf(settings.getAsJsonObject("default_block").get("Name").getAsString()) : null;
				List<Biome> biomes = biomes(id, generator);
				surface(id.getNamespace(), biomes);
				int dimension = DimensionManager.getNextFreeDimId();
				DimensionType dimensionType = DimensionType.register(id.toString(), "_" + id.getNamespace() + "_" + id.getPath().replace('/', '_'), dimension,
						DataWorldProvider.class, false);
				BY_ID.put(dimension, new Data(id, terrain, biomes, stone == Blocks.STONE.getDefaultState() ? null : stone, type));
				DimensionManager.registerDimension(dimension, dimensionType);
				LOGGER.info("Dimension {} of a 1.21.1 mod: 1.12.2 dimension {} ({} terrain, {} biomes)", id, dimension, terrain, biomes.size());
			} catch (RuntimeException e) {
				LOGGER.error("Couldn't make the dimension {} of a 1.21.1 mod", id, e);
			}
		}
	}

	private static JsonObject resolve(@Nullable JsonElement element, Map<ResourceLocation, JsonObject> files) {
		if (element == null) {
			return new JsonObject();
		}
		if (element.isJsonObject()) {
			return element.getAsJsonObject();
		}
		JsonObject found = files.get(new ResourceLocation(element.getAsString()));
		return found != null ? found : new JsonObject();
	}

	/** The biomes of the dimension's biome source; a mod's own source type: the mod's biomes outside the overworld. */
	private static List<Biome> biomes(ResourceLocation id, JsonObject generator) {
		var names = new ArrayList<String>();
		JsonObject source = generator.has("biome_source") ? generator.getAsJsonObject("biome_source") : null;
		if (source != null && source.has("biome") && source.get("biome").isJsonPrimitive()) {
			names.add(source.get("biome").getAsString());
		}
		if (source != null && source.has("biomes") && source.get("biomes").isJsonArray()) {
			for (JsonElement element : source.getAsJsonArray("biomes")) {
				if (element.isJsonObject() && element.getAsJsonObject().has("biome")) {
					names.add(element.getAsJsonObject().get("biome").getAsString());
				} else if (element.isJsonPrimitive()) {
					names.add(element.getAsString());
				}
			}
		}
		if (generator.has("settings") && generator.get("settings").isJsonObject() && generator.getAsJsonObject("settings").has("biome")) {
			names.add(generator.getAsJsonObject("settings").get("biome").getAsString());
		}
		var biomes = new ArrayList<Biome>();
		for (String name : names) {
			Biome biome = ForgeRegistries.BIOMES.getValue(new ResourceLocation(name));
			if (biome != null && !biomes.contains(biome)) {
				biomes.add(biome);
			}
		}
		if (biomes.isEmpty()) {
			for (Biome biome : DataBiomes.registered()) {
				var name = biome.getRegistryName();
				if (name != null && name.getNamespace().equals(id.getNamespace()) && !isOverworld(biome)) {
					biomes.add(biome);
				}
			}
		}
		return biomes;
	}

	private static boolean isOverworld(Biome biome) {
		return !net.minecraftforge.common.BiomeDictionary.hasType(biome, net.minecraftforge.common.BiomeDictionary.Type.getType("TRANSLATOR_OTHER_DIMENSION"));
	}

	/** Grass and dirt of the dimension's mod for its biomes when it has them (the Aether's aether grass, aether dirt). */
	private static void surface(String namespace, List<Biome> biomes) {
		Block grass = null;
		Block dirt = null;
		for (Block block : ForgeRegistries.BLOCKS) {
			var name = block.getRegistryName();
			if (name == null || !name.getNamespace().equals(namespace)) {
				continue;
			}
			if (grass == null && name.getPath().endsWith("grass_block")) {
				grass = block;
			} else if (dirt == null && (name.getPath().endsWith("_dirt") || name.getPath().equals("dirt")) && !name.getPath().contains("path")) {
				dirt = block;
			}
		}
		for (Biome biome : biomes) {
			if (biome instanceof DataBiome && grass != null && biome.topBlock.getBlock() == Blocks.GRASS) {
				biome.topBlock = grass.getDefaultState();
			}
			if (biome instanceof DataBiome && dirt != null && biome.fillerBlock.getBlock() == Blocks.DIRT) {
				biome.fillerBlock = dirt.getDefaultState();
			}
		}
	}
}
