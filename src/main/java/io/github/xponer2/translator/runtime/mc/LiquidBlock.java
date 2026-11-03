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
package io.github.xponer2.translator.runtime.mc;

import io.github.xponer2.translator.block.BlockProperties;

import net.minecraft.block.material.Material;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

/**
 * Facade of the 1.21 {@code LiquidBlock} (a mod's fluid in the world): a Forge fluid block of the fluid (it flows the
 * Forge way), lava-like when the fluid is hot.
 */
public class LiquidBlock extends BlockFluidClassic {
	public LiquidBlock(Fluid fluid, BlockProperties properties) {
		super(fluid == null ? FluidRegistry.WATER : fluid, fluid != null && fluid.getTemperature() > 600 ? Material.LAVA : Material.WATER);
		properties.applyTo(this, this::setSoundType);
	}

	public Fluid getFluid() {
		return this.getFluid0();
	}

	private Fluid getFluid0() {
		return FluidRegistry.getFluid(this.fluidName);
	}
}
