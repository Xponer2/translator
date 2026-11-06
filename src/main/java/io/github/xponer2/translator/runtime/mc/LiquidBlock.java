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
	/** Made before its fluid was (null): water stands in until the fluid of its name is known. */
	private boolean standIn;

	public LiquidBlock(Fluid fluid, BlockProperties properties) {
		// the source of a flowing fluid, as 1.21's block holds (its bucket's fluid is the source)
		super(fluid == null ? FluidRegistry.WATER : io.github.xponer2.translator.fluids.FlowingFluid.sourceOf(fluid),
				fluid != null && fluid.getTemperature() > 600 ? Material.LAVA : Material.WATER);
		this.standIn = fluid == null;
		properties.applyTo(this, this::setSoundType);
		// the fluid's block is this one (its bucket places it, Forge's fluid handlers find it), the flowing one's too
		Fluid source = io.github.xponer2.translator.fluids.FlowingFluid.sourceOf(fluid);
		for (Fluid held : new Fluid[] {source, fluid}) {
			if (held != null && held.getBlock() == null) {
				held.setBlock(this);
			}
		}
	}

	@Override
	public Fluid getFluid() {
		if (this.standIn) {
			this.resolve();
		}
		return this.getFluid0();
	}

	/**
	 * The fluid of the block's name ("biomesoplenty:blood": Forge's "biomesoplenty.blood") in place of the water that stood
	 * in: its bucket finds this block, an empty bucket takes it as its own fluid.
	 */
	private void resolve() {
		var id = this.getRegistryName();
		if (id == null) {
			return;
		}
		Fluid found = null;
		for (String name : new String[] {id.getNamespace() + "." + id.getPath(), id.getPath(), id.toString()}) {
			if (found == null && FluidRegistry.getFluid(name) != null && FluidRegistry.getFluid(name) != FluidRegistry.WATER) {
				found = FluidRegistry.getFluid(name);
			}
		}
		if (found == null) {
			return;
		}
		this.standIn = false;
		try {
			var fluidName = net.minecraftforge.fluids.BlockFluidBase.class.getDeclaredField("fluidName");
			fluidName.setAccessible(true);
			fluidName.set(this, found.getName());
			var defined = net.minecraftforge.fluids.BlockFluidBase.class.getDeclaredField("definedFluid");
			defined.setAccessible(true);
			defined.set(this, found);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return;
		}
		if (found.getBlock() == null) {
			found.setBlock(this);
		}
	}

	private Fluid getFluid0() {
		return FluidRegistry.getFluid(this.fluidName);
	}
}
