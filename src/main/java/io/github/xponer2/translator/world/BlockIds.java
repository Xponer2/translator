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

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;

/**
 * Room for more than 4096 blocks, as JustEnoughIDs / RoughlyEnoughIDs give a modpack (the 1.21.1 mods of a big pack
 * have more blocks than Forge 1.12.2 takes: "Invalid id 4096 - maximum id range exceeded"):
 * <ul>
 * <li>the block registry takes ids up to 65535;</li>
 * <li>a chunk section saves the ids' bits 12-15 in a {@code TranslatorAdd2} array next to 1.12.2's {@code Blocks}
 * (bits 0-7) and {@code Add} (8-11), and gets them back when loaded; worlds without such blocks save as before;</li>
 * <li>the world generator's ChunkPrimer keeps the state ids that don't fit in its {@code char}s here (the coremod's
 * {@code BlockIdLimit} calls {@link #primerStore} and {@link #primerLoad}).</li>
 * </ul>
 * The network needs nothing: 1.12.2 sizes chunk data by the largest state id.
 */
public final class BlockIds {
	private static final Logger LOGGER = LogManager.getLogger("Translator");
	public static final int MAX_BLOCK_ID = 65535;
	private static final String ADD2 = "TranslatorAdd2";
	/** A section's block states past their metadata ({@link StateIds}): Palette (name, properties), Entries. */
	private static final String STATES = "TranslatorStates";
	/** The state ids over 0xFFFF of the ChunkPrimers that have any, by their data array; 0xFFFF in the array marks them. */
	private static final Map<char[], Int2IntOpenHashMap> HIGH = Collections.synchronizedMap(new WeakHashMap<>());

	private BlockIds() {
	}

