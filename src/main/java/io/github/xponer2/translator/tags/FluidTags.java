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
