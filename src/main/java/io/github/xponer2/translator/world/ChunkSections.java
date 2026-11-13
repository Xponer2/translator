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

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;

import io.github.xponer2.translator.registry.Holder;
import io.github.xponer2.translator.registry.Registry;
import io.github.xponer2.translator.registry.ResourceKey;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.util.BitArray;
import net.minecraft.util.ObjectIntIdentityMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DimensionType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/**
 * 1.21 chunk sections on 1.12.2's: a 1.21 {@code LevelChunkSection} is block states and biomes (a
 * {@code PalettedContainer} each) and can be made from them, where 1.12.2's ExtendedBlockStorage only has block states
 * and its chunk keeps the biomes by column. A mod that keeps its own copies of chunks (JourneyMap's chunk snapshots: it
 * maps from them, saves and reads them) makes sections, asks them for biomes and saves both containers.
 * <ul>
 * <li>sections of a live chunk ({@code chunk.getSections()}) remember their chunk, their biomes are the chunk's;</li>
 * <li>sections made by a mod keep the biome container they were made with ({@link BiomeContainer}).</li>
 * </ul>
 */
public final class ChunkSections {
	private static final ResourceKey<Registry<Biome>> BIOMES = ResourceKey.createRegistryKey(new ResourceLocation("worldgen/biome"));
	/** The biomes of the sections a mod made. */
	private static final Map<ExtendedBlockStorage, BiomeContainer> MADE = Collections.synchronizedMap(new WeakHashMap<>());
	/** The chunk of the sections of live chunks (weakly: a section doesn't keep its chunk loaded). */
	private static final Map<ExtendedBlockStorage, WeakReference<Chunk>> LIVE = Collections.synchronizedMap(new WeakHashMap<>());
	private static final Map<Biome, Holder<Biome>> HOLDERS = Collections.synchronizedMap(new IdentityHashMap<>());
	private static final Map<Registry<?>, Codec<BlockStateContainer>> BIOME_CODECS = Collections.synchronizedMap(new WeakHashMap<>());
	@Nullable
	private static Field sectionData;

	private ChunkSections() {
	}

	// --- biomes as holders ---------------------------------------------------------------------------------------------

	/** The holder of a 1.12.2 biome under its registry name (one per biome). */
	public static Holder<Biome> holder(@Nullable Biome biome) {
		Biome known = biome == null ? Biomes.PLAINS : biome;
		return HOLDERS.computeIfAbsent(known, b -> {
			ResourceLocation name = b.getRegistryName();
			return name == null ? Holder.direct(b) : new Registry.Reference<>(ResourceKey.create(BIOMES, name), b);
		});
	}

	private static Holder<Biome> columnBiome(Chunk chunk, int cellX, int cellZ) {
		// the middle of the 4 by 4 cell, from the chunk's own biome ids (no biome provider: this is asked off the main thread)
		int id = chunk.getBiomeArray()[((cellZ & 3) * 4 + 2) << 4 | (cellX & 3) * 4 + 2] & 255;
		return holder(Biome.getBiome(id, Biomes.PLAINS));
	}

	// --- LevelChunk ----------------------------------------------------------------------------------------------------

	/**
	 * 1.21 {@code chunk.getSections()} (1.12.2's getBlockStorageArray, forced through here): the same array, its sections
	 * now knowing their chunk. An empty section is null on 1.12.2, as mods that read sections of 1.20+ already allow for
	 * (a chunk section missing from the save).
	 */
	public static ExtendedBlockStorage[] getBlockStorageArray(Chunk chunk) {
		ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
		WeakReference<Chunk> reference = null;
		for (ExtendedBlockStorage section : sections) {
			if (section != null && !LIVE.containsKey(section)) {
				if (reference == null) {
					reference = new WeakReference<>(chunk);
				}
				LIVE.put(section, reference);
			}
		}
		return sections;
	}

	// --- LevelChunkSection ---------------------------------------------------------------------------------------------

