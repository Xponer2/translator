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
package io.github.xponer2.translator.tags;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;

/**
 * Fluid tags (1.21 {@code FluidTags}): 1.12.2 fluids are blocks, so the water tag is the water material.
 */
public final class FluidTags {
    public static final Fluid WATER = state -> state.getMaterial() == Material.WATER;
    public static final Fluid LAVA = state -> state.getMaterial() == Material.LAVA;

    private FluidTags() { }

    @FunctionalInterface
    public interface Fluid {
        boolean test(IBlockState state);
    }
}
