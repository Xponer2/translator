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

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import io.github.xponer2.translator.registry.RegistryAccess;
import io.github.xponer2.translator.registry.RegistryOps;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ResourceLocation;
import xyz.wagyourtail.jvmdg.shade.asm.ClassWriter;
import xyz.wagyourtail.jvmdg.shade.asm.MethodVisitor;
import xyz.wagyourtail.jvmdg.shade.asm.Opcodes;
import xyz.wagyourtail.jvmdg.shade.asm.Type;

/**
 * The 1.21.1 mods' configured and placed features ({@code data/<mod>/worldgen/configured_feature},
 * {@code placed_feature}) in the 1.21 registries of those kinds: an entry is read from its file by the 1.21.1 codecs
 * ({@code ConfiguredFeature.DIRECT_CODEC}, carried) when it's first asked for, so a mod's own lookup finds it and its
 * 1.21 feature code places it (BOP's bone meal: {@code registryOrThrow(CONFIGURED_FEATURE).get(HUGE_TOADSTOOL)
 * .place(level, generator, random, pos)}, its glowing moss and sprout patches).
 */
public final class DataFeatures {
	private static final Logger LOGGER = LogManager.getLogger("Translator Runtime");
	private static final ResourceLocation CONFIGURED = new ResourceLocation("worldgen/configured_feature");
	private static final ResourceLocation PLACED = new ResourceLocation("worldgen/placed_feature");

	/**
	 * The carried 1.21 types whose static fields register the vanilla kinds a feature file names by id (feature types,
	 * block state providers, placement modifiers, ...): 1.21's Bootstrap loads them before any data, here they load
	 * before the first feature file is read.
	 */
	private static final String[] KINDS = {
			"net.minecraft.world.level.levelgen.feature.Feature",
			"net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType",
			"net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType",
			"net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType",
			"net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType",
			"net.minecraft.world.level.levelgen.feature.featuresize.FeatureSizeType",
			"net.minecraft.world.level.levelgen.feature.rootplacers.RootPlacerType",
			"net.minecraft.world.level.levelgen.placement.PlacementModifierType",
			"net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType",
			"net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType",
			"net.minecraft.world.level.levelgen.structure.templatesystem.PosRuleTestType",
			"net.minecraft.world.level.levelgen.heightproviders.HeightProviderType",
			"net.minecraft.util.valueproviders.IntProviderType",
			"net.minecraft.util.valueproviders.FloatProviderType",
	};

	private static boolean bootstrapped;
	private static final Set<String> REPORTED = new HashSet<>();
	@Nullable
	private static Object generator;
	private static boolean noGenerator;

	private DataFeatures() {
	}

	/** Whether a registry's entries are the mods' feature files (configured and placed features). */
	public static boolean isDataRegistry(ResourceLocation registry) {
		return CONFIGURED.equals(registry) || PLACED.equals(registry) || DataStructures.isDataRegistry(registry);
	}

	/** The feature of a mod's file for that registry, read by the 1.21.1 codec; null when there is none or it doesn't read. */
	@Nullable
	public static Object decode(ResourceLocation registry, ResourceLocation id) {
		if (DataStructures.isDataRegistry(registry)) {
			// structures, their sets, jigsaw pools and processor lists
			return DataStructures.decode(registry, id);
		}
		if ("minecraft".equals(id.getNamespace())) {
			// a vanilla feature a mod's biome or feature names: made by 1.21's own feature bootstraps
			VanillaFeatures.ensure();
			var vanilla = io.github.xponer2.translator.util.BuiltInRegistries.byKey(io.github.xponer2.translator.registry.ResourceKey.createRegistryKey(registry));
			Object value = vanilla instanceof io.github.xponer2.translator.registry.MappedRegistry<?> mapped ? mapped.direct(id) : null;
			if (value != null) {
				return value;
			}
		}
		boolean placed = PLACED.equals(registry);
		JsonObject json = DataBiomes.featureFile(placed, id);
		if (json == null) {
			return null;
		}
		bootstrap();
		String type = placed ? "net.minecraft.world.level.levelgen.placement.PlacedFeature" : "net.minecraft.world.level.levelgen.feature.ConfiguredFeature";
		try {
			@SuppressWarnings("unchecked")
			Codec<Object> codec = (Codec<Object>) Class.forName(type, true, Launch.classLoader).getField("DIRECT_CODEC").get(null);
			DataResult<Object> result = codec.parse(RegistryOps.create(JsonOps.INSTANCE, RegistryAccess.INSTANCE), json);
			var value = result.result();
			if (!value.isPresent()) {
				report(id, result.error().map(DataResult.PartialResult::message).orElse("no value"));
			}
			return value.orElse(null);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			report(id, e.toString());
			return null;
		}
	}