	/** 1.21 {@code new LevelChunkSection(states, biomes)}: a section holding these block states, with these biomes. */
	public static ExtendedBlockStorage newExtendedBlockStorage(BlockStateContainer states, @Nullable BlockStateContainer biomes) {
		var section = new ExtendedBlockStorage(0, false);
		if (states != null && !(states instanceof BiomeContainer)) {
			try {
				if (sectionData == null) {
					sectionData = ReflectionHelper.findField(ExtendedBlockStorage.class, "data", "field_177488_d");
				}
				sectionData.set(section, states);
			} catch (ReflectiveOperationException | RuntimeException e) {
				// the field can't be replaced: copied block by block
				for (int y = 0; y < 16; y++) {
					for (int z = 0; z < 16; z++) {
						for (int x = 0; x < 16; x++) {
							section.getData().set(x, y, z, states.get(x, y, z));
						}
					}
				}
			}
			section.recalculateRefCounts();
		}
		if (biomes instanceof BiomeContainer container) {
			MADE.put(section, container);
		}
		return section;
	}

	/** 1.21 {@code section.getNoiseBiome(x, y, z)} at cell coordinates (0 to 3): never null. */
	public static Holder<Biome> getNoiseBiome(@Nullable ExtendedBlockStorage section, int x, int y, int z) {
		if (section != null) {
			BiomeContainer made = MADE.get(section);
			if (made != null) {
				return made.biome(x, y, z);
			}
			WeakReference<Chunk> live = LIVE.get(section);
			Chunk chunk = live == null ? null : live.get();
			if (chunk != null) {
				return columnBiome(chunk, x, z);
			}
		}
		return holder(Biomes.PLAINS);
	}

	/** 1.21 {@code section.getBiomes()}: the biome container of a made section, a copy of the chunk's biomes for a live one. */
	public static BlockStateContainer getBiomes(@Nullable ExtendedBlockStorage section) {
		if (section != null) {
			BiomeContainer made = MADE.get(section);
			if (made != null) {
				return made;
			}
			WeakReference<Chunk> live = LIVE.get(section);
			Chunk chunk = live == null ? null : live.get();
			if (chunk != null) {
				var biomes = new BiomeContainer(columnBiome(chunk, 0, 0));
				for (int z = 0; z < 4; z++) {
					for (int x = 0; x < 4; x++) {
						Holder<Biome> biome = columnBiome(chunk, x, z);
						for (int y = 0; y < 4; y++) {
							biomes.biome(x, y, z, biome);
						}
					}
				}
				return biomes;
			}
		}
		return new BiomeContainer(holder(Biomes.PLAINS));
	}

	// --- PalettedContainer ---------------------------------------------------------------------------------------------

