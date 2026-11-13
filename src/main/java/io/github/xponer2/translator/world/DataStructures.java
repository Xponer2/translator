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

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import io.github.xponer2.translator.registry.RegistryAccess;
import io.github.xponer2.translator.registry.RegistryOps;
import io.github.xponer2.translator.resources.ServerResourceManager;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ResourceLocation;

/**
 * The 1.21 structure data of the mods and of Minecraft 1.21.1 ({@code data/<ns>/worldgen/structure}, {@code structure_set},
 * {@code template_pool}, {@code processor_list}) in the 1.21 registries of those kinds: an entry is read from its file by
 * the carried 1.21.1 codec when it is first asked for (a structure set names its structures, a jigsaw pool its processor
 * lists), as {@link DataFeatures} does for features. {@link StructureGeneration} places them.
 */
public final class DataStructures {
	private static final Logger LOGGER = LogManager.getLogger("Translator Runtime");
	/** Registry -> the carried class whose DIRECT_CODEC reads its files. */
	private static final Map<ResourceLocation, String[]> KINDS = new HashMap<>();
	/**
	 * The carried 1.21 types whose static fields register the vanilla kinds a structure file names by id (structure
	 * types, placements, pool elements, processors, piece types).
	 */
	private static final String[] TYPES = {
			"net.minecraft.world.level.levelgen.structure.StructureType",
			"net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType",
			"net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType",
			"net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType",
			"net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType",
			"net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.RuleBlockEntityModifierType",
			"net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType",
			"net.minecraft.world.level.levelgen.structure.templatesystem.PosRuleTestType",
			"net.minecraft.world.level.levelgen.heightproviders.HeightProviderType",
			"net.minecraft.util.valueproviders.IntProviderType",
			"net.minecraft.util.valueproviders.FloatProviderType",
	};
	private static boolean bootstrapped;
	private static final Set<String> REPORTED = new HashSet<>();
	private static final Map<ResourceLocation, Set<ResourceLocation>> BIOME_TAGS = new HashMap<>();

	static {
		KINDS.put(new ResourceLocation("worldgen/structure"), new String[] {"structure", "net.minecraft.world.level.levelgen.structure.Structure"});
		KINDS.put(new ResourceLocation("worldgen/structure_set"),
				new String[] {"structure_set", "net.minecraft.world.level.levelgen.structure.StructureSet"});
		KINDS.put(new ResourceLocation("worldgen/template_pool"),
				new String[] {"template_pool", "net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool"});
		KINDS.put(new ResourceLocation("worldgen/processor_list"),
				new String[] {"processor_list", "net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType"});
	}

	private DataStructures() {
	}

	/** Whether a registry's entries are structure data files. */
	static boolean isDataRegistry(ResourceLocation registry) {
		return KINDS.containsKey(registry);
	}

	/** The entry of that registry read from its file by the 1.21.1 codec; null when there is none or it doesn't read. */
	@Nullable
	static Object decode(ResourceLocation registry, ResourceLocation id) {
		String[] kind = KINDS.get(registry);
		JsonObject json = kind == null ? null : json(id.getNamespace(), "worldgen/" + kind[0] + "/" + id.getPath() + ".json");
		if (json == null) {
			return null;
		}
		bootstrap();
		try {
			@SuppressWarnings("unchecked")
			Codec<Object> codec = (Codec<Object>) Class.forName(kind[1], true, Launch.classLoader).getField("DIRECT_CODEC").get(null);
			DataResult<Object> result = codec.parse(RegistryOps.create(JsonOps.INSTANCE, RegistryAccess.INSTANCE), json);
			var value = result.result();
			if (!value.isPresent()) {
				report(kind[0] + " " + id, result.error().map(DataResult.PartialResult::message).orElse("no value"));
			}
			return value.orElse(null);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			report(kind[0] + " " + id, where(e));
			return null;
		}
	}

	/** An exception with the first frame of a mod or of the Translator (a bare NullPointerException says nothing). */
	private static boolean own(StackTraceElement frame) {
		String type = frame.getClassName();
		return !type.startsWith("java.") && !type.startsWith("sun.") && !type.startsWith("com.mojang.serialization.")
				&& !type.startsWith("com.mojang.datafixers.");
	}

	public static String where(Throwable e) {
		Throwable cause = e instanceof java.lang.reflect.InvocationTargetException && e.getCause() != null ? e.getCause() : e;
		StackTraceElement[] frames = cause.getStackTrace();
		// where it was thrown, when that is in a library (a codec of DataFixerUpper given a null): the frame itself
		String thrown = frames.length > 0 && !own(frames[0]) ? " in " + frames[0] : "";
		for (StackTraceElement frame : frames) {
			if (own(frame)) {
				return cause + thrown + " at " + frame;
			}
		}
		return cause + thrown;
	}

