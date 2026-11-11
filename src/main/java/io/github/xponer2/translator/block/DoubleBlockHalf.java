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
package io.github.xponer2.translator.block;

import net.minecraft.util.IStringSerializable;

/** The half of a two blocks high block (1.21 {@code DoubleBlockHalf}, neoforge-1.21.1-runtime.tsv). */
public enum DoubleBlockHalf implements IStringSerializable {
	UPPER,
	LOWER;

	@Override
	public String getName() {
		return this == UPPER ? "upper" : "lower";
	}

	public String getSerializedName() {
		return this.getName();
	}

	/** 1.21 {@code getDirectionToOther()}: where the other half is (Iron's Spells' portal frame). */
	public net.minecraft.util.EnumFacing getDirectionToOther() {
		return this == UPPER ? net.minecraft.util.EnumFacing.DOWN : net.minecraft.util.EnumFacing.UP;
	}

	/** 1.21 {@code getOtherHalf()}. */
	public DoubleBlockHalf getOtherHalf() {
		return this == UPPER ? LOWER : UPPER;
	}
}
