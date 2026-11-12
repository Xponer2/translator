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

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidTank;

/**
 * NeoForge's FluidTank on Forge 1.12.2's (the table maps the class): the members NeoForge added or gave a registry
 * lookup. Refurbished Furniture's sinks, basins, toilets and kitchen blocks kept, saved and read nothing in their tanks.
 * The saved form is Forge's (the tank is read and written here only).
 */
public final class FluidTankBridges {
	private FluidTankBridges() {
	}

	public static boolean isEmpty(FluidTank tank) {
		return tank.getFluid() == null || tank.getFluidAmount() <= 0;
	}

	/** NeoForge {@code getSpace()}: how much more fits. */
	public static int getSpace(FluidTank tank) {
		return Math.max(0, tank.getCapacity() - tank.getFluidAmount());
	}

	/** NeoForge {@code setCapacity(capacity)} returns the tank (Forge's returns nothing). */
	public static FluidTank setCapacity(FluidTank tank, int capacity) {
		tank.setCapacity(capacity);
		return tank;
	}

	/** NeoForge {@code readFromNBT(registries, tag)}. */
	public static FluidTank readFromNBT(FluidTank tank, Object registries, NBTTagCompound tag) {
		return tank.readFromNBT(tag);
	}

	/** NeoForge {@code writeToNBT(registries, tag)}. */
	public static NBTTagCompound writeToNBT(FluidTank tank, Object registries, NBTTagCompound tag) {
		return tank.writeToNBT(tag);
	}
}
