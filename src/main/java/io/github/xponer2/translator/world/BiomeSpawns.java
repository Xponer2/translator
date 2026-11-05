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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.xponer2.translator.registry.EntityType;
import io.github.xponer2.translator.runtime.RuntimeMods;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * The natural spawns of the mods' mobs: NeoForge biome modifiers ({@code data/<mod>/neoforge/biome_modifier/*.json},
 * {@code neoforge:add_spawns}) on 1.12.2's spawn lists, their biomes by id or by biome tag (the mods' tags, the
 * vanilla ones by their BiomeDictionary type: is_overworld, is_forest, is_ocean, ...). The mods' mobs never spawned
 * in the world before.
 */
public final class BiomeSpawns {
	private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("Translator Runtime");
	private static final Pattern MODIFIER = Pattern.compile("[a-z0-9_.-]+/(neoforge|forge)/biome_modifier/[a-z0-9_./-]+\\.json");
	private static final Pattern TAG = Pattern.compile("([a-z0-9_.-]+)/tags/worldgen/biome/([a-z0-9_./-]+)\\.json");

	private final List<JsonObject> modifiers = new ArrayList<>();
	private final Map<ResourceLocation, List<String>> tags = new HashMap<>();

	private BiomeSpawns() {
	}

	private static boolean done;

	/** Once, after the registries (the mods' entity types and biomes are there). */
	public static synchronized void apply() {
		if (done) {
			return;
		}
		done = true;
		try {
			var spawns = new BiomeSpawns();
			spawns.scan();
			int added = 0;
			for (JsonObject modifier : spawns.modifiers) {
				added += spawns.add(modifier);
			}
			LOGGER.info("Spawns of the 1.21.1 mods' mobs: {} added from {} biome modifiers", added, spawns.modifiers.size());
		} catch (RuntimeException | LinkageError e) {
			LOGGER.error("Couldn't add the spawns of the 1.21.1 mods", e);
		}
	}

	private void scan() {
		for (ModContainer mod : Loader.instance().getActiveModList()) {
			if (!RuntimeMods.isRuntimeNamespace(mod.getModId())) {
				continue;
			}
			CraftingHelper.findFiles(mod, "data", null, (root, file) -> {
				String path = root.relativize(file).toString().replace('\\', '/');
				boolean modifier = MODIFIER.matcher(path).matches();
				var tag = TAG.matcher(path);
				if (!modifier && !tag.matches()) {
					return true;
				}
				try {
					JsonElement json = new JsonParser().parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
					if (modifier && json.isJsonObject()) {
						this.modifiers.add(json.getAsJsonObject());
					} else if (tag.matches() && json.isJsonObject() && json.getAsJsonObject().has("values")) {
						var values = this.tags.computeIfAbsent(new ResourceLocation(tag.group(1), tag.group(2)), k -> new ArrayList<>());
						for (JsonElement value : json.getAsJsonObject().getAsJsonArray("values")) {
							values.add(value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString());
						}
					}
				} catch (IOException | RuntimeException e) {
					LOGGER.warn("Couldn't read {} of {}: {}", path, mod.getModId(), e.toString());
				}
				return true;
			}, true, true);
		}
	}

	private int add(JsonObject modifier) {
		String type = modifier.has("type") ? modifier.get("type").getAsString() : "";
		if (!type.endsWith(":add_spawns")) {
			return 0;
		}
		Set<Biome> biomes = this.biomes(modifier.get("biomes"));
		if (biomes.isEmpty()) {
			return 0;
		}
		var spawners = new ArrayList<JsonObject>();
		JsonElement element = modifier.get("spawners");
		if (element != null && element.isJsonArray()) {
			element.getAsJsonArray().forEach(e -> spawners.add(e.getAsJsonObject()));
		} else if (element != null && element.isJsonObject()) {
			spawners.add(element.getAsJsonObject());
		}
		int added = 0;
		for (JsonObject spawner : spawners) {
			var id = new ResourceLocation(spawner.get("type").getAsString());
			EntityType<?> entity = io.github.xponer2.translator.util.BuiltInRegistries.ENTITY_TYPE.get(id);
			if (entity == null || !EntityLiving.class.isAssignableFrom(entity.getEntityClass())) {
				continue;
			}
			EnumCreatureType category = entity.getCategory() != null ? entity.getCategory() : EnumCreatureType.CREATURE;
			int weight = spawner.has("weight") ? spawner.get("weight").getAsInt() : 1;
			int min = spawner.has("minCount") ? spawner.get("minCount").getAsInt() : 1;
			int max = spawner.has("maxCount") ? spawner.get("maxCount").getAsInt() : min;
			@SuppressWarnings("unchecked")
			Class<? extends EntityLiving> type2 = (Class<? extends EntityLiving>) entity.getEntityClass();
			EntityRegistry.addSpawn(type2, weight, min, max, category, biomes.toArray(new Biome[0]));
			added++;
		}
		return added;
	}

