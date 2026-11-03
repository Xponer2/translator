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

import static io.github.xponer2.translator.lwjgl.Native.UNSAFE;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nullable;

/**
 * LWJGL 3's {@code org.lwjgl.system.MemoryUtil} on 1.12.2: native memory by address and in direct buffers (the
 * 1.21.1 mods fill vertex data, images and GL arguments with it; LWJGL 2 has no such API). Allocations are freed by
 * {@code memFree} as in LWJGL 3.
 */
public final class MemoryUtil {
	public static final long NULL = 0L;
	public static final int PAGE_SIZE = 4096;
	public static final int CACHE_LINE_SIZE = 64;

	private MemoryUtil() {
	}

	// --- allocation ------------------------------------------------------------------------------------------------------

	private static final MemoryAllocator ALLOCATOR = new MemoryAllocator() {
	};

	/** LWJGL 3's allocator (1.21's ByteBufferBuilder takes its vertex memory from it). */
	public static MemoryAllocator getAllocator() {
		return ALLOCATOR;
	}

	public static MemoryAllocator getAllocator(boolean tracked) {
		return ALLOCATOR;
	}

	/** LWJGL 3's {@code MemoryUtil.MemoryAllocator}: the same native memory as the methods here; no function pointers. */
	public interface MemoryAllocator {
		default long getMalloc() {
			return NULL;
		}

		default long getCalloc() {
			return NULL;
		}

		default long getRealloc() {
			return NULL;
		}

		default long getFree() {
			return NULL;
		}

		default long getAlignedAlloc() {
			return NULL;
		}

		default long getAlignedFree() {
			return NULL;
		}

		default long malloc(long size) {
			return nmemAlloc(size);
		}

		default long calloc(long num, long size) {
			return nmemCalloc(num, size);
		}

		default long realloc(long ptr, long size) {
			return nmemRealloc(ptr, size);
		}

		default void free(long ptr) {
			nmemFree(ptr);
		}

		default long aligned_alloc(long alignment, long size) {
			return nmemAlloc(size);
		}

		default void aligned_free(long ptr) {
			nmemFree(ptr);
		}
	}

	public static long nmemAlloc(long size) {
		return Native.allocate(size, false);
	}

	public static long nmemAllocChecked(long size) {
		return nmemAlloc(size);
	}

	public static long nmemCalloc(long num, long size) {
		return Native.allocate(num * size, true);
	}

	public static long nmemCallocChecked(long num, long size) {
		return nmemCalloc(num, size);
	}

	public static long nmemRealloc(long ptr, long size) {
		return ptr == NULL ? nmemAlloc(size) : UNSAFE.reallocateMemory(ptr, Math.max(1L, size));
	}

	public static long nmemReallocChecked(long ptr, long size) {
		return nmemRealloc(ptr, size);
	}

	public static void nmemFree(long ptr) {
		if (ptr != NULL) {
			UNSAFE.freeMemory(ptr);
		}
	}

	public static long nmemAlignedAlloc(long alignment, long size) {
		return nmemAlloc(size);
	}

	public static void nmemAlignedFree(long ptr) {
		nmemFree(ptr);
	}

	public static ByteBuffer memAlloc(int size) {
		return Native.wrap(nmemAlloc(size), size);
	}

	public static ByteBuffer memCalloc(int num, int size) {
		return Native.wrap(nmemCalloc(num, size), num * size);
	}

	public static ByteBuffer memCalloc(int num) {
		return memCalloc(num, 1);
	}

	public static ShortBuffer memAllocShort(int num) {
		return memAlloc(num * 2).asShortBuffer();
	}

	public static IntBuffer memAllocInt(int num) {
		return memAlloc(num * 4).asIntBuffer();
	}

	public static LongBuffer memAllocLong(int num) {
		return memAlloc(num * 8).asLongBuffer();
	}

	public static FloatBuffer memAllocFloat(int num) {
		return memAlloc(num * 4).asFloatBuffer();
	}

	public static DoubleBuffer memAllocDouble(int num) {
		return memAlloc(num * 8).asDoubleBuffer();
	}

