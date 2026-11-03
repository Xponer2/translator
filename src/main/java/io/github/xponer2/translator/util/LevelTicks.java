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
package io.github.xponer2.translator.util;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The scheduled ticks of a level (1.21 {@code LevelTickAccess}, neoforge-1.21.1-runtime.tsv): the 1.12.2 world's
 * pending block updates. A fluid has its block's updates.
 */
public interface LevelTicks {
	static LevelTicks of(World world) {
		return () -> world;
	}

	World world();

	static Block blockOf(Object type) {
		if (type instanceof Block block) {
			return block;
		}
		if (type instanceof net.minecraftforge.fluids.Fluid fluid && fluid.getBlock() != null) {
			return fluid.getBlock();
		}
		return null;
	}

	default boolean hasScheduledTick(BlockPos pos, Object type) {
		Block block = blockOf(type);
		return block != null && this.world().isUpdateScheduled(pos, block);
	}

	default boolean willTickThisTick(BlockPos pos, Object type) {
		Block block = blockOf(type);
		return block != null && this.world().isBlockTickPending(pos, block);
	}

	default void schedule(BlockPos pos, Object type, int delay) {
		Block block = blockOf(type);
		if (block != null) {
			this.world().scheduleUpdate(pos, block, delay);
		}
	}

	default int count() {
		return 0;
	}
}
