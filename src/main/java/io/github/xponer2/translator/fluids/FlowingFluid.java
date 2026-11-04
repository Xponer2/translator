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
package io.github.xponer2.translator.fluids;

import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;

/**
 * Facade of the 1.21 {@code FlowingFluid} (NeoForge's BaseFlowingFluid and its Source and Flowing extend it): a
 * {@link FluidBase} with 1.21's falling and level properties. How it flows in the world is its liquid block's.
 */
public abstract class FlowingFluid extends FluidBase {
	public static final PropertyBool FALLING = PropertyBool.create("falling");
	public static final PropertyInteger LEVEL = PropertyInteger.create("level", 1, 8);

	protected FlowingFluid() {
	}

	/** 1.21: the fluid's state properties; the Translator's fluid states don't keep them. */
	protected void createFluidStateDefinition(Object builder) {
	}
}
