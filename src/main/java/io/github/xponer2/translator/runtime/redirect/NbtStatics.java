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

import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;

/** The 1.21 {@code XTag.valueOf(value)} factories (1.12.2 has the constructors). */
public final class NbtStatics {
	private NbtStatics() {
	}

	public static NBTTagInt valueOf(int value) {
		return new NBTTagInt(value);
	}

	public static NBTTagLong valueOf(long value) {
		return new NBTTagLong(value);
	}

	public static NBTTagShort valueOf(short value) {
		return new NBTTagShort(value);
	}

	public static NBTTagByte valueOf(byte value) {
		return new NBTTagByte(value);
	}

	public static NBTTagByte valueOf(boolean value) {
		return new NBTTagByte((byte) (value ? 1 : 0));
	}

	public static NBTTagFloat valueOf(float value) {
		return new NBTTagFloat(value);
	}

	public static NBTTagDouble valueOf(double value) {
		return new NBTTagDouble(value);
	}

	public static NBTTagString valueOf(String value) {
		return new NBTTagString(value);
	}
}
