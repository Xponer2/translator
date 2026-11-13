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
package io.github.xponer2.translator.registry;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import io.github.xponer2.translator.recipe.ICondition;
import io.github.xponer2.translator.runtime.RuntimeMods;
import io.github.xponer2.translator.tags.TagManager;
import io.github.xponer2.translator.util.BuiltInRegistries;

import net.minecraft.util.ResourceLocation;

/**
 * The registries the 1.21.1 mods named in {@link DataPackRegistryEvent.NewRegistry}, filled from the files of the
 * mods' jars as 1.21's RegistryDataLoader does: {@code data/<namespace>/<dir>/<path>.json} is {@code namespace:path},
 * where dir is the registry's path for a minecraft registry and {@code <its namespace>/<its path>} otherwise. Read once,
 * after the mods' items and blocks are registered (the files name them). There are no world data packs on 1.12.2, so
 * the mods' own files are all there is.
 */
public final class DataPackRegistries {
	private static final Logger LOGGER = LogManager.getLogger("Translator Runtime");
	private static final Map<ResourceKey<?>, Codec<?>> REGISTRIES = new LinkedHashMap<>();
	private static boolean loaded;

	private DataPackRegistries() {
	}

	static synchronized <T> void add(ResourceKey<Registry<T>> key, Codec<T> codec) {
		REGISTRIES.putIfAbsent(key, codec);
		// the registry exists from now on, empty until the files are read
		BuiltInRegistries.byKey(key);
		if (loaded) {
			load(key, codec);
		}
	}

