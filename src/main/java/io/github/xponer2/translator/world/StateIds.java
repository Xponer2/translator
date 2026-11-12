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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ObjectIntIdentityMap;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * Every block state of the 1.21.1 mods' blocks with its own id. 1.12.2 numbers a block's states by its metadata (4 bits,
 * {@code Block.BLOCK_STATE_IDS}): the states of a 1.21 block with more properties than fit there shared one id, so a
 * chunk section in the global palette (more than 256 different states), the network and the save gave back another
 * state of the same metadata (Macaw's fence gates never stayed open: their OPEN didn't fit, and opening one opened its
 * neighbour forever, StackOverflowError). The states metadata can't tell apart get extra ids after all the regular ones
 * (dense, in block id order: the same on a server and its clients with the same mods and block ids), assigned again
 * whenever the block ids change ({@code FMLModIdMappingEvent}: frozen, a world's ids, a server's ids, back).
 * <ul>
 * <li>memory and network: the palettes and block change packets write {@code BLOCK_STATE_IDS} ids, the extra ones too;
 * the global palette is sized by the largest id, not by the count (the coremod's {@code StateIdsTransformer} calls
 * {@link #paletteSize});</li>
 * <li>save: the section keeps the state's regular id + metadata ({@link #saveId}), the full states go next to it
 * ({@link BlockIds.Saving}, {@code TranslatorStates}); a world without them loads as before.</li>
 * </ul>
 */
public final class StateIds {
	private static final Logger LOGGER = LogManager.getLogger("Translator");
	/** Each state with an extra id -> the state its metadata gives (its regular id). Replaced whole when assigned. */
	private static volatile Map<IBlockState, IBlockState> canonical = Collections.emptyMap();
	/** One past the largest id in use (regular or extra): the global palette's size. */
	private static volatile int end;

	private StateIds() {
	}

	/** The extra ids, once more (the block ids changed). */
	@SuppressWarnings("deprecation")
	public static synchronized void assign() {
		if (net.minecraftforge.fml.common.Loader.isModLoaded("jeid") || net.minecraftforge.fml.common.Loader.isModLoaded("reid")) {
			// JustEnoughIDs / RoughlyEnoughIDs number the states their own way (the coremod leaves their palettes too)
			return;
		}
		ObjectIntIdentityMap<IBlockState> ids = Block.BLOCK_STATE_IDS;
		List<Block> blocks = new ArrayList<>();
		int largest = 0;
		for (Block block : ForgeRegistries.BLOCKS) {
			blocks.add(block);
			largest = Math.max(largest, Block.getIdFromBlock(block) << 4 | 15);
		}
		blocks.sort((a, b) -> Integer.compare(Block.getIdFromBlock(a), Block.getIdFromBlock(b)));
		Map<IBlockState, IBlockState> made = new IdentityHashMap<>();
		int next = largest + 1;
		int converted = 0;
		for (Block block : blocks) {
			if (!converted(block)) {
				continue;
			}
			List<IBlockState> canonicals = new ArrayList<>();
			for (IBlockState state : block.getBlockState().getValidStates()) {
				IBlockState meta;
				try {
					meta = block.getStateFromMeta(block.getMetaFromState(state));
				} catch (RuntimeException e) {
					continue;
				}
				if (meta == state) {
					continue;
				}
				ids.put(state, next++);
				made.put(state, meta);
				if (!canonicals.contains(meta)) {
					canonicals.add(meta);
				}
			}
			// a regular id names its metadata's state again (Forge put every state of that metadata there, the last won)
			int id = Block.getIdFromBlock(block);
			for (IBlockState meta : canonicals) {
				ids.put(meta, id << 4 | block.getMetaFromState(meta));
			}
			if (!canonicals.isEmpty()) {
				converted++;
			}
		}
		canonical = made;
		end = Math.max(next, largest + 1);
		LOGGER.info("Block states: {} with an extra id ({} blocks), ids up to {}", made.size(), converted, end - 1);
	}

	/** A block of a 1.21.1 mod or a carried 1.21 block: its class extends a Translator class. */
	private static boolean converted(Block block) {
		for (Class<?> type = block.getClass(); type != null && type != Block.class; type = type.getSuperclass()) {
			if (type.getName().startsWith("io.github.xponer2.translator.")) {
				return true;
			}
		}
		return false;
	}

	/** Whether any state has an extra id. */
	public static boolean any() {
		return !canonical.isEmpty();
	}

	/** Whether the state has an extra id. */
	public static boolean isExtra(IBlockState state) {
		return canonical.containsKey(state);
	}

	/** The state of the state's metadata: itself, or the state its regular id names. */
	public static IBlockState metaState(IBlockState state) {
		IBlockState meta = canonical.get(state);
		return meta == null ? state : meta;
	}

	/** The id a chunk section saves for the state: its metadata's (1.12.2's format has 4 bits for it). */
	@SuppressWarnings("unchecked")
	public static int saveId(Object ids, Object state) {
		IBlockState meta = state instanceof IBlockState ? canonical.get(state) : null;
		return ((ObjectIntIdentityMap<Object>) ids).get(meta == null ? state : meta);
	}

	/** The size of the global palette: one past the largest id (1.12.2 took the number of states, which is less). */
	public static int paletteSize(Object ids) {
		return Math.max(((ObjectIntIdentityMap<?>) ids).size(), end);
	}
}
