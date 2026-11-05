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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.MappedRegistry;
import io.github.xponer2.translator.registry.Registry;
import io.github.xponer2.translator.registry.ResourceKey;
import io.github.xponer2.translator.util.BuiltInRegistries;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ResourceLocation;

/**
 * The vanilla configured and placed features of 1.21.1 (grass and flower patches, trees, ores, ...), which the mods'
 * biomes name ({@code minecraft:patch_grass}, {@code minecraft:oak_checked}): 1.12.2 has no such data, so they are made
 * by 1.21's own code, its feature bootstraps (FeatureUtils, PlacementUtils and the kinds under them), run once with a
 * bootstrap context that registers into the Translator's feature registries. A feature that names another one before
 * it exists gets a holder that looks it up when used. A part that fails leaves out only its own kind of features.
 */
final class VanillaFeatures {
	private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("Translator Runtime");
	private static final String[] CONFIGURED = {"AquaticFeatures", "CaveFeatures", "EndFeatures", "MiscOverworldFeatures", "NetherFeatures", "OreFeatures",
			"PileFeatures", "TreeFeatures", "VegetationFeatures"};
	private static final String[] PLACED = {"AquaticPlacements", "CavePlacements", "EndPlacements", "MiscOverworldPlacements", "NetherPlacements",
			"OrePlacements", "TreePlacements", "VegetationPlacements", "VillagePlacements"};
	private static boolean done;

	private VanillaFeatures() {
	}

	/** Runs the vanilla bootstraps once (the first time a mod's data names a vanilla feature). */
	static synchronized void ensure() {
		if (done) {
			return;
		}
		done = true;
		Registry<Object> configured = registry("worldgen/configured_feature");
		Registry<Object> placed = registry("worldgen/placed_feature");
		int before = configured.keySet().size() + placed.keySet().size();
		var failed = new java.util.ArrayList<String>();
		for (String kind : CONFIGURED) {
			run("net.minecraft.data.worldgen.features." + kind, configured, configured, placed, failed);
		}
		for (String kind : PLACED) {
			run("net.minecraft.data.worldgen.placement." + kind, placed, configured, placed, failed);
		}
		LOGGER.info("Vanilla 1.21.1 features made by their own code: {} (parts that failed: {})",
				configured.keySet().size() + placed.keySet().size() - before, failed.isEmpty() ? "none" : failed);
	}

	@SuppressWarnings("unchecked")
	private static Registry<Object> registry(String name) {
		return (Registry<Object>) BuiltInRegistries.byKey(ResourceKey.createRegistryKey(new ResourceLocation(name)));
	}

	private static void run(String type, Registry<Object> target, Registry<Object> configured, Registry<Object> placed, java.util.List<String> failed) {
		try {
			Class<?> context = Class.forName("net.minecraft.data.worldgen.BootstrapContext", true, Launch.classLoader);
			Class<?> kind = Class.forName(type, true, Launch.classLoader);
			Method bootstrap = null;
			for (Method method : kind.getMethods()) {
				if ("bootstrap".equals(method.getName()) && method.getParameterCount() == 1 && method.getParameterTypes()[0] == context) {
					bootstrap = method;
				}
			}
			if (bootstrap == null) {
				return;
			}
			Object proxy = Proxy.newProxyInstance(Launch.classLoader, new Class<?>[] {context}, (self, method, args) -> {
				switch (method.getName()) {
				case "register": {
					@SuppressWarnings("unchecked")
					ResourceKey<Object> key = (ResourceKey<Object>) args[0];
					Object value = args[1];
					if (target instanceof MappedRegistry<Object> mapped && !mapped.hasEntry(key.location()) && value != null) {
						try {
							mapped.register(key.location(), value);
						} catch (IllegalStateException e) {
							// the same feature under a second name: the first stays
						}
					}
					return new Registry.Reference<>(key, value);
				}
				case "lookup":
					return lookup((ResourceKey<?>) args[0], configured, placed);
				case "registryLookup":
					return Optional.of(lookup((ResourceKey<?>) args[0], configured, placed));
				case "hashCode":
					return System.identityHashCode(self);
				case "equals":
					return self == args[0];
				case "toString":
					return "BootstrapContext{" + type + "}";
				default:
					throw new UnsupportedOperationException(method.getName());
				}
			});
			bootstrap.invoke(null, proxy);
		} catch (java.lang.reflect.InvocationTargetException e) {
			failed.add(type.substring(type.lastIndexOf('.') + 1) + ": " + e.getCause());
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			failed.add(type.substring(type.lastIndexOf('.') + 1) + ": " + e);
		}
	}

	private static Registry<?> lookup(ResourceKey<?> key, Registry<Object> configured, Registry<Object> placed) {
		String name = key.location().getPath();
		if ("worldgen/configured_feature".equals(name)) {
			return new Lazy(configured);
		}
		if ("worldgen/placed_feature".equals(name)) {
			return new Lazy(placed);
		}
		@SuppressWarnings({"unchecked", "rawtypes"})
		Registry<?> registry = BuiltInRegistries.byKey((ResourceKey) key);
		return registry;
	}

	/** A registry view whose holders look their value up when used (a feature named before it is registered). */
	private static final class Lazy implements Registry<Object> {
		private final Registry<Object> registry;

		Lazy(Registry<Object> registry) {
			this.registry = registry;
		}

		@Override
		public Reference<Object> getOrThrow(ResourceKey<Object> key) {
			return new LazyReference(key, this.registry);
		}

		@Override
		public Optional<Reference<Object>> getHolder(ResourceKey<Object> key) {
			return Optional.of(new LazyReference(key, this.registry));
		}

		@Override
		public ResourceKey<? extends Registry<Object>> key() {
			return this.registry.key();
		}

		@Nullable
		@Override
		public ResourceLocation getKey(Object value) {
			return this.registry.getKey(value);
		}

		@Nullable
		@Override
		public Object get(@Nullable ResourceLocation location) {
			return this.registry.get(location);
		}

		@Override
		public boolean containsKey(ResourceLocation location) {
			return this.registry.containsKey(location);
		}

		@Override
		public Set<ResourceLocation> keySet() {
			return this.registry.keySet();
		}

		@Override
		public Set<Map.Entry<ResourceKey<Object>, Object>> entrySet() {
			return this.registry.entrySet();
		}

		@Override
		public Iterator<Object> iterator() {
			return this.registry.iterator();
		}
	}

	/** A holder of a feature by its key, its value the registry's when asked. */
	private static final class LazyReference extends Registry.Reference<Object> {
		private final ResourceKey<Object> key;
		private final Registry<Object> registry;

		LazyReference(ResourceKey<Object> key, Registry<Object> registry) {
			super(key, null);
			this.key = key;
			this.registry = registry;
		}

		@Override
		public Object value() {
			Object value = this.registry.get(this.key.location());
			if (value == null) {
				throw new IllegalStateException("Missing element " + this.key);
			}
			return value;
		}

		@Override
		public boolean isBound() {
			return this.registry.containsKey(this.key.location());
		}
	}
}
