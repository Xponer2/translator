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

import java.util.UUID;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagLongArray;

/**
 * The 1.21 array tags ({@code IntArrayTag}, {@code ByteArrayTag}, {@code LongArrayTag}) are 1.12.2's: what 1.21 adds
 * to them (the list view, the long array getter) and the UUIDs 1.21 writes as int arrays ({@code NbtUtils.createUUID},
 * {@code loadUUID}). They were carried 1.21 classes before, which aren't 1.12.2 tags: a mod putting one in a compound
 * didn't verify and its save was dropped (Sophisticated Backpacks' contents, Mekanism, AE2, Create, Framework,
 * Torchmaster), and a tag read back was never one of them.
 */
public final class NbtArrays {
	private NbtArrays() {
	}

	/** 1.21 {@code LongArrayTag.getAsLongArray()}: 1.12.2 keeps the array without a getter. */
	public static long[] getAsLongArray(NBTTagLongArray tag) {
		if (LONGS != null) {
			try {
				return (long[]) LONGS.get(tag);
			} catch (IllegalAccessException e) {
				// below
			}
		}
		return new long[0];
	}

	private static final java.lang.reflect.Field LONGS = longsField();

	private static java.lang.reflect.Field longsField() {
		for (var field : NBTTagLongArray.class.getDeclaredFields()) {
			if (field.getType() == long[].class) {
				field.setAccessible(true);
				return field;
			}
		}
		return null;
	}

	public static int size(NBTTagIntArray tag) {
		return tag.getIntArray().length;
	}

	public static int size(NBTTagByteArray tag) {
		return tag.getByteArray().length;
	}

	public static int size(NBTTagLongArray tag) {
		return getAsLongArray(tag).length;
	}

	public static boolean isEmpty(NBTTagIntArray tag) {
		return size(tag) == 0;
	}

	public static boolean isEmpty(NBTTagByteArray tag) {
		return size(tag) == 0;
	}

	public static boolean isEmpty(NBTTagLongArray tag) {
		return size(tag) == 0;
	}

	/** 1.21 {@code NbtUtils.createUUID(uuid)}: the UUID as four ints (1.21's {@code UUIDUtil.uuidToIntArray}). */
	public static NBTTagIntArray createUUID(UUID uuid) {
		long most = uuid.getMostSignificantBits();
		long least = uuid.getLeastSignificantBits();
		return new NBTTagIntArray(new int[] {(int) (most >> 32), (int) most, (int) (least >> 32), (int) least});
	}

	/**
	 * 1.21 {@code NbtUtils.loadUUID(tag)} (the table names it {@code getUUIDFromTag}): four ints, or 1.12.2's compound of
	 * M and L.
	 */
	public static UUID getUUIDFromTag(NBTBase tag) {
		if (tag instanceof NBTTagIntArray array && array.getIntArray().length == 4) {
			int[] ints = array.getIntArray();
			return new UUID((long) ints[0] << 32 | ints[1] & 0xFFFFFFFFL, (long) ints[2] << 32 | ints[3] & 0xFFFFFFFFL);
		}
		if (tag instanceof NBTTagCompound compound) {
			return net.minecraft.nbt.NBTUtil.getUUIDFromTag(compound);
		}
		throw new IllegalArgumentException("Expected UUID-Tag to be of type IntArray with length 4, but found " + tag);
	}
}
