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
package io.github.xponer2.translator.runtime.redirect;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;

/**
 * {@code Blocks.X.defaultBlockState()} of a 1.21 block that is a variant of a 1.12.2 block (the flattening: granite is
 * stone with metadata 1, coarse dirt dirt 1, the coloured terracottas stained hardened clay, a fern tall grass 2): the
 * converter makes the call this, with the 1.21 name. Without it the block's own default state was placed (Biomes O'
 * Plenty's badlands of white terracotta, plain stone for granite, dead bushes for short grass).
 */
public final class FlattenedStates {
	private static final Map<String, IBlockState> STATES = new ConcurrentHashMap<>();

	private FlattenedStates() {
	}

	/** The 1.21 block's default state (the flattening's metadata); the block's own when the name gives no other. */
	public static IBlockState defaultState(Block block, String name) {
		IBlockState known = STATES.get(name);
		if (known != null && known.getBlock() == block) {
			return known;
		}
		IBlockState state;
		try {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString("Name", name);
			state = io.github.xponer2.translator.world.BlockStateNbt.read(tag);
		} catch (RuntimeException e) {
			state = null;
		}
		if (state == null || state.getBlock() != block) {
			// a block the flattening doesn't know, or one not registered yet: the block's own default
			return block.getDefaultState();
		}
		STATES.put(name, state);
		return state;
	}
}
