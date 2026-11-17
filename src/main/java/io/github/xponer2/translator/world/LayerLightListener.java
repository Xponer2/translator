/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.world;

import net.minecraft.util.math.BlockPos;

/** 1.21 {@code LayerLightEventListener}: the light of one layer (block or sky) of a world, asked by block. */
@FunctionalInterface
public interface LayerLightListener {
	int getLightValue(BlockPos pos);
}