	/** NeoForge posts the event once every mod is constructed, after NewRegistryEvent. */
	public static void fire() {
		try {
			net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new DataPackRegistryEvent.NewRegistry());
		} catch (Throwable t) {
			LOGGER.error("DataPackRegistryEvent.NewRegistry failed", t);
		}
	}

	/**
	 * Reads the files of every registry named so far, together: an entry may name another entry of its own registry or of
	 * another one read here (Twilight Forest's biome layers stack on each other, its chunk blankets name structures), so
	 * the files are decoded in passes until a pass reads none more (1.21's loader binds such references later; here
	 * what one names is read first). The vanilla kinds the files name by id (block state providers, structure types, ...)
	 * are loaded before.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static synchronized void loadAll() {
		if (loaded) {
			return;
		}
		loaded = true;
		bootstrapKinds();
		addVanilla();
		List<Pending> pending = new ArrayList<>();
		for (var entry : new ArrayList<>(REGISTRIES.entrySet())) {
			pending.addAll(read((ResourceKey) entry.getKey(), (Codec) entry.getValue()));
		}
		decode(pending);
	}

	/**
	 * 1.21's own data pack registries the mods add to and read (their types are carried): registry, entry class, the
	 * class whose {@code bootstrap(context)} makes the vanilla entries (1.21 writes its data with it).
	 */
	private static final String[][] VANILLA = {
			{"damage_type", "net.minecraft.world.damagesource.DamageType", "net.minecraft.world.damagesource.DamageTypes"},
			{"wolf_variant", "net.minecraft.world.entity.animal.WolfVariant", "net.minecraft.world.entity.animal.WolfVariants"},
	};

	/**
	 * The vanilla data pack registries: the vanilla entries from 1.21's bootstrap, then the mods' files like any other
	 * (Twilight Forest's damage types expired, frozen, hydra_mortar, axing; its hostile, mist and winter wolves are the
	 * vanilla pale wolf). No mod announces them, so they stayed empty: "Missing element".
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void addVanilla() {
		for (String[] vanilla : VANILLA) {
			ResourceKey key = ResourceKey.createRegistryKey(new ResourceLocation("minecraft", vanilla[0]));
			try {
				Codec<?> codec = (Codec<?>) Class.forName(vanilla[1], true, net.minecraft.launchwrapper.Launch.classLoader).getField("DIRECT_CODEC")
						.get(null);
				REGISTRIES.putIfAbsent(key, codec);
				Registry<?> registry = BuiltInRegistries.byKey(key);
				if (registry instanceof MappedRegistry mapped && mapped.keySet().isEmpty()) {
					bootstrap(vanilla[2], key, mapped);
					LOGGER.info("Data pack registry {}: {} vanilla entries", key.location(), mapped.keySet().size());
				}
			} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
				LOGGER.warn("Data pack registry {}: no vanilla entries ({})", key.location(), io.github.xponer2.translator.world.DataStructures.where(e));
			}
		}
	}

	/**
	 * Runs a carried {@code bootstrap(BootstrapContext)}: what it registers goes into the registry (each entry as it
	 * comes, so a failure late in it keeps the ones before), what it looks up is the Translator's registries.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void bootstrap(String owner, ResourceKey<?> key, MappedRegistry registry) throws ReflectiveOperationException {
		ClassLoader loader = net.minecraft.launchwrapper.Launch.classLoader;
		Class<?> contextType = Class.forName("net.minecraft.data.worldgen.BootstrapContext", true, loader);
		Object context = java.lang.reflect.Proxy.newProxyInstance(loader, new Class<?>[] {contextType}, (proxy, method, args) -> {
			switch (method.getName()) {
				case "register": {
					ResourceKey entry = (ResourceKey) args[0];
					if (!registry.hasEntry(entry.location())) {
						registry.register(entry.location(), args[1]);
					}
					return new Registry.Reference(entry, args[1]);
				}
				case "lookup":
					return BuiltInRegistries.byKey((ResourceKey) args[0]);
				case "registryLookup":
					return java.util.Optional.ofNullable(BuiltInRegistries.byKey((ResourceKey) args[0]));
				case "equals":
					return proxy == args[0];
				case "hashCode":
					return System.identityHashCode(proxy);
				default:
					return "BootstrapContext[" + key.location() + "]";
			}
		});
		try {
			Class.forName(owner, true, loader).getMethod("bootstrap", contextType).invoke(null, context);
		} catch (java.lang.reflect.InvocationTargetException e) {
			LOGGER.warn("Data pack registry {}: the vanilla bootstrap stopped at {} entries: {}", key.location(), registry.keySet().size(),
					io.github.xponer2.translator.world.DataStructures.where(e.getCause()));
		}
	}

	/** The vanilla kinds a file names by id: 1.21's Bootstrap loads them before any data. */
	private static void bootstrapKinds() {
		try {
			io.github.xponer2.translator.world.DataFeatures.bootstrap();
			io.github.xponer2.translator.world.DataStructures.bootstrap();
		} catch (RuntimeException | LinkageError e) {
			LOGGER.warn("Couldn't load the 1.21 kinds the data pack registries name: {}", e.toString());
		}
	}

	private static String dir(ResourceLocation registry) {
		return "minecraft".equals(registry.getNamespace()) ? registry.getPath() : registry.getNamespace() + "/" + registry.getPath();
	}

	/** A file of a registry not read yet. */
	private static final class Pending {
		final ResourceKey<?> key;
		final Codec<Object> codec;
		final MappedRegistry<Object> registry;
		final ResourceLocation id;
		final JsonElement json;
		String error = "not read";

		Pending(ResourceKey<?> key, Codec<Object> codec, MappedRegistry<Object> registry, ResourceLocation id, JsonElement json) {
			this.key = key;
			this.codec = codec;
			this.registry = registry;
			this.id = id;
			this.json = json;
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static <T> void load(ResourceKey<Registry<T>> key, Codec<T> codec) {
		bootstrapKinds();
		decode((List) read(key, codec));
	}

	/** The files of a registry, in a stable order (a registry's ids keep the same order from game to game). */
	@SuppressWarnings("unchecked")
	private static <T> List<Pending> read(ResourceKey<Registry<T>> key, Codec<T> codec) {
		String dir = "/" + dir(key.location()) + "/";
		Map<ResourceLocation, JsonElement> files = new TreeMap<>(java.util.Comparator.comparing(ResourceLocation::toString));
		RuntimeMods.forEachData((path, bytes) -> {
			int slash = path.indexOf('/');
			if (slash <= 0 || !path.endsWith(".json") || !path.startsWith(dir, slash)) {
				return;
			}
			String name = path.substring(slash + dir.length(), path.length() - ".json".length());
			ResourceLocation id;
			try {
				id = new ResourceLocation(path.substring(0, slash), name);
			} catch (RuntimeException e) {
				return;
			}
			try {
				files.put(id, new JsonParser().parse(new String(bytes.get(), StandardCharsets.UTF_8)));
			} catch (RuntimeException e) {
				LOGGER.warn("Couldn't read data/{}: {}", path, e.toString());
			}
		});
		Registry<T> registry = BuiltInRegistries.byKey(key);
		if (!(registry instanceof MappedRegistry)) {
			LOGGER.warn("The data pack registry {} isn't one the Translator fills ({})", key.location(), registry);
			return Collections.emptyList();
		}
		List<Pending> pending = new ArrayList<>();
		for (var file : files.entrySet()) {
			if (!((MappedRegistry<T>) registry).hasEntry(file.getKey()) && conditionsMet(file.getValue())) {
				pending.add(new Pending(key, (Codec<Object>) codec, (MappedRegistry<Object>) registry, file.getKey(), file.getValue()));
			}
		}
		if (pending.isEmpty()) {
			LOGGER.info("Data pack registry {}: no files in the mods", key.location());
		}
		return pending;
	}

	/** The files decoded in passes, each pass those whose references are there now; then what is left is logged. */
	private static void decode(List<Pending> pending) {
		var ops = RegistryOps.create(JsonOps.INSTANCE, RegistryAccess.INSTANCE);
		Map<ResourceKey<?>, int[]> counts = new LinkedHashMap<>();
		for (Pending file : pending) {
			counts.putIfAbsent(file.key, new int[1]);
		}
		List<Pending> left = new ArrayList<>(pending);
		boolean progress = true;
		while (progress && !left.isEmpty()) {
			progress = false;
			for (var iterator = left.iterator(); iterator.hasNext(); ) {
				Pending file = iterator.next();
				try {
					DataResult<Object> result = file.codec.parse(ops, file.json);
					var value = result.result();
					if (value.isPresent()) {
						file.registry.register(file.id, value.get());
						counts.get(file.key)[0]++;
						iterator.remove();
						progress = true;
					} else {
						file.error = result.error().map(DataResult.PartialResult::message).orElse("no value");
					}
				} catch (RuntimeException | LinkageError e) {
					file.error = describe(e);
				}
			}
		}
		for (var count : counts.entrySet()) {
			LOGGER.info("Data pack registry {}: {} entries from the mods' files", count.getKey().location(), count.getValue()[0]);
			int shown = 0;
			for (Pending file : left) {
				if (file.key.equals(count.getKey()) && shown++ < 20) {
					LOGGER.warn("Data pack registry {}: couldn't read {}: {}", count.getKey().location(), file.id, file.error);
				}
			}
		}
	}

	/** An exception with where it came from: the first frame of a mod or of the Translator (an NPE says nothing else). */
	private static String describe(Throwable e) {
		return io.github.xponer2.translator.world.DataStructures.where(e);
	}

	private static boolean conditionsMet(JsonElement json) {
		if (!json.isJsonObject() || !((JsonObject) json).has("neoforge:conditions")) {
			return true;
		}
		try {
			var conditions = ICondition.LIST_CODEC.parse(JsonOps.INSTANCE, ((JsonObject) json).get("neoforge:conditions")).result();
			for (var condition : conditions.orElse(Collections.emptyList())) {
				if (!condition.test(TagManager.conditionContext())) {
					return false;
				}
			}
		} catch (RuntimeException | LinkageError e) {
			// a condition the Translator can't read: the file is kept
		}
		return true;
	}
}
