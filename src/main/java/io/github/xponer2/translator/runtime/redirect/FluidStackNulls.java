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

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * The members of NeoForge's FluidStack that Forge 1.12.2's has too, for a null stack: NeoForge's empty stack is
 * FluidStack.EMPTY, Forge's is null (a tank with nothing in it gives null), and the mods call them on whatever a tank
 * gave (X lines: every call of the mods goes here). Refurbished Furniture's toilets, sinks and basins threw at
 * {@code tank.getFluid().getFluid()} on an empty tank.
 */
public final class FluidStackNulls {
	private FluidStackNulls() {
	}

	/** The fluid; for no stack the empty fluid (NeoForge's Fluids.EMPTY). */
	public static Fluid getFluid(@Nullable FluidStack stack) {
		return stack == null ? io.github.xponer2.translator.util.FluidState.EMPTY_FLUID : stack.getFluid();
	}

	@Nullable
	public static FluidStack copy(@Nullable FluidStack stack) {
		return stack == null ? null : stack.copy();
	}

	public static boolean isFluidEqual(@Nullable FluidStack stack, @Nullable FluidStack other) {
		if (stack == null || stack.amount <= 0) {
			return other == null || other.amount <= 0;
		}
		return stack.isFluidEqual(other);
	}

	public static boolean containsFluid(@Nullable FluidStack stack, @Nullable FluidStack other) {
		return stack == null ? other == null || other.amount <= 0 : stack.containsFluid(other);
	}

	public static boolean isFluidStackIdentical(@Nullable FluidStack stack, @Nullable FluidStack other) {
		return stack == null ? other == null || other.amount <= 0 : stack.isFluidStackIdentical(other);
	}

	public static String getLocalizedName(@Nullable FluidStack stack) {
		return stack == null ? "" : stack.getLocalizedName();
	}

	public static String getUnlocalizedName(@Nullable FluidStack stack) {
		return stack == null ? "" : stack.getUnlocalizedName();
	}

	/** Writes the stack into the tag (nothing for no stack). */
	public static NBTTagCompound writeToNBT(@Nullable FluidStack stack, NBTTagCompound tag) {
		return stack == null ? tag : stack.writeToNBT(tag);
	}
}