	/** The structure sets of the 1.21.1 mods (not Minecraft's own: 1.12.2 generates its own villages, temples, ...). */
	static List<ResourceLocation> modStructureSets() {
		List<ResourceLocation> sets = new ArrayList<>();
		String folder = "worldgen/structure_set/";
		for (ResourceLocation file : ServerResourceManager.get().listResources("worldgen/structure_set", id -> id.getPath().endsWith(".json")).keySet()) {
			if ("minecraft".equals(file.getNamespace())) {
				continue;
			}
			String path = file.getPath();
			sets.add(new ResourceLocation(file.getNamespace(), path.substring(folder.length(), path.length() - ".json".length())));
		}
		return sets;
	}

	/**
	 * The ids of the biomes a structure may start in, from its file's {@code "biomes"} (an id, a {@code #tag} or a list
	 * of them; tags of the mods and of Minecraft 1.21.1, nested ones too). Empty when the file can't be read.
	 */
	static Set<ResourceLocation> structureBiomes(ResourceLocation structure) {
		JsonObject json = json(structure.getNamespace(), "worldgen/structure/" + structure.getPath() + ".json");
		Set<ResourceLocation> biomes = new LinkedHashSet<>();
		if (json != null && json.has("biomes")) {
			addBiomes(json.get("biomes"), biomes);
		}
		return biomes;
	}

	private static void addBiomes(JsonElement element, Set<ResourceLocation> into) {
		if (element.isJsonArray()) {
			for (JsonElement one : element.getAsJsonArray()) {
				addBiomes(one, into);
			}
		} else if (element.isJsonPrimitive()) {
			String name = element.getAsString();
			if (name.startsWith("#")) {
				into.addAll(biomeTag(new ResourceLocation(name.substring(1))));
			} else {
				into.add(new ResourceLocation(name));
			}
		}
	}

	/** The biome ids of a biome tag ({@code data/<ns>/tags/worldgen/biome/<path>.json} of every pack, merged). */
	static synchronized Set<ResourceLocation> biomeTag(ResourceLocation tag) {
		Set<ResourceLocation> known = BIOME_TAGS.get(tag);
		if (known != null) {
			return known;
		}
		BIOME_TAGS.put(tag, Collections.emptySet());
		Set<ResourceLocation> ids = new LinkedHashSet<>();
		var file = new ResourceLocation(tag.getNamespace(), "tags/worldgen/biome/" + tag.getPath() + ".json");
		for (var resource : ServerResourceManager.get().getResourceStack(file)) {
			try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
				@SuppressWarnings("deprecation")
				JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
				if (json.has("replace") && json.get("replace").getAsBoolean()) {
					ids.clear();
				}
				for (JsonElement value : json.getAsJsonArray("values")) {
					String name = value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString();
					if (name.startsWith("#")) {
						ids.addAll(biomeTag(new ResourceLocation(name.substring(1))));
					} else {
						ids.add(new ResourceLocation(name));
					}
				}
			} catch (java.io.IOException | RuntimeException e) {
				report("biome tag " + tag, e.toString());
			}
		}
		BIOME_TAGS.put(tag, ids);
		return ids;
	}

	@Nullable
	private static JsonObject json(String namespace, String path) {
		var resource = ServerResourceManager.get().getResource(new ResourceLocation(namespace, path));
		if (!resource.isPresent()) {
			return null;
		}
		try (var reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
			@SuppressWarnings("deprecation")
			JsonElement json = new JsonParser().parse(reader);
			return json.isJsonObject() ? json.getAsJsonObject() : null;
		} catch (java.io.IOException | RuntimeException e) {
			report(namespace + ":" + path, e.toString());
			return null;
		}
	}

	static synchronized void report(String what, String why) {
		if (REPORTED.add(what)) {
			LOGGER.warn("Couldn't read the 1.21 structure data {}: {}", what, why);
		}
	}

	public static synchronized void bootstrap() {
		if (bootstrapped) {
			return;
		}
		bootstrapped = true;
		for (String type : TYPES) {
			try {
				Class.forName(type, true, Launch.classLoader);
			} catch (ClassNotFoundException e) {
				// not in this 1.21.1 API
			} catch (RuntimeException | LinkageError e) {
				LOGGER.warn("Couldn't load the 1.21 kinds of {}: {}", type, e.toString());
			}
		}
		// the kinds of pool alias bindings (1.21's BuiltInRegistries fills them so; a jigsaw structure's pool_aliases)
		try {
			var registry = io.github.xponer2.translator.util.BuiltInRegistries.POOL_ALIAS_BINDING_TYPE;
			if (registry.keySet().isEmpty()) {
				Class.forName("net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBindings", true, Launch.classLoader)
						.getMethod("bootstrap", io.github.xponer2.translator.registry.Registry.class).invoke(null, registry);
			}
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			LOGGER.warn("Couldn't register the 1.21 pool alias bindings: {}", e.toString());
		}
	}
}