	/**
	 * 1.21 {@code new PalettedContainer(idMap, value, strategy)}: of block states filled with that state, or of biomes
	 * (any other value: a biome holder) filled with that biome.
	 */
	@SuppressWarnings("unchecked")
	public static BlockStateContainer newBlockStateContainer(@Nullable Object ids, @Nullable Object value, @Nullable Object strategy) {
		if (value instanceof Holder<?> holder && holder.value() instanceof Biome) {
			return new BiomeContainer((Holder<Biome>) holder);
		}
		if (value instanceof Biome biome) {
			return new BiomeContainer(holder(biome));
		}
		var states = new BlockStateContainer();
		if (value instanceof IBlockState state && state.getBlock() != net.minecraft.init.Blocks.AIR) {
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						states.set(x, y, z, state);
					}
				}
			}
		}
		return states;
	}

	/** 1.21 {@code container.set(x, y, z, value)}: a block state, or a biome of a biome container. */
	@SuppressWarnings("unchecked")
	public static void set(BlockStateContainer container, int x, int y, int z, @Nullable Object value) {
		if (container instanceof BiomeContainer biomes) {
			if (value instanceof Holder<?> holder && holder.value() instanceof Biome) {
				biomes.biome(x, y, z, (Holder<Biome>) holder);
			} else if (value instanceof Biome biome) {
				biomes.biome(x, y, z, holder(biome));
			}
		} else if (value instanceof IBlockState state) {
			container.set(x, y, z, state);
		}
	}

	/** 1.21 {@code container.getAndSet(x, y, z, value)}: what was there. */
	public static Object getAndSet(BlockStateContainer container, int x, int y, int z, @Nullable Object value) {
		Object before = get(container, x, y, z);
		set(container, x, y, z, value);
		return before;
	}

	public static Object getAndSetUnchecked(BlockStateContainer container, int x, int y, int z, @Nullable Object value) {
		return getAndSet(container, x, y, z, value);
	}

	/** 1.21 {@code container.get(x, y, z)} as a mod's erased call reads it (a block state, or a biome holder). */
	/**
	 * 1.21 {@code container.count(counter)}: each state in the section with how many blocks have it (the carried
	 * PalettedContainer.CountConsumer, called through its interface). AE2's meteorite compass looks for sky stone so.
	 */
	public static void count(BlockStateContainer container, Object counter) {
		var counts = new java.util.IdentityHashMap<IBlockState, int[]>();
		for (int index = 0; index < 4096; index++) {
			counts.computeIfAbsent(container.get(index & 15, index >> 8 & 15, index >> 4 & 15), k -> new int[1])[0]++;
		}
		try {
			java.lang.reflect.Method accept = Class.forName("net.minecraft.world.level.chunk.PalettedContainer$CountConsumer", true,
					net.minecraft.launchwrapper.Launch.classLoader).getMethod("accept", Object.class, int.class);
			for (var entry : counts.entrySet()) {
				accept.invoke(counter, entry.getKey(), entry.getValue()[0]);
			}
		} catch (java.lang.reflect.InvocationTargetException e) {
			Throwable cause = e.getCause();
			throw cause instanceof RuntimeException runtime ? runtime : new IllegalStateException(cause);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("No carried PalettedContainer.CountConsumer", e);
		}
	}

	/** 1.21 {@code container.getAll(consumer)}: each different state in the section once (Twilight Forest's chunk blankets). */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void getAll(BlockStateContainer container, java.util.function.Consumer consumer) {
		var seen = java.util.Collections.newSetFromMap(new IdentityHashMap<IBlockState, Boolean>());
		for (int index = 0; index < 4096; index++) {
			IBlockState state = container.get(index & 15, index >> 8 & 15, index >> 4 & 15);
			if (seen.add(state)) {
				consumer.accept(state);
			}
		}
	}

	public static Object get(BlockStateContainer container, int x, int y, int z) {
		return container instanceof BiomeContainer biomes ? biomes.biome(x, y, z) : container.get(x, y, z);
	}

	// --- ids, bit storage, codecs --------------------------------------------------------------------------------------

	/**
	 * 1.21 {@code registry.asHolderIdMap()}: the registry's holders by their order in it (a 1.12.2 id map, which the
	 * AccessWidener makes a 1.21 IdMap).
	 */
	public static io.github.xponer2.translator.registry.IdMap<Object> asHolderIdMap(Registry<?> registry) {
		return new LiveHolderIds(registry);
	}

	/**
	 * A registry's holders by their order in it, read live and found by key: a cached map made before a data pack
	 * registry was filled (wolf variants) was empty, and a holder made again was another object than the one put in.
	 * A spawning Twilight Forest wolf took the server down ("Can't find id for wolf_variant pale").
	 */
	private static final class LiveHolderIds implements io.github.xponer2.translator.registry.IdMap<Object> {
		private final Registry<Object> registry;

		@SuppressWarnings("unchecked")
		LiveHolderIds(Registry<?> registry) {
			this.registry = (Registry<Object>) registry;
		}

		private java.util.List<net.minecraft.util.ResourceLocation> keys() {
			return new java.util.ArrayList<>(this.registry.keySet());
		}

		@Override
		public int getId(@Nullable Object value) {
			if (value == null) {
				return DEFAULT;
			}
			var keys = this.keys();
			if (value instanceof io.github.xponer2.translator.registry.Holder<?> holder) {
				var key = holder.unwrapKey();
				if (key.isPresent()) {
					int id = keys.indexOf(key.get().location());
					if (id >= 0) {
						return id;
					}
				}
				try {
					value = holder.value();
				} catch (RuntimeException e) {
					return DEFAULT;
				}
			}
			for (int id = 0; id < keys.size(); id++) {
				if (this.registry.get(keys.get(id)) == value) {
					return id;
				}
			}
			return DEFAULT;
		}

		@Override
		@Nullable
		public Object byId(int id) {
			var keys = this.keys();
			if (id < 0 || id >= keys.size()) {
				return null;
			}
			var location = keys.get(id);
			return new io.github.xponer2.translator.registry.Registry.Reference<>(
					io.github.xponer2.translator.registry.ResourceKey.create(this.registry.key(), location), this.registry.get(location));
		}

		@Override
		public int size() {
			return this.registry.keySet().size();
		}

		@Override
		public java.util.Iterator<Object> iterator() {
			var holders = new java.util.ArrayList<Object>();
			for (int id = 0; id < this.size(); id++) {
				holders.add(this.byId(id));
			}
			return holders.iterator();
		}
	}

	/** 1.21 {@code idMapper.byId(id)} called on the class (the table has it on the interface): null for an unknown id. */
	@Nullable
	public static Object byId(ObjectIntIdentityMap<?> map, int id) {
		return map.getByValue(id);
	}

	/**
	 * 1.21 {@code new SimpleBitStorage(bits, size, data)}: 1.21 packs whole entries into each long (none spans two),
	 * 1.12.2's BitArray packs them end to end: the entries are read the 1.21 way and put into a 1.12.2 array.
	 */
	public static BitArray newBitArray(int bits, int size, @Nullable long[] data) {
		var array = new BitArray(Math.max(1, Math.min(32, bits)), size);
		if (data != null && bits > 0 && bits <= 32) {
			int perLong = 64 / bits;
			long mask = (1L << bits) - 1L;
			for (int i = 0; i < size && i / perLong < data.length; i++) {
				array.setAt(i, (int) (data[i / perLong] >>> i % perLong * bits & mask));
			}
		}
		return array;
	}

	public static BitArray newBitArray(int bits, int size, int[] values) {
		var array = new BitArray(Math.max(1, Math.min(32, bits)), size);
		for (int i = 0; i < size && i < values.length; i++) {
			array.setAt(i, values[i]);
		}
		return array;
	}

	/** 1.21 {@code SimpleBitStorage.get(index)} called on the class (the table has it on BitStorage). */
	public static int get(BitArray array, int index) {
		return array.getAt(index);
	}

	public static void set(BitArray array, int index, int value) {
		array.setAt(index, value);
	}

	public static int getSize(BitArray array) {
		return array.size();
	}

	public static int getBits(BitArray array) {
		long[] data = array.getBackingLongArray();
		return array.size() == 0 ? 0 : (int) Math.min(64L, data.length * 64L / array.size());
	}

	/**
	 * 1.21 {@code ChunkSerializer.makeBiomeCodec(biomeRegistry)}: the codec of a section's biomes (1.21's makes its
	 * PalettedContainer with data 1.12.2's container hasn't).
	 */
	public static Codec<BlockStateContainer> makeBiomeCodec(Registry<Biome> registry) {
		return BIOME_CODECS.computeIfAbsent(registry, r -> {
			@SuppressWarnings("unchecked")
			Registry<Biome> biomes = (Registry<Biome>) r;
			Codec<Holder<Biome>> byName = ResourceLocation_CODEC.xmap(name -> {
				Biome biome = biomes.get(name);
				if (biome == null) {
					biome = net.minecraftforge.fml.common.registry.ForgeRegistries.BIOMES.getValue(name);
				}
				return holder(biome);
			}, holder -> {
				ResourceLocation name = holder.value() == null ? null : holder.value().getRegistryName();
				return name == null ? new ResourceLocation("plains") : name;
			});
			return BiomeContainer.codec(byName, holder(Biomes.PLAINS));
		});
	}

	private static final Codec<ResourceLocation> ResourceLocation_CODEC = Codec.STRING.xmap(ResourceLocation::new, ResourceLocation::toString);

	// --- small ones on the way of a mod's chunk reading ----------------------------------------------------------------

	/** 1.21 {@code dimensionType.fixedTime()}: the Nether and the End have no day (their time stands), the others do. */
	public static OptionalLong fixedTime(@Nullable DimensionType type) {
		if (type == DimensionType.NETHER) {
			return OptionalLong.of(18000L);
		}
		if (type == DimensionType.THE_END) {
			return OptionalLong.of(6000L);
		}
		return OptionalLong.empty();
	}
}
