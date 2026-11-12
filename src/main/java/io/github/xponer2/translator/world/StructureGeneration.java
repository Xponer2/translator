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
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.xponer2.translator.registry.Holder;
import io.github.xponer2.translator.registry.RegistryAccess;
import io.github.xponer2.translator.registry.ResourceKey;

import net.minecraft.block.state.IBlockState;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.fml.common.IWorldGenerator;

/**
 * The structures of the 1.21.1 mods in the 1.12.2 world (Twilight Forest's landmarks, the Aether's dungeons, Iron's
 * Spells' towers): 1.12.2 has no 1.21 structure generation, so none of them was made. For each structure set of the mods
 * ({@code data/<mod>/worldgen/structure_set}) its 1.21.1 placement picks the start chunks (with the world's seed, as on
 * 1.21); a start's structure is made by its own 1.21.1 code ({@code Structure.generate}) when a chunk near it is
 * populated, and its pieces are placed part by part ({@code StructureStart.placeInChunk}) in the area 1.12.2 populates
 * (8 blocks into the chunk, so a piece never reaches an unloaded chunk). Heights come from the world's own terrain
 * generator (a chunk made aside, not the loaded one: a start is the same whenever it is made), biomes from its biome
 * provider. Starts are kept while the world is open and made again the same after a restart.
 * <p>
 * Off until it is checked in game: {@code -Dtranslator.structures=true}.
 */
