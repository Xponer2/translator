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

import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.material.Material;

/** Facade of the 1.21 {@code PressurePlateBlock}: a 1.12.2 plate (everything presses a wooden one, mobs a stone one). */
public class PressurePlateBlock extends BlockPressurePlate {
	public PressurePlateBlock(Object blockSetType, BlockProperties properties) {
		super(properties.material(), properties.material() == Material.WOOD ? Sensitivity.EVERYTHING : Sensitivity.MOBS);
		properties.applyTo(this, this::setSoundType);
	}
}