	public static PointerBuffer memAllocPointer(int num) {
		return PointerBuffer.create(nmemAlloc(num * 8L), num);
	}

	public static ShortBuffer memCallocShort(int num) {
		return memCalloc(num, 2).asShortBuffer();
	}

	public static IntBuffer memCallocInt(int num) {
		return memCalloc(num, 4).asIntBuffer();
	}

	public static LongBuffer memCallocLong(int num) {
		return memCalloc(num, 8).asLongBuffer();
	}

	public static FloatBuffer memCallocFloat(int num) {
		return memCalloc(num, 4).asFloatBuffer();
	}

	public static DoubleBuffer memCallocDouble(int num) {
		return memCalloc(num, 8).asDoubleBuffer();
	}

	public static PointerBuffer memCallocPointer(int num) {
		return PointerBuffer.create(nmemCalloc(num, 8L), num);
	}

	public static ByteBuffer memRealloc(@Nullable ByteBuffer ptr, int size) {
		ByteBuffer buffer = Native.wrap(nmemRealloc(ptr == null ? NULL : Native.address(ptr), size), size);
		if (ptr != null) {
			buffer.position(Math.min(ptr.position(), size));
		}
		return buffer;
	}

	public static IntBuffer memRealloc(@Nullable IntBuffer ptr, int size) {
		IntBuffer buffer = Native.wrap(nmemRealloc(ptr == null ? NULL : Native.address(ptr), size * 4L), size * 4).asIntBuffer();
		if (ptr != null) {
			buffer.position(Math.min(ptr.position(), size));
		}
		return buffer;
	}

	public static FloatBuffer memRealloc(@Nullable FloatBuffer ptr, int size) {
		FloatBuffer buffer = Native.wrap(nmemRealloc(ptr == null ? NULL : Native.address(ptr), size * 4L), size * 4).asFloatBuffer();
		if (ptr != null) {
			buffer.position(Math.min(ptr.position(), size));
		}
		return buffer;
	}

	public static void memFree(@Nullable Buffer ptr) {
		if (ptr != null) {
			nmemFree(Native.address(ptr));
		}
	}

	public static void memFree(@Nullable PointerBuffer ptr) {
		if (ptr != null) {
			nmemFree(ptr.address0());
		}
	}

	// --- addresses and buffers -------------------------------------------------------------------------------------------

	public static long memAddress0(Buffer buffer) {
		return Native.address(buffer);
	}

	public static long memAddress0(PointerBuffer buffer) {
		return buffer.address0();
	}

	public static long memAddress(ByteBuffer buffer) {
		return Native.address(buffer) + buffer.position();
	}

	public static long memAddress(ByteBuffer buffer, int position) {
		return Native.address(buffer) + position;
	}

	public static long memAddress(ShortBuffer buffer) {
		return Native.address(buffer) + 2L * buffer.position();
	}

	public static long memAddress(IntBuffer buffer) {
		return Native.address(buffer) + 4L * buffer.position();
	}

	public static long memAddress(IntBuffer buffer, int position) {
		return Native.address(buffer) + 4L * position;
	}

	public static long memAddress(LongBuffer buffer) {
		return Native.address(buffer) + 8L * buffer.position();
	}

	public static long memAddress(FloatBuffer buffer) {
		return Native.address(buffer) + 4L * buffer.position();
	}

	public static long memAddress(FloatBuffer buffer, int position) {
		return Native.address(buffer) + 4L * position;
	}

	public static long memAddress(DoubleBuffer buffer) {
		return Native.address(buffer) + 8L * buffer.position();
	}

	public static long memAddress(PointerBuffer buffer) {
		return buffer.address();
	}

	public static long memAddressSafe(@Nullable ByteBuffer buffer) {
		return buffer == null ? NULL : memAddress(buffer);
	}

	public static long memAddressSafe(@Nullable IntBuffer buffer) {
		return buffer == null ? NULL : memAddress(buffer);
	}

