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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.xponer2.translator.registry.Holder;

import net.minecraft.block.state.IBlockState;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.BlockStateContainer;

/**
 * The biomes of a chunk section as 1.21 keeps them: a 1.21 {@code PalettedContainer<Holder<Biome>>} of 4 by 4 by 4
 * cells (each 4 blocks wide). 1.21's PalettedContainer is 1.12.2's BlockStateContainer in the tables, so this is one
 * too; its block states are never used. 1.12.2 keeps one biome per column of a chunk: a section read from a live chunk
 * has the same biome at every height.
 */
public final class BiomeContainer extends BlockStateContainer {
	public static final int CELLS = 64;

	private final Holder<Biome>[] cells;

	@SuppressWarnings("unchecked")
	public BiomeContainer(Holder<Biome> fill) {
		// the block state storage 1.12.2's constructor made isn't used: let go (2 KB a section)
		this.storage = null;
		this.palette = null;
		this.cells = new Holder[CELLS];
		Arrays.fill(this.cells, fill);
	}

	private static int index(int x, int y, int z) {
		return (y & 3) << 4 | (z & 3) << 2 | x & 3;
	}

	public Holder<Biome> biome(int x, int y, int z) {
		return this.cells[index(x, y, z)];
	}

	public void biome(int x, int y, int z, Holder<Biome> biome) {
		if (biome != null) {
			this.cells[index(x, y, z)] = biome;
		}
	}

	/** 1.12.2's block state getter: this container holds none. */
	@Override
	public IBlockState get(int x, int y, int z) {
		return AIR_BLOCK_STATE;
	}

	@Override
	public void set(int x, int y, int z, IBlockState state) {
	}

	@Override
	protected IBlockState get(int index) {
		return AIR_BLOCK_STATE;
	}

	@Override
	protected void set(int index, IBlockState state) {
	}

	@Override
	public int onResize(int bits, IBlockState state) {
		return 0;
	}

	@Override
	public void read(net.minecraft.network.PacketBuffer buf) {
	}

	@Override
	public void write(net.minecraft.network.PacketBuffer buf) {
	}

	@Override
	public int getSerializedSize() {
		return 0;
	}

	// --- 1.21's saved form: {palette: [biome names], data: packed indices (left out for one biome)} -------------------

	/** The codec of a section's biomes as 1.21 saves them, biomes by the holder codec of their registry. */
	public static Codec<BlockStateContainer> codec(Codec<Holder<Biome>> biome, Holder<Biome> fallback) {
		return RecordCodecBuilder.<Saved>create(instance -> instance.group(
				biome.listOf().fieldOf("palette").forGetter(saved -> saved.palette),
				Codec.LONG_STREAM.optionalFieldOf("data").forGetter(saved -> saved.data.map(LongStream::of))
		).apply(instance, (palette, data) -> new Saved(palette, data.map(LongStream::toArray))))
				.comapFlatMap(saved -> unpack(saved, fallback), container -> pack(container, fallback));
	}

	private static final class Saved {
		final List<Holder<Biome>> palette;
		final Optional<long[]> data;

		Saved(List<Holder<Biome>> palette, Optional<long[]> data) {
			this.palette = palette;
			this.data = data;
		}
	}

	private static int bits(int paletteSize) {
		// 1.21 SECTION_BIOMES: no data for one value, else the bits the palette needs (entries don't span longs)
		return paletteSize <= 1 ? 0 : 32 - Integer.numberOfLeadingZeros(paletteSize - 1);
	}

	private static Saved pack(BlockStateContainer container, Holder<Biome> fallback) {
		var palette = new ArrayList<Holder<Biome>>();
		int[] indices = new int[CELLS];
		if (container instanceof BiomeContainer biomes) {
			for (int i = 0; i < CELLS; i++) {
				Holder<Biome> holder = biomes.cells[i];
				int index = -1;
				for (int j = 0; j < palette.size(); j++) {
					if (palette.get(j) == holder || palette.get(j).value() == holder.value()) {
						index = j;
						break;
					}
				}
				if (index < 0) {
					index = palette.size();
					palette.add(holder);
				}
				indices[i] = index;
			}
		} else {
			palette.add(fallback);
		}
		int bits = bits(palette.size());
		if (bits == 0) {
			return new Saved(palette, Optional.empty());
		}
		int perLong = 64 / bits;
		long[] data = new long[(CELLS + perLong - 1) / perLong];
		for (int i = 0; i < CELLS; i++) {
			data[i / perLong] |= (long) indices[i] << i % perLong * bits;
		}
		return new Saved(palette, Optional.of(data));
	}

	private static DataResult<BlockStateContainer> unpack(Saved saved, Holder<Biome> fallback) {
		if (saved.palette.isEmpty()) {
			return DataResult.success(new BiomeContainer(fallback));
		}
		var container = new BiomeContainer(saved.palette.get(0));
		int bits = bits(saved.palette.size());
		if (bits == 0 || !saved.data.isPresent()) {
			return DataResult.success(container);
		}
		long[] data = saved.data.get();
		int perLong = 64 / bits;
		long mask = (1L << bits) - 1L;
		for (int i = 0; i < CELLS && i / perLong < data.length; i++) {
			int index = (int) (data[i / perLong] >>> i % perLong * bits & mask);
			container.cells[i] = index < saved.palette.size() ? saved.palette.get(index) : fallback;
		}
		return DataResult.success(container);
	}
}