	/** {@code "biomes"}: an id, a #tag, or a list of them. */
	private Set<Biome> biomes(JsonElement selector) {
		var out = new LinkedHashSet<Biome>();
		if (selector == null) {
			return out;
		}
		if (selector.isJsonArray()) {
			for (JsonElement element : selector.getAsJsonArray()) {
				out.addAll(this.biomes(element));
			}
		} else if (selector.isJsonPrimitive()) {
			String value = selector.getAsString();
			if (value.startsWith("#")) {
				out.addAll(this.tag(new ResourceLocation(value.substring(1)), new HashSet<>()));
			} else {
				Biome biome = biome(new ResourceLocation(value));
				if (biome != null) {
					out.add(biome);
				}
			}
		}
		return out;
	}

	private Set<Biome> tag(ResourceLocation tag, Set<ResourceLocation> seen) {
		var out = new LinkedHashSet<Biome>();
		if (!seen.add(tag)) {
			return out;
		}
		List<String> values = this.tags.get(tag);
		if (values != null) {
			for (String value : values) {
				if (value.startsWith("#")) {
					out.addAll(this.tag(new ResourceLocation(value.substring(1)), seen));
				} else {
					Biome biome = biome(new ResourceLocation(value));
					if (biome != null) {
						out.add(biome);
					}
				}
			}
		}
		// the vanilla and common tags 1.12.2 has no data for: by their BiomeDictionary type (and the mods' biomes in them)
		if ("minecraft".equals(tag.getNamespace()) || "c".equals(tag.getNamespace()) || "forge".equals(tag.getNamespace())) {
			out.addAll(dictionary(tag.getPath()));
		}
		return out;
	}

	private static Set<Biome> dictionary(String path) {
		var out = new LinkedHashSet<Biome>();
		String name = path.startsWith("is_") ? path.substring(3) : path;
		if ("overworld".equals(name)) {
			for (Biome biome : ForgeRegistries.BIOMES) {
				if (!BiomeDictionary.hasType(biome, BiomeDictionary.Type.NETHER) && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.END)
						&& !BiomeDictionary.hasType(biome, BiomeDictionary.Type.VOID)) {
					out.add(biome);
				}
			}
			return out;
		}
		String type = switch (name) {
			case "deep_ocean", "ocean" -> "OCEAN";
			case "taiga" -> "CONIFEROUS";
			case "badlands" -> "MESA";
			case "hill" -> "HILLS";
			case "desert" -> "SANDY";
			case "snowy", "icy" -> "SNOWY";
			case "mushroom" -> "MUSHROOM";
			default -> name.toUpperCase(java.util.Locale.ROOT);
		};
		for (BiomeDictionary.Type candidate : BiomeDictionary.Type.getAll()) {
			if (candidate.getName().equals(type)) {
				out.addAll(BiomeDictionary.getBiomes(candidate));
			}
		}
		return out;
	}

	private static Biome biome(ResourceLocation id) {
		Biome biome = ForgeRegistries.BIOMES.getValue(id);
		if (biome == null && "minecraft".equals(id.getNamespace())) {
			// a few 1.21 names of 1.12.2 biomes
			String old = switch (id.getPath()) {
				case "dark_forest" -> "roofed_forest";
				case "badlands" -> "mesa";
				case "snowy_plains" -> "ice_flats";
				case "windswept_hills" -> "extreme_hills";
				case "old_growth_birch_forest" -> "mutated_birch_forest";
				case "snowy_taiga" -> "taiga_cold";
				case "stony_shore" -> "stone_beach";
				case "mushroom_fields" -> "mushroom_island";
				case "nether_wastes" -> "hell";
				case "the_end" -> "sky";
				case "sparse_jungle" -> "jungle_edge";
				case "savanna_plateau" -> "savanna_rock";
				case "wooded_badlands" -> "mesa_rock";
				default -> null;
			};
			biome = old == null ? null : ForgeRegistries.BIOMES.getValue(new ResourceLocation("minecraft", old));
		}
		return biome;
	}
}
