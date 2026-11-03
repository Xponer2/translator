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
package io.github.xponer2.translator.lwjgl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import sun.misc.Unsafe;

/**
 * Raw native memory for the LWJGL 3 memory API on 1.12.2 (LWJGL 2 has none): {@link Unsafe} allocates, reads and
 * writes addresses, and wraps an address in a direct {@link ByteBuffer} as LWJGL 3 does.
 */
public final class Native {
	public static final Unsafe UNSAFE;
	public static final long BYTE_ARRAY_OFFSET;
	private static final long BUFFER_ADDRESS;
	private static final Constructor<?> DIRECT_BUFFER;

	static {
		try {
			Field field = Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			UNSAFE = (Unsafe) field.get(null);
			BYTE_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
			BUFFER_ADDRESS = UNSAFE.objectFieldOffset(Buffer.class.getDeclaredField("address"));
			DIRECT_BUFFER = Class.forName("java.nio.DirectByteBuffer").getDeclaredConstructor(long.class, int.class);
			DIRECT_BUFFER.setAccessible(true);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private Native() {
	}

	/** The address of a direct buffer's first element (not its position). */
	public static long address(Buffer buffer) {
		if (!buffer.isDirect()) {
			throw new IllegalArgumentException("Not a direct buffer: " + buffer);
		}
		return UNSAFE.getLong(buffer, BUFFER_ADDRESS);
	}

	/** A direct buffer (native byte order) over {@code capacity} bytes at {@code address}; the memory isn't owned by it. */
	public static ByteBuffer wrap(long address, int capacity) {
		if (address == 0L) {
			return null;
		}
		try {
			return ((ByteBuffer) DIRECT_BUFFER.newInstance(address, capacity)).order(ByteOrder.nativeOrder());
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Can't wrap native memory in a buffer", e);
		}
	}

	public static long allocate(long bytes, boolean clear) {
		long address = UNSAFE.allocateMemory(Math.max(1L, bytes));
		if (clear) {
			UNSAFE.setMemory(address, Math.max(1L, bytes), (byte) 0);
		}
		return address;
	}
}
