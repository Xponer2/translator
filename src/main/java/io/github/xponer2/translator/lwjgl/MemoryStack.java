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

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;

/**
 * LWJGL 3's {@code org.lwjgl.system.MemoryStack}: short-lived native memory per thread, given out from a block of
 * 64 KB and taken back when the frame opened by {@link #stackPush()} is closed (try-with-resources).
 */
public class MemoryStack implements AutoCloseable {
	private static final int DEFAULT_SIZE = 64 * 1024;
	private static final int MAX_FRAMES = 128;
	private static final ThreadLocal<MemoryStack> STACKS = ThreadLocal.withInitial(MemoryStack::create);

	private final long address;
	private final int size;
	private int pointer;
	private final int[] frames = new int[MAX_FRAMES];
	private int frameIndex;

	protected MemoryStack(long address, int size) {
		this.address = address;
		this.size = size;
		this.pointer = size;
	}

	public static MemoryStack create() {
		return create(DEFAULT_SIZE);
	}

	public static MemoryStack create(int capacity) {
		return new MemoryStack(Native.allocate(capacity, false), capacity);
	}

	public static MemoryStack stackGet() {
		return STACKS.get();
	}

	public static MemoryStack stackPush() {
		return stackGet().push();
	}

	public static MemoryStack stackPop() {
		return stackGet().pop();
	}

	public MemoryStack push() {
		if (this.frameIndex == this.frames.length) {
			throw new IllegalStateException("Out of MemoryStack frames");
		}
		this.frames[this.frameIndex++] = this.pointer;
		return this;
	}

	public MemoryStack pop() {
		this.pointer = this.frames[--this.frameIndex];
		return this;
	}

	@Override
	public void close() {
		this.pop();
	}

	public long getAddress() {
		return this.address;
	}

	public int getSize() {
		return this.size;
	}

	public int getFrameIndex() {
		return this.frameIndex;
	}

	public long getPointerAddress() {
		return this.address + this.pointer;
	}

	public int getPointer() {
		return this.pointer;
	}

	public void setPointer(int pointer) {
		this.pointer = pointer;
	}

	// --- raw ------------------------------------------------------------------------------------------------------------

	public long nmalloc(int size) {
		return this.nmalloc(8, size);
	}

	public long nmalloc(int alignment, int size) {
		long newPointer = (this.address + this.pointer - size) & -(long) Math.max(1, alignment);
		if (newPointer < this.address) {
			throw new OutOfMemoryError("Out of stack space: " + size + " bytes asked, " + this.pointer + " free");
		}
		this.pointer = (int) (newPointer - this.address);
		return newPointer;
	}

	public long ncalloc(int alignment, int num, int size) {
		int bytes = num * size;
		long pointer = this.nmalloc(alignment, bytes);
		UNSAFE.setMemory(pointer, bytes, (byte) 0);
		return pointer;
	}

	// --- buffers --------------------------------------------------------------------------------------------------------

	public ByteBuffer malloc(int size) {
		return Native.wrap(this.nmalloc(8, size), size);
	}

	public ByteBuffer malloc(int alignment, int size) {
		return Native.wrap(this.nmalloc(alignment, size), size);
	}

	public ByteBuffer calloc(int size) {
		return Native.wrap(this.ncalloc(8, size, 1), size);
	}

	public ByteBuffer calloc(int alignment, int size) {
		return Native.wrap(this.ncalloc(alignment, size, 1), size);
	}

	public ShortBuffer mallocShort(int num) {
		return this.malloc(num * 2).asShortBuffer();
	}

	public ShortBuffer callocShort(int num) {
		return this.calloc(num * 2).asShortBuffer();
	}

	public IntBuffer mallocInt(int num) {
		return this.malloc(num * 4).asIntBuffer();
	}

	public IntBuffer callocInt(int num) {
		return this.calloc(num * 4).asIntBuffer();
	}

	public LongBuffer mallocLong(int num) {
		return this.malloc(num * 8).asLongBuffer();
	}

	public LongBuffer callocLong(int num) {
		return this.calloc(num * 8).asLongBuffer();
	}

	public FloatBuffer mallocFloat(int num) {
		return this.malloc(num * 4).asFloatBuffer();
	}

	public FloatBuffer callocFloat(int num) {
		return this.calloc(num * 4).asFloatBuffer();
	}

	public DoubleBuffer mallocDouble(int num) {
		return this.malloc(num * 8).asDoubleBuffer();
	}

	public DoubleBuffer callocDouble(int num) {
		return this.calloc(num * 8).asDoubleBuffer();
	}