	public static long memAddressSafe(@Nullable FloatBuffer buffer) {
		return buffer == null ? NULL : memAddress(buffer);
	}

	public static long memAddressSafe(@Nullable PointerBuffer buffer) {
		return buffer == null ? NULL : buffer.address();
	}

	public static ByteBuffer memByteBuffer(long address, int capacity) {
		return Native.wrap(address, capacity);
	}

	@Nullable
	public static ByteBuffer memByteBufferSafe(long address, int capacity) {
		return address == NULL ? null : Native.wrap(address, capacity);
	}

	public static ShortBuffer memShortBuffer(long address, int capacity) {
		return Native.wrap(address, capacity * 2).asShortBuffer();
	}

	public static IntBuffer memIntBuffer(long address, int capacity) {
		return Native.wrap(address, capacity * 4).asIntBuffer();
	}

	public static LongBuffer memLongBuffer(long address, int capacity) {
		return Native.wrap(address, capacity * 8).asLongBuffer();
	}

	public static FloatBuffer memFloatBuffer(long address, int capacity) {
		return Native.wrap(address, capacity * 4).asFloatBuffer();
	}

	public static DoubleBuffer memDoubleBuffer(long address, int capacity) {
		return Native.wrap(address, capacity * 8).asDoubleBuffer();
	}

	public static PointerBuffer memPointerBuffer(long address, int capacity) {
		return PointerBuffer.create(address, capacity);
	}

	public static ByteBuffer memSlice(ByteBuffer buffer) {
		return buffer.slice().order(buffer.order());
	}

	public static ByteBuffer memSlice(ByteBuffer buffer, int offset, int capacity) {
		return Native.wrap(memAddress(buffer) + offset, capacity);
	}

	// --- copying and filling -------------------------------------------------------------------------------------------

	public static void memCopy(long src, long dst, long bytes) {
		UNSAFE.copyMemory(src, dst, bytes);
	}

	/** The remaining bytes of {@code src} to {@code dst}'s position (neither position moves). */
	public static void memCopy(ByteBuffer src, ByteBuffer dst) {
		memCopy(memAddress(src), memAddress(dst), src.remaining());
	}

	public static void memCopy(IntBuffer src, IntBuffer dst) {
		memCopy(memAddress(src), memAddress(dst), src.remaining() * 4L);
	}

	public static void memCopy(FloatBuffer src, FloatBuffer dst) {
		memCopy(memAddress(src), memAddress(dst), src.remaining() * 4L);
	}

	public static void memSet(long ptr, int value, long bytes) {
		UNSAFE.setMemory(ptr, bytes, (byte) value);
	}

	public static void memSet(ByteBuffer buffer, int value) {
		memSet(memAddress(buffer), value, buffer.remaining());
	}

	public static void memSet(IntBuffer buffer, int value) {
		memSet(memAddress(buffer), value, buffer.remaining() * 4L);
	}

	public static void memSet(FloatBuffer buffer, int value) {
		memSet(memAddress(buffer), value, buffer.remaining() * 4L);
	}

	// --- single values ----------------------------------------------------------------------------------------------------

	public static boolean memGetBoolean(long ptr) {
		return UNSAFE.getByte(ptr) != 0;
	}

	public static byte memGetByte(long ptr) {
		return UNSAFE.getByte(ptr);
	}

	public static short memGetShort(long ptr) {
		return UNSAFE.getShort(ptr);
	}

	public static int memGetInt(long ptr) {
		return UNSAFE.getInt(ptr);
	}

	public static long memGetLong(long ptr) {
		return UNSAFE.getLong(ptr);
	}

	public static float memGetFloat(long ptr) {
		return UNSAFE.getFloat(ptr);
	}

	public static double memGetDouble(long ptr) {
		return UNSAFE.getDouble(ptr);
	}

	public static long memGetAddress(long ptr) {
		return UNSAFE.getAddress(ptr);
	}

	public static void memPutByte(long ptr, byte value) {
		UNSAFE.putByte(ptr, value);
	}

	public static void memPutShort(long ptr, short value) {
		UNSAFE.putShort(ptr, value);
	}

