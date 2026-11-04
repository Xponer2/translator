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

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 1.21 {@code LevelLightEngine} of a world ({@code level.getLightEngine()}): 1.12.2 relights a block with
 * {@code World.checkLight} and keeps no queue of its own (AE2's cable buses relight when their parts change).
 */
public final class LevelLightEngine {
	private final World world;

	public LevelLightEngine(World world) {
		this.world = world;
	}

	public void checkBlock(BlockPos pos) {
		this.world.checkLight(pos);
	}

	public int getRawBrightness(BlockPos pos, int skyDarken) {
		return this.world.getLightFromNeighbors(pos);
	}

	public boolean hasLightWork() {
		return false;
	}

	public int runLightUpdates() {
		return 0;
	}
}