	private static synchronized void report(ResourceLocation id, String why) {
		if (REPORTED.add(id.toString())) {
			LOGGER.warn("Couldn't read the feature {} of a 1.21.1 mod: {}", id, why);
		}
	}

	private static synchronized void bootstrap() {
		if (bootstrapped) {
			return;
		}
		bootstrapped = true;
		for (String kind : KINDS) {
			try {
				Class.forName(kind, true, Launch.classLoader);
			} catch (ClassNotFoundException e) {
				// not in this 1.21.1 API
			} catch (RuntimeException | LinkageError e) {
				LOGGER.warn("Couldn't load the 1.21 kinds of {}: {}", kind, e.toString());
			}
		}
	}

	private static final java.util.Map<net.minecraft.world.biome.Biome, Object> SETTINGS = new java.util.IdentityHashMap<>();

	/**
	 * 1.21 {@code biome.getGenerationSettings()}: the placed features of a mod's biome by its file ({@code "features"},
	 * per generation step; those of the 1.21.1 game itself aren't here, nor the carvers), as the carried 1.21
	 * BiomeGenerationSettings; the empty one for a 1.12.2 biome. Bone meal asks it for the biome's flowers
	 * ({@code getFlowerFeatures()}: BOP's algal end stone, grass blocks of the mods).
	 */
	@Nullable
	public static synchronized Object generationSettings(net.minecraft.world.biome.Biome biome) {
		if (SETTINGS.containsKey(biome)) {
			return SETTINGS.get(biome);
		}
		Object settings = null;
		try {
			Class<?> type = Class.forName("net.minecraft.world.level.biome.BiomeGenerationSettings", true, Launch.classLoader);
			settings = type.getField("EMPTY").get(null);
			JsonObject json = biome.getRegistryName() == null ? null : DataBiomes.biomeFile(biome.getRegistryName());
			if (json != null && json.has("features") && json.get("features").isJsonArray()) {
				bootstrap();
				Class<?> holderSet = Class.forName("net.minecraft.core.HolderSet", true, Launch.classLoader);
				java.lang.reflect.Method direct = holderSet.getMethod("direct", java.util.List.class);
				var registry = io.github.xponer2.translator.util.BuiltInRegistries.byKey(
						io.github.xponer2.translator.registry.ResourceKey.<Object>createRegistryKey(PLACED));
				var steps = new java.util.ArrayList<Object>();
				for (var step : json.getAsJsonArray("features")) {
					var holders = new java.util.ArrayList<Object>();
					if (step.isJsonArray()) {
						for (var entry : step.getAsJsonArray()) {
							// the mods' placed features (a vanilla one or a tag isn't here)
							if (entry.isJsonPrimitive() && !entry.getAsString().startsWith("#")) {
								registry.getHolder(new ResourceLocation(entry.getAsString())).ifPresent(holders::add);
							}
						}
					}
					steps.add(direct.invoke(null, holders));
				}
				settings = type.getConstructor(java.util.Map.class, java.util.List.class).newInstance(new java.util.HashMap<>(), steps);
			}
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			LOGGER.warn("Couldn't make the features of the biome {}: {}", biome.getRegistryName(), e.toString());
		}
		SETTINGS.put(biome, settings);
		return settings;
	}

	/** Placed features whose 1.21.1 code failed once: left out from then on (the world generates on without them). */
	private static final Set<Object> BROKEN = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
	@Nullable
	private static java.lang.reflect.Method placeWithBiomeCheck;

