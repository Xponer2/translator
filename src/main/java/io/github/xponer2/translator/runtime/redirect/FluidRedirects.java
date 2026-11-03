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

import io.github.xponer2.translator.util.FluidState;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

/**
 * 1.21 fluids on 1.12.2 (runtime): a 1.21 {@code Fluid} is the Forge {@link Fluid}, {@code Fluids} is
 * {@link FluidRegistry} (WATER, LAVA) plus the constants here, a fluid state is {@link FluidState}.
 */
public final class FluidRedirects {
	// --- Fluids (static fields 1.12.2's FluidRegistry lacks) -----------------------------------------------------------

	public static final Fluid EMPTY = FluidState.EMPTY_FLUID;
	public static final Fluid FLOWING_WATER = FluidRegistry.WATER;
	public static final Fluid FLOWING_LAVA = FluidRegistry.LAVA;

	private FluidRedirects() {
	}

	// --- BlockState / BlockGetter -------------------------------------------------------------------------------------

	public static FluidState getFluidState(IBlockState state) {
		return FluidState.of(state);
	}

	public static FluidState getFluidState(IBlockAccess world, BlockPos pos) {
		return FluidState.of(world.getBlockState(pos));
	}

	// --- Fluid --------------------------------------------------------------------------------------------------------

	/** Still and flowing water are one Forge fluid. */
	public static boolean isSame(Fluid fluid, Fluid other) {
		return fluid == other;
	}

	public static boolean isSource(Fluid fluid, FluidState state) {
		return state.isSource();
	}
}