public final class StructureGeneration implements IWorldGenerator {
	private static final Logger LOGGER = LogManager.getLogger("Translator Runtime");
	public static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("translator.structures", "false"));
	/** How far (chunks) a structure reaches from its start chunk: 1.21's limit for references and jigsaw size. */
	private static final int RANGE = 8;
	private static final Object NONE = new Object();

	private final Map<World, Level> levels = new java.util.WeakHashMap<>();
	@Nullable
	private static Object randomState;

	/** The 1.21 RandomState of the structures: made without its noise (see {@link Reflect#unsafe}), once. */
	public static synchronized Object randomState() {
		if (randomState == null) {
			try {
				randomState = Reflect.unsafe().allocateInstance(Class.forName("net.minecraft.world.level.levelgen.RandomState", true, Launch.classLoader));
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("No carried RandomState", e);
			}
		}
		return randomState;
	}

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
		if (!ENABLED || !(world instanceof WorldServer server)) {
			return;
		}
		Level level = this.levels.computeIfAbsent(world, w -> Level.create(server));
		if (level != null) {
			level.populate(chunkX, chunkZ);
		}
	}

	/** One world's structure sets, its 1.21 generator and the starts made so far. */
	private static final class Level {
		final WorldServer world;
		final Generator generator;
		final Source biomes;
		final Object randomState;
		final Object structureState;
		final List<ModSet> sets;

		private Level(WorldServer world, Generator generator, Source biomes, Object randomState, Object structureState, List<ModSet> sets) {
			this.world = world;
			this.generator = generator;
			this.biomes = biomes;
			this.randomState = randomState;
			this.structureState = structureState;
			this.sets = sets;
		}

		@Nullable
		static Level create(WorldServer world) {
			try {
				DataStructures.bootstrap();
				var sets = new ArrayList<ModSet>();
				var holders = new ArrayList<Object>();
				for (ResourceLocation id : DataStructures.modStructureSets()) {
					ModSet set = ModSet.read(id);
					if (set != null) {
						sets.add(set);
						holders.add(Holder.direct(set.value));
					}
				}
				Source biomes = new Source(world);
				Generator generator = new Generator(biomes, world);
				Object randomState = randomState();
				Object structureState = Reflect.constructor("net.minecraft.world.level.chunk.ChunkGeneratorStructureState", 5)
						.newInstance(randomState, biomes, world.getSeed(), world.getSeed(), holders);
				LOGGER.info("Structures of the 1.21.1 mods in dimension {}: {} structure sets", world.provider.getDimension(), sets.size());
				return new Level(world, generator, biomes, randomState, structureState, sets);
			} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
				LOGGER.error("No structures of the 1.21.1 mods in dimension {}", world.provider.getDimension(), e);
				return null;
			}
		}

		/** Places the parts of the starts near the chunk that lie in the area 1.12.2 populates for it. */
		void populate(int chunkX, int chunkZ) {
			int x0 = chunkX * 16 + 8;
			int z0 = chunkZ * 16 + 8;
			var area = new StructureBoundingBox(x0, 0, z0, x0 + 15, 255, z0 + 15);
			ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
			for (ModSet set : this.sets) {
				if (set.broken) {
					continue;
				}
				for (int dx = -RANGE; dx <= RANGE; dx++) {
					for (int dz = -RANGE; dz <= RANGE; dz++) {
						Object start = set.start(this, chunkX + dx, chunkZ + dz);
						if (start == NONE) {
							continue;
						}
						try {
							StructureBoundingBox box = (StructureBoundingBox) Reflect.method(start, "getBoundingBox", 0).invoke(start);
							if (box == null || !box.intersectsWith(area)) {
								continue;
							}
							var random = new Random(this.world.getSeed() ^ ((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L));
							Reflect.method(start, "placeInChunk", 6).invoke(start, this.world, null, this.generator, random, area, chunk);
						} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
							set.failed(e);
						}
					}
				}
			}
		}

		/** Whether the biome of a 1.21 holder is one a structure may start in. */
		static Predicate<Object> biomeCheck(java.util.Set<ResourceLocation> allowed) {
			return holder -> {
				Object biome = holder instanceof Holder<?> one ? one.value() : null;
				return biome instanceof Biome b && b.getRegistryName() != null && allowed.contains(b.getRegistryName());
			};
		}
	}

	/** A structure set of a mod: its placement and structures (with the biomes each may start in), and its starts. */
	private static final class ModSet {
		final ResourceLocation id;
		final Object value;
		final Object placement;
		final List<Object> structures = new ArrayList<>();
		final List<ResourceLocation> structureIds = new ArrayList<>();
		final List<Integer> weights = new ArrayList<>();
		final List<Predicate<Object>> valid = new ArrayList<>();
		/** By start chunk: the start, or NONE. Cleared when it gets big (a start is made again the same). */
		final Map<Long, Object> starts = new HashMap<>();
		int failures;
		boolean broken;

		private ModSet(ResourceLocation id, Object value, Object placement) {
			this.id = id;
			this.value = value;
			this.placement = placement;
		}

		@Nullable
		static ModSet read(ResourceLocation id) {
			var registry = io.github.xponer2.translator.util.BuiltInRegistries.byKey(ResourceKey.<Object>createRegistryKey(new ResourceLocation("worldgen/structure_set")));
			Object value = registry.get(id);
			JsonObject json = setFile(id);
			if (value == null || json == null) {
				return null;
			}
			try {
				var set = new ModSet(id, value, Reflect.method(value, "placement", 0).invoke(value));
				var structureRegistry = io.github.xponer2.translator.util.BuiltInRegistries.byKey(ResourceKey.<Object>createRegistryKey(new ResourceLocation("worldgen/structure")));
				for (JsonElement entry : json.getAsJsonArray("structures")) {
					JsonObject one = entry.getAsJsonObject();
					var structureId = new ResourceLocation(one.get("structure").getAsString());
					Object structure = structureRegistry.get(structureId);
					if (structure == null) {
						continue;
					}
					set.structures.add(structure);
					set.structureIds.add(structureId);
					set.weights.add(one.has("weight") ? one.get("weight").getAsInt() : 1);
					set.valid.add(Level.biomeCheck(DataStructures.structureBiomes(structureId)));
				}
				return set.structures.isEmpty() ? null : set;
			} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
				DataStructures.report("structure set " + id, e.toString());
				return null;
			}
		}

		/** The start of this set in that chunk (made on first ask), or NONE. */
		Object start(Level level, int x, int z) {
			long key = ChunkPos.asLong(x, z);
			Object known = this.starts.get(key);
			if (known != null) {
				return known;
			}
			if (this.starts.size() > 100_000) {
				this.starts.clear();
			}
			Object start = NONE;
			try {
				if ((Boolean) Reflect.method(this.placement, "isStructureChunk", 3).invoke(this.placement, level.structureState, x, z)) {
					start = this.make(level, x, z);
				}
			} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
				this.failed(e);
			}
			this.starts.put(key, start);
			return start;
		}

		/** 1.21's choice among the set's structures (by weight, the next one when one doesn't fit there). */
		private Object make(Level level, int x, int z) throws ReflectiveOperationException {
			// the biome at the chunk's middle first: no structure of another dimension or climate is worked out
			Biome here = level.world.getBiomeProvider().getBiome(new BlockPos(x * 16 + 8, 64, z * 16 + 8));
			var candidates = new ArrayList<Integer>();
			for (int i = 0; i < this.structures.size(); i++) {
				if (this.valid.get(i).test(Holder.direct(here))) {
					candidates.add(i);
				}
			}
			if (candidates.isEmpty()) {
				return NONE;
			}
			var random = new Random(level.world.getSeed());
			long a = random.nextLong();
			long b = random.nextLong();
			random.setSeed((long) x * a ^ (long) z * b ^ level.world.getSeed());
			int total = 0;
			for (int i : candidates) {
				total += this.weights.get(i);
			}
			while (!candidates.isEmpty() && total > 0) {
				int pick = random.nextInt(total);
				int index = 0;
				for (; index < candidates.size() - 1; index++) {
					pick -= this.weights.get(candidates.get(index));
					if (pick < 0) {
						break;
					}
				}
				int chosen = candidates.get(index);
				Object start = this.generate(level, chosen, x, z);
				if (start != null) {
					return start;
				}
				candidates.remove(index);
				total -= this.weights.get(chosen);
			}
			return NONE;
		}

		@Nullable
		private Object generate(Level level, int index, int x, int z) throws ReflectiveOperationException {
			Object structure = this.structures.get(index);
			Object start = Reflect.method(structure, "generate", 10).invoke(structure, RegistryAccess.INSTANCE, level.generator, level.biomes, level.randomState,
					level.world.getStructureTemplateManager(), level.world.getSeed(), new ChunkPos(x, z), 0, level.world, this.valid.get(index));
			if (start == null || !(Boolean) Reflect.method(start, "isValid", 0).invoke(start)) {
				return null;
			}
			LOGGER.info("Structure {} of the set {} starts in chunk {}, {} (dimension {})", this.structureIds.get(index), this.id, x, z,
					level.world.provider.getDimension());
			return start;
		}

		void failed(Throwable e) {
			Throwable cause = e instanceof java.lang.reflect.InvocationTargetException && e.getCause() != null ? e.getCause() : e;
			if (this.failures++ < 3) {
				LOGGER.warn("Structure set {} of a 1.21.1 mod failed", this.id, cause);
			}
			if (this.failures >= 50) {
				// its code can't run here: left out, the world generates on without it
				this.broken = true;
				LOGGER.warn("Structure set {} left out after {} failures", this.id, this.failures);
			}
		}

		@Nullable
		private static JsonObject setFile(ResourceLocation id) {
			var resource = io.github.xponer2.translator.resources.ServerResourceManager.get()
					.getResource(new ResourceLocation(id.getNamespace(), "worldgen/structure_set/" + id.getPath() + ".json"));
			if (!resource.isPresent()) {
				return null;
			}
			try (var reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
				@SuppressWarnings("deprecation")
				JsonElement json = new JsonParser().parse(reader);
				return json.isJsonObject() && json.getAsJsonObject().has("structures") ? json.getAsJsonObject() : null;
			} catch (java.io.IOException | RuntimeException e) {
				return null;
			}
		}
	}

	/**
	 * The 1.21 ChunkGenerator a structure asks for heights and columns: the world's own terrain, from a chunk its 1.12.2
	 * generator makes aside (never the loaded chunk, whose trees and buildings would move a start between two runs).
	 */
	public static final class Generator extends ChunkGenerator {
		private final WorldServer world;
		private final Map<Long, Chunk> terrain = new LinkedHashMap<Long, Chunk>(64, 0.75F, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<Long, Chunk> eldest) {
				return this.size() > 128;
			}
		};

		Generator(BiomeSource biomes, WorldServer world) {
			super(biomes);
			this.world = world;
		}

		private Chunk terrain(int x, int z) {
			int chunkX = x >> 4;
			int chunkZ = z >> 4;
			return this.terrain.computeIfAbsent(ChunkPos.asLong(chunkX, chunkZ),
					key -> this.world.getChunkProvider().chunkGenerator.generateChunk(chunkX, chunkZ));
		}

		/** 1.21 {@code getBaseHeight}: one above the surface of that kind at the column. */
		public int getBaseHeight(int x, int z, Heightmap.Types type, World level, RandomState random) {
			return new ChunkHeightmap(this.terrain(x, z), type).getFirstAvailable(x & 15, z & 15);
		}

		/** 1.21 {@code getBaseColumn}: the blocks of the column, from y 0. */
		public NoiseColumn getBaseColumn(int x, int z, World level, RandomState random) {
			Chunk chunk = this.terrain(x, z);
			IBlockState[] column = new IBlockState[256];
			var pos = new BlockPos.MutableBlockPos();
			for (int y = 0; y < 256; y++) {
				column[y] = chunk.getBlockState(pos.setPos(x & 15, y, z & 15));
			}
			try {
				return (NoiseColumn) Reflect.constructor("net.minecraft.world.level.NoiseColumn", 2).newInstance(0, column);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
		}

		public int getSeaLevel() {
			return this.world.getSeaLevel();
		}

		public int getMinY() {
			return 0;
		}

		public int getGenDepth() {
			return 256;
		}
	}

	/** The 1.21 BiomeSource of a world: its 1.12.2 biome provider's biomes. */
	public static final class Source extends BiomeSource {
		private final WorldServer world;

		Source(WorldServer world) {
			this.world = world;
		}

		/** 1.21 {@code getNoiseBiome} (quarter-block coordinates). */
		public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
			return Holder.direct(this.world.getBiomeProvider().getBiome(new BlockPos(x << 2, y << 2, z << 2)));
		}

		/** 1.21 {@code collectPossibleBiomes}: every registered biome. */
		protected Stream<Holder<Biome>> collectPossibleBiomes() {
			var all = new ArrayList<Holder<Biome>>();
			for (Biome biome : net.minecraftforge.fml.common.registry.ForgeRegistries.BIOMES) {
				all.add(Holder.direct(biome));
			}
			return all.stream();
		}
	}

	/** Reflection on the carried classes (the Translator compiles against stand-ins of a few of them only). */
	private static final class Reflect {
		private static final Map<String, Method> METHODS = new HashMap<>();
		private static final Map<String, java.lang.reflect.Constructor<?>> CONSTRUCTORS = new HashMap<>();
		private static sun.misc.Unsafe unsafe;

		static synchronized Method method(Object target, String name, int parameters) throws NoSuchMethodException {
			String key = target.getClass().getName() + "." + name + "/" + parameters;
			Method method = METHODS.get(key);
			if (method == null) {
				for (Class<?> type = target.getClass(); type != null && method == null; type = type.getSuperclass()) {
					for (Method one : type.getDeclaredMethods()) {
						if (one.getName().equals(name) && one.getParameterCount() == parameters && !one.isBridge()) {
							method = one;
							break;
						}
					}
				}
				if (method == null) {
					throw new NoSuchMethodException(target.getClass().getName() + "." + name);
				}
				method.setAccessible(true);
				METHODS.put(key, method);
			}
			return method;
		}

		static synchronized java.lang.reflect.Constructor<?> constructor(String type, int parameters) throws ReflectiveOperationException {
			String key = type + "/" + parameters;
			var constructor = CONSTRUCTORS.get(key);
			if (constructor == null) {
				for (var one : Class.forName(type, true, Launch.classLoader).getDeclaredConstructors()) {
					if (one.getParameterCount() == parameters) {
						constructor = one;
					}
				}
				if (constructor == null) {
					throw new NoSuchMethodException(type + ".<init>/" + parameters);
				}
				constructor.setAccessible(true);
				CONSTRUCTORS.put(key, constructor);
			}
			return constructor;
		}

		/**
		 * A 1.21 RandomState with no noise in it (made without its constructor, which builds the 1.21 noise router): the
		 * heights come from the 1.12.2 terrain, a structure only passes it on to the generator.
		 */
		static synchronized sun.misc.Unsafe unsafe() throws ReflectiveOperationException {
			if (unsafe == null) {
				var field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
				field.setAccessible(true);
				unsafe = (sun.misc.Unsafe) field.get(null);
			}
			return unsafe;
		}
	}
}