	public PointerBuffer mallocPointer(int num) {
		return PointerBuffer.create(this.nmalloc(8, num * 8), num);
	}

	public PointerBuffer callocPointer(int num) {
		return PointerBuffer.create(this.ncalloc(8, num, 8), num);
	}

	public ByteBuffer bytes(byte... values) {
		ByteBuffer buffer = this.malloc(values.length);
		buffer.put(values).flip();
		return buffer;
	}

	public IntBuffer ints(int value) {
		return this.mallocInt(1).put(0, value);
	}

	public IntBuffer ints(int a, int b) {
		return this.mallocInt(2).put(0, a).put(1, b);
	}

	public IntBuffer ints(int... values) {
		IntBuffer buffer = this.mallocInt(values.length);
		buffer.put(values).flip();
		return buffer;
	}

	public LongBuffer longs(long value) {
		return this.mallocLong(1).put(0, value);
	}

	public LongBuffer longs(long... values) {
		LongBuffer buffer = this.mallocLong(values.length);
		buffer.put(values).flip();
		return buffer;
	}

	public FloatBuffer floats(float value) {
		return this.mallocFloat(1).put(0, value);
	}

	public FloatBuffer floats(float... values) {
		FloatBuffer buffer = this.mallocFloat(values.length);
		buffer.put(values).flip();
		return buffer;
	}

	public DoubleBuffer doubles(double... values) {
		DoubleBuffer buffer = this.mallocDouble(values.length);
		buffer.put(values).flip();
		return buffer;
	}

	public PointerBuffer pointers(long value) {
		return this.mallocPointer(1).put(0, value);
	}

	public PointerBuffer pointers(long... values) {
		PointerBuffer buffer = this.mallocPointer(values.length);
		for (long value : values) {
			buffer.put(value);
		}
		return buffer.flip();
	}

	public ByteBuffer UTF8(CharSequence text) {
		return this.UTF8(text, true);
	}

	public ByteBuffer UTF8(CharSequence text, boolean nullTerminated) {
		return this.encode(text.toString().getBytes(StandardCharsets.UTF_8), nullTerminated);
	}

	public ByteBuffer ASCII(CharSequence text) {
		return this.ASCII(text, true);
	}

	public ByteBuffer ASCII(CharSequence text, boolean nullTerminated) {
		return this.encode(text.toString().getBytes(StandardCharsets.US_ASCII), nullTerminated);
	}

	/** The text at the stack's pointer; its length in bytes. */
	public int nUTF8(CharSequence text, boolean nullTerminated) {
		return this.UTF8(text, nullTerminated).capacity();
	}

	public int nASCII(CharSequence text, boolean nullTerminated) {
		return this.ASCII(text, nullTerminated).capacity();
	}

	private ByteBuffer encode(byte[] bytes, boolean nullTerminated) {
		ByteBuffer buffer = this.malloc(1, bytes.length + (nullTerminated ? 1 : 0));
		long address = Native.address(buffer);
		UNSAFE.copyMemory(bytes, Native.BYTE_ARRAY_OFFSET, null, address, bytes.length);
		if (nullTerminated) {
			UNSAFE.putByte(address + bytes.length, (byte) 0);
		}
		return buffer;
	}

	// --- static shortcuts on this thread's stack -------------------------------------------------------------------------

	public static ByteBuffer stackMalloc(int size) {
		return stackGet().malloc(size);
	}

	public static ByteBuffer stackCalloc(int size) {
		return stackGet().calloc(size);
	}

	public static IntBuffer stackMallocInt(int num) {
		return stackGet().mallocInt(num);
	}

	public static IntBuffer stackCallocInt(int num) {
		return stackGet().callocInt(num);
	}

	public static LongBuffer stackMallocLong(int num) {
		return stackGet().mallocLong(num);
	}

	public static FloatBuffer stackMallocFloat(int num) {
		return stackGet().mallocFloat(num);
	}

	public static FloatBuffer stackCallocFloat(int num) {
		return stackGet().callocFloat(num);
	}

	public static PointerBuffer stackMallocPointer(int num) {
		return stackGet().mallocPointer(num);
	}

	public static PointerBuffer stackCallocPointer(int num) {
		return stackGet().callocPointer(num);
	}

	public static ByteBuffer stackUTF8(CharSequence text) {
		return stackGet().UTF8(text);
	}

	public static ByteBuffer stackASCII(CharSequence text) {
		return stackGet().ASCII(text);
	}

	public static IntBuffer stackInts(int... values) {
		return stackGet().ints(values);
	}

	public static FloatBuffer stackFloats(float... values) {
		return stackGet().floats(values);
	}
}