	/** The block registry's limit raised (pre-init, before the registry events). */
	public static void raiseLimit() {
		if (net.minecraftforge.fml.common.Loader.isModLoaded("jeid") || net.minecraftforge.fml.common.Loader.isModLoaded("reid")) {
			LOGGER.info("Block ids: extended by JustEnoughIDs / RoughlyEnoughIDs");
			return;
		}
		try {
			var field = ForgeRegistry.class.getDeclaredField("max");
			field.setAccessible(true);
			var blocks = (ForgeRegistry<Block>) ForgeRegistries.BLOCKS;
			int before = field.getInt(blocks);
			if (before < MAX_BLOCK_ID) {
				field.setInt(blocks, MAX_BLOCK_ID);
				LOGGER.info("Block ids: up to {} (Forge 1.12.2 takes {})", MAX_BLOCK_ID, before);
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			LOGGER.error("Can't raise the block id limit: more than 4096 blocks will fail to register", e);
		}
	}

	/** Whether any block has an id past 4095 (else the chunk save format stays 1.12.2's own): known per world load. */
	private static volatile boolean anyHigh;

	private static boolean computeHigh() {
		for (Block block : ForgeRegistries.BLOCKS) {
			if (Block.getIdFromBlock(block) > 4095) {
				return true;
			}
		}
		return false;
	}

	// --- Block.getStateId / getStateById -----------------------------------------------------------------------------------

	/** Set for the packing of blocks past 4095 (block id in bits 0-15, meta in 16-19); others pack as in 1.12.2. */
	private static final int HIGH_STATE = 1 << 30;
	/** Set for a state with an extra id ({@link StateIds}: past its metadata): that id in the bits below. */
	private static final int EXTRA_STATE = 1 << 29;

	public static int stateId(IBlockState state) {
		if (StateIds.isExtra(state)) {
			return EXTRA_STATE | Block.BLOCK_STATE_IDS.get(state);
		}
		Block block = state.getBlock();
		int id = Block.getIdFromBlock(block);
		int meta = block.getMetaFromState(state);
		return id <= 4095 ? id + (meta << 12) : HIGH_STATE | id | meta << 16;
	}

	@SuppressWarnings("deprecation")
	public static IBlockState stateById(int id) {
		if ((id & EXTRA_STATE) != 0 && (id & HIGH_STATE) == 0) {
			IBlockState state = Block.BLOCK_STATE_IDS.getByValue(id & ~EXTRA_STATE);
			if (state != null) {
				return state;
			}
		}
		if ((id & HIGH_STATE) != 0) {
			return Block.getBlockById(id & 0xFFFF).getStateFromMeta(id >> 16 & 15);
		}
		return Block.getBlockById(id & 4095).getStateFromMeta(id >> 12 & 15);
	}

	// --- ChunkPrimer ----------------------------------------------------------------------------------------------------

	public static void primerStore(char[] data, int index, int id) {
		if (id >= 0xFFFF) {
			HIGH.computeIfAbsent(data, k -> new Int2IntOpenHashMap()).put(index, id);
			data[index] = 0xFFFF;
			return;
		}
		if (data[index] == 0xFFFF) {
			var high = HIGH.get(data);
			if (high != null) {
				high.remove(index);
			}
		}
		data[index] = (char) id;
	}

	public static int primerLoad(char[] data, int index) {
		char id = data[index];
		if (id != 0xFFFF) {
			return id;
		}
		var high = HIGH.get(data);
		return high != null && high.containsKey(index) ? high.get(index) : id;
	}

	// --- chunk save and load ----------------------------------------------------------------------------------------------

	public static final class Saving {
		/** The ids a world uses are set when it loads (a saved world's own ids). */
		@SubscribeEvent
		public void onWorldLoad(net.minecraftforge.event.world.WorldEvent.Load event) {
			anyHigh = computeHigh();
		}

		@SubscribeEvent
		public void onSave(ChunkDataEvent.Save event) {
			boolean extra = StateIds.any();
			if (!anyHigh && !extra) {
				return;
			}
			NBTTagList sections = event.getData().getCompoundTag("Level").getTagList("Sections", 10);
			for (int i = 0; i < sections.tagCount(); i++) {
				NBTTagCompound section = sections.getCompoundTagAt(i);
				ExtendedBlockStorage storage = storage(event.getChunk().getBlockStorageArray(), section.getByte("Y"));
				if (storage == null) {
					continue;
				}
				NibbleArray add2 = null;
				// the states past their metadata: a palette of them (by name and properties) and palette << 12 | index
				Map<IBlockState, Integer> palette = null;
				it.unimi.dsi.fastutil.ints.IntArrayList entries = null;
				for (int index = 0; index < 4096; index++) {
					int x = index & 15;
					int y = index >> 8 & 15;
					int z = index >> 4 & 15;
					IBlockState state = storage.get(x, y, z);
					if (anyHigh) {
						int high = StateIds.saveId(Block.BLOCK_STATE_IDS, state) >> 16 & 15;
						if (high != 0) {
							if (add2 == null) {
								add2 = new NibbleArray();
							}
							add2.set(x, y, z, high);
						}
					}
					if (extra && StateIds.isExtra(state)) {
						if (palette == null) {
							palette = new java.util.IdentityHashMap<>();
							entries = new it.unimi.dsi.fastutil.ints.IntArrayList();
						}
						Integer at = palette.get(state);
						if (at == null) {
							at = palette.size();
							palette.put(state, at);
						}
						entries.add(at << 12 | index);
					}
				}
				if (add2 != null) {
					section.setByteArray(ADD2, add2.getData());
				}
				if (palette != null) {
					var states = new NBTTagCompound();
					var list = new NBTTagList();
					IBlockState[] ordered = new IBlockState[palette.size()];
					palette.forEach((state, at) -> ordered[at] = state);
					for (IBlockState state : ordered) {
						list.appendTag(net.minecraft.nbt.NBTUtil.writeBlockState(new NBTTagCompound(), state));
					}
					states.setTag("Palette", list);
					states.setIntArray("Entries", entries.toIntArray());
					section.setTag(STATES, states);
				}
			}
		}

		@SubscribeEvent
		public void onLoad(ChunkDataEvent.Load event) {
			NBTTagList sections = event.getData().getCompoundTag("Level").getTagList("Sections", 10);
			for (int i = 0; i < sections.tagCount(); i++) {
				NBTTagCompound section = sections.getCompoundTagAt(i);
				boolean high = section.hasKey(ADD2, 7);
				boolean extra = section.hasKey(STATES, 10);
				if (!high && !extra) {
					continue;
				}
				ExtendedBlockStorage storage = storage(event.getChunk().getBlockStorageArray(), section.getByte("Y"));
				if (storage == null) {
					continue;
				}
				if (high) {
					loadHigh(section, storage);
				}
				if (extra) {
					loadStates(section.getCompoundTag(STATES), storage);
				}
			}
		}

		private static void loadHigh(NBTTagCompound section, ExtendedBlockStorage storage) {
			var add2 = new NibbleArray(section.getByteArray(ADD2));
			byte[] blocks = section.getByteArray("Blocks");
			var data = new NibbleArray(section.getByteArray("Data"));
			NibbleArray add = section.hasKey("Add", 7) ? new NibbleArray(section.getByteArray("Add")) : null;
			for (int index = 0; index < 4096; index++) {
				int x = index & 15;
				int y = index >> 8 & 15;
				int z = index >> 4 & 15;
				int high = add2.get(x, y, z);
				if (high == 0) {
					continue;
				}
				int block = high << 12 | (add == null ? 0 : add.get(x, y, z)) << 8 | blocks[index] & 255;
				IBlockState state = Block.BLOCK_STATE_IDS.getByValue(block << 4 | data.get(x, y, z));
				if (state != null) {
					storage.set(x, y, z, state);
				}
			}
		}

		/**
		 * The full states of the section's blocks past their metadata, where the block there is still the one saved (a
		 * mod gone or changed: the block of the metadata stays).
		 */
		private static void loadStates(NBTTagCompound states, ExtendedBlockStorage storage) {
			NBTTagList list = states.getTagList("Palette", 10);
			IBlockState[] palette = new IBlockState[list.tagCount()];
			for (int i = 0; i < palette.length; i++) {
				try {
					palette[i] = net.minecraft.nbt.NBTUtil.readBlockState(list.getCompoundTagAt(i));
				} catch (RuntimeException e) {
					palette[i] = null;
				}
			}
			for (int entry : states.getIntArray("Entries")) {
				int at = entry >>> 12;
				int index = entry & 4095;
				IBlockState state = at < palette.length ? palette[at] : null;
				if (state == null) {
					continue;
				}
				int x = index & 15;
				int y = index >> 8 & 15;
				int z = index >> 4 & 15;
				if (storage.get(x, y, z).getBlock() == state.getBlock()) {
					storage.set(x, y, z, state);
				}
			}
		}

		private static ExtendedBlockStorage storage(ExtendedBlockStorage[] storages, int sectionY) {
			for (ExtendedBlockStorage storage : storages) {
				if (storage != null && storage.getYLocation() >> 4 == sectionY) {
					return storage;
				}
			}
			return null;
		}
	}
}