	/**
	 * The mod's own placed features of a biome (its file's {@code "features"}, step by step, as 1.21 decorates a chunk:
	 * BOP's flowers, grass, mushrooms, rocks; not the trees, those grow from the data, nor vanilla features), placed by
	 * the 1.21.1 code ({@code PlacedFeature.placeWithBiomeCheck}) in the chunk 1.12.2 populates: its origin is 8 blocks in,
	 * so what a feature spreads over (0-15 across, a patch's radius) stays in the four chunks loaded for it.
	 */
	static void decorate(net.minecraft.world.biome.Biome biome, net.minecraft.world.World world, java.util.Random random, net.minecraft.util.math.BlockPos corner) {
		if (world.isRemote) {
			return;
		}
		Object settings = generationSettings(biome);
		Object chunkGenerator = generator();
		if (settings == null || chunkGenerator == null) {
			return;
		}
		var placedRegistry = io.github.xponer2.translator.util.BuiltInRegistries.byKey(
				io.github.xponer2.translator.registry.ResourceKey.<Object>createRegistryKey(PLACED));
		net.minecraft.util.math.BlockPos origin = corner.add(8, 0, 8);
		try {
			for (Object step : (java.util.List<?>) settings.getClass().getMethod("features").invoke(settings)) {
				for (Object member : (Iterable<?>) step) {
					Object placed = ((io.github.xponer2.translator.registry.Holder<?>) member).value();
					ResourceLocation id = placedRegistry.getKey(placed);
					// a vanilla feature of the biome's list: 1.12.2's decorator has its look-alike (ores, lakes, springs,
					// grass and flowers by DataBiomes' counts); placing it too made two of each
					if (placed == null || id == null || "minecraft".equals(id.getNamespace()) || DataBiomes.treeLike(id.getPath()) || isBroken(placed)) {
						continue;
					}
					place(id, placed, world, chunkGenerator, random, origin);
				}
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			report(biome.getRegistryName() == null ? new ResourceLocation("unknown") : biome.getRegistryName(), "its features: " + e);
		}
	}

	private static void place(ResourceLocation id, Object placed, net.minecraft.world.World world, Object chunkGenerator, java.util.Random random,
			net.minecraft.util.math.BlockPos origin) throws ReflectiveOperationException {
		java.lang.reflect.Method method = placeWithBiomeCheck;
		if (method == null) {
			for (java.lang.reflect.Method one : placed.getClass().getMethods()) {
				if (one.getName().equals("placeWithBiomeCheck") && one.getParameterCount() == 4) {
					method = one;
				}
			}
			if (method == null) {
				throw new NoSuchMethodException("PlacedFeature.placeWithBiomeCheck");
			}
			placeWithBiomeCheck = method;
		}
		try {
			method.invoke(placed, world, chunkGenerator, random, origin);
		} catch (java.lang.reflect.InvocationTargetException e) {
			synchronized (BROKEN) {
				BROKEN.add(placed);
			}
			report(id, "placing it: " + e.getCause());
		}
	}

	private static boolean isBroken(Object placed) {
		synchronized (BROKEN) {
			return BROKEN.contains(placed);
		}
	}

	/**
	 * A 1.21 ChunkGenerator for the features a mod places itself (bone meal: {@code feature.place(level,
	 * level.getChunkSource().getGenerator(), random, pos)}): 1.12.2 has none of that kind. It only tells the heights a
	 * placed feature's context reads ({@code getMinY}, {@code getGenDepth}, {@code getSeaLevel}: 0, 256, 63); its other
	 * methods aren't there. Made once, as a subclass of the carried class; null if that can't be made.
	 */
	@Nullable
	public static synchronized Object generator() {
		if (generator != null || noGenerator) {
			return generator;
		}
		try {
			Class<?> base = Class.forName("net.minecraft.world.level.chunk.ChunkGenerator", true, Launch.classLoader);
			Constructor<?> constructor = null;
			for (Constructor<?> one : base.getDeclaredConstructors()) {
				if (one.getParameterCount() == 1) {
					constructor = one;
				}
			}
			if (constructor == null) {
				throw new NoSuchMethodException("ChunkGenerator(BiomeSource)");
			}
			String name = "translator_gen/FeatureChunkGenerator";
			var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, name, null, Type.getInternalName(base), null);
			MethodVisitor init = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
			init.visitCode();
			init.visitVarInsn(Opcodes.ALOAD, 0);
			init.visitInsn(Opcodes.ACONST_NULL);
			init.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(base), "<init>", Type.getConstructorDescriptor(constructor), false);
			init.visitInsn(Opcodes.RETURN);
			init.visitMaxs(0, 0);
			init.visitEnd();
			constant(writer, "getMinY", 0);
			constant(writer, "getGenDepth", 256);
			constant(writer, "getSeaLevel", 63);
			writer.visitEnd();
			byte[] bytes = writer.toByteArray();
			Class<?> made = new Definer(Launch.classLoader).define(name.replace('/', '.'), bytes);
			generator = made.getConstructor().newInstance();
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			noGenerator = true;
			LOGGER.warn("No 1.21 chunk generator for the mods' features: {}", e.toString());
		}
		return generator;
	}

	private static void constant(ClassWriter writer, String method, int value) {
		MethodVisitor code = writer.visitMethod(Opcodes.ACC_PUBLIC, method, "()I", null, null);
		code.visitCode();
		code.visitLdcInsn(value);
		code.visitInsn(Opcodes.IRETURN);
		code.visitMaxs(0, 0);
		code.visitEnd();
	}

	private static final class Definer extends ClassLoader {
		Definer(ClassLoader parent) {
			super(parent);
		}

		Class<?> define(String name, byte[] bytes) {
			return this.defineClass(name, bytes, 0, bytes.length);
		}
	}
}
