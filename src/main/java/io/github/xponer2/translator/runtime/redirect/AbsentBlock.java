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

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;

/**
 * A 1.21 vanilla block 1.12.2 hasn't (soul soil, the nyliums, ...) as a mod's {@code Blocks.SOUL_SOIL} reads it: a block
 * of its own, never in a world, so a comparison with it is false. It was air, so every check "the block below is soul
 * soil" held for air (Biomes O' Plenty's burning blossom stayed when its ground was gone and could be placed in the air).
 * Its default state is air's: what a mod places as it ({@code SOUL_SOIL.defaultBlockState()}) is air, as before. Not
 * registered.
 */
public final class AbsentBlock extends Block {
	private final String name;

	AbsentBlock(String name) {
		super(Material.AIR);
		this.name = name;
		this.setDefaultState(Blocks.AIR.getDefaultState());
	}

	/** The 1.21 name of the block this stands for. */
	public String name() {
		return this.name;
	}

	@Override
	public String toString() {
		return "AbsentBlock{" + this.name + "}";
	}
}
