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
package io.github.xponer2.translator.util;

import java.util.Locale;

/**
 * The 1.21 serialized names of 1.12.2 enums that 1.21 has as StringRepresentable and 1.12.2 not as IStringSerializable
 * (Rotation, Mirror): the AccessWidener gives them IStringSerializable with this as getName(). A mod's
 * {@code EnumProperty.create("rotate_tile", Rotation.class)} threw in 1.12.2's PropertyEnum (each value is cast to
 * IStringSerializable): Supplementaries' block properties after it stayed null and its cannon balls, pancakes, book
 * piles and end lamps failed.
 */
public final class VanillaEnumNames {
	private VanillaEnumNames() {
	}

	public static String name(Enum<?> value) {
		if (value instanceof net.minecraft.util.Rotation && value == net.minecraft.util.Rotation.CLOCKWISE_180) {
			return "180";
		}
		return value.name().toLowerCase(Locale.ROOT);
	}
}