	public static void memPutInt(long ptr, int value) {
		UNSAFE.putInt(ptr, value);
	}

	public static void memPutLong(long ptr, long value) {
		UNSAFE.putLong(ptr, value);
	}

	public static void memPutFloat(long ptr, float value) {
		UNSAFE.putFloat(ptr, value);
	}

	public static void memPutDouble(long ptr, double value) {
		UNSAFE.putDouble(ptr, value);
	}

	public static void memPutAddress(long ptr, long value) {
		UNSAFE.putAddress(ptr, value);
	}

	// --- text ----------------------------------------------------------------------------------------------------------------

	/** The text as UTF-8 in a new buffer (free it with memFree), null-terminated. */
	public static ByteBuffer memUTF8(CharSequence text) {
		return memUTF8(text, true);
	}

	public static ByteBuffer memUTF8(CharSequence text, boolean nullTerminated) {
		return encode(text.toString().getBytes(StandardCharsets.UTF_8), nullTerminated);
	}

	@Nullable
	public static ByteBuffer memUTF8Safe(@Nullable CharSequence text) {
		return text == null ? null : memUTF8(text);
	}

	public static ByteBuffer memASCII(CharSequence text) {
		return memASCII(text, true);
	}

	public static ByteBuffer memASCII(CharSequence text, boolean nullTerminated) {
		return encode(text.toString().getBytes(StandardCharsets.US_ASCII), nullTerminated);
	}

	public static int memLengthUTF8(CharSequence text, boolean nullTerminated) {
		return text.toString().getBytes(StandardCharsets.UTF_8).length + (nullTerminated ? 1 : 0);
	}

	public static int memLengthASCII(CharSequence text, boolean nullTerminated) {
		return text.length() + (nullTerminated ? 1 : 0);
	}

	/** The remaining bytes of the buffer as UTF-8 text. */
	public static String memUTF8(ByteBuffer buffer) {
		return decode(memAddress(buffer), buffer.remaining(), StandardCharsets.UTF_8);
	}

	public static String memUTF8(ByteBuffer buffer, int length) {
		return decode(memAddress(buffer), length, StandardCharsets.UTF_8);
	}

	public static String memUTF8(ByteBuffer buffer, int length, int offset) {
		return decode(memAddress(buffer) + offset, length, StandardCharsets.UTF_8);
	}

	/** The null-terminated UTF-8 text at the address. */
	public static String memUTF8(long address) {
		return decode(address, strlen(address), StandardCharsets.UTF_8);
	}

	public static String memUTF8(long address, int length) {
		return decode(address, length, StandardCharsets.UTF_8);
	}

	@Nullable
	public static String memUTF8Safe(long address) {
		return address == NULL ? null : memUTF8(address);
	}

	public static String memASCII(ByteBuffer buffer) {
		return decode(memAddress(buffer), buffer.remaining(), StandardCharsets.US_ASCII);
	}

	public static String memASCII(long address) {
		return decode(address, strlen(address), StandardCharsets.US_ASCII);
	}

	@Nullable
	public static String memASCIISafe(long address) {
		return address == NULL ? null : memASCII(address);
	}

	private static ByteBuffer encode(byte[] bytes, boolean nullTerminated) {
		ByteBuffer buffer = memAlloc(bytes.length + (nullTerminated ? 1 : 0));
		long address = Native.address(buffer);
		UNSAFE.copyMemory(bytes, Native.BYTE_ARRAY_OFFSET, null, address, bytes.length);
		if (nullTerminated) {
			UNSAFE.putByte(address + bytes.length, (byte) 0);
		}
		return buffer;
	}

	static String decode(long address, int length, java.nio.charset.Charset charset) {
		byte[] bytes = new byte[length];
		UNSAFE.copyMemory(null, address, bytes, Native.BYTE_ARRAY_OFFSET, length);
		return new String(bytes, charset);
	}

	static int strlen(long address) {
		int length = 0;
		while (UNSAFE.getByte(address + length) != 0) {
			length++;
		}
		return length;
	}
}
