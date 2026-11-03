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

import javax.annotation.Nullable;

/** LWJGL 3's {@code org.lwjgl.PointerBuffer}: a buffer of native addresses (8 bytes each) at an address. */
public class PointerBuffer {
	private final long address;
	private final int capacity;
	private int position;
	private int limit;
	private int mark = -1;

	protected PointerBuffer(long address, int capacity) {
		this.address = address;
		this.capacity = capacity;
		this.limit = capacity;
	}

	public static PointerBuffer allocateDirect(int capacity) {
		return new PointerBuffer(Native.allocate(capacity * 8L, true), capacity);
	}

	public static PointerBuffer create(long address, int capacity) {
		return new PointerBuffer(address, capacity);
	}

	public static PointerBuffer create(ByteBuffer source) {
		return new PointerBuffer(MemoryUtil.memAddress(source), source.remaining() / 8);
	}

	public static int sizeof() {
		return 8;
	}

	public long address0() {
		return this.address;
	}

	public long address() {
		return this.address + 8L * this.position;
	}

	public long address(int position) {
		return this.address + 8L * position;
	}

	public int capacity() {
		return this.capacity;
	}

	public int position() {
		return this.position;
	}

	public PointerBuffer position(int position) {
		this.position = position;
		return this;
	}

	public int limit() {
		return this.limit;
	}

	public PointerBuffer limit(int limit) {
		this.limit = limit;
		this.position = Math.min(this.position, limit);
		return this;
	}

	public int remaining() {
		return this.limit - this.position;
	}

	public boolean hasRemaining() {
		return this.position < this.limit;
	}

	public PointerBuffer mark() {
		this.mark = this.position;
		return this;
	}

	public PointerBuffer reset() {
		this.position = this.mark;
		return this;
	}

	public PointerBuffer clear() {
		this.position = 0;
		this.limit = this.capacity;
		this.mark = -1;
		return this;
	}

	public PointerBuffer flip() {
		this.limit = this.position;
		this.position = 0;
		this.mark = -1;
		return this;
	}

	public PointerBuffer rewind() {
		this.position = 0;
		this.mark = -1;
		return this;
	}

	public PointerBuffer slice() {
		return new PointerBuffer(this.address(), this.remaining());
	}

	public long get() {
		return UNSAFE.getAddress(this.address + 8L * this.position++);
	}

	public long get(int index) {
		return UNSAFE.getAddress(this.address + 8L * index);
	}

	public PointerBuffer put(long value) {
		UNSAFE.putAddress(this.address + 8L * this.position++, value);
		return this;
	}

	public PointerBuffer put(int index, long value) {
		UNSAFE.putAddress(this.address + 8L * index, value);
		return this;
	}

	/** The address of the buffer's remaining data. */
	public PointerBuffer put(Buffer buffer) {
		return this.put(bufferAddress(buffer));
	}

	public PointerBuffer put(int index, Buffer buffer) {
		return this.put(index, bufferAddress(buffer));
	}

	public PointerBuffer put(PointerBuffer source) {
		while (source.hasRemaining()) {
			this.put(source.get());
		}
		return this;
	}

	public ByteBuffer getByteBuffer(int size) {
		return MemoryUtil.memByteBuffer(this.get(), size);
	}

	public ByteBuffer getByteBuffer(int index, int size) {
		return MemoryUtil.memByteBuffer(this.get(index), size);
	}

	public String getStringUTF8() {
		return MemoryUtil.memUTF8(this.get());
	}

	public String getStringUTF8(int index) {
		return MemoryUtil.memUTF8(this.get(index));
	}

	public String getStringASCII() {
		return MemoryUtil.memASCII(this.get());
	}

	public String getStringASCII(int index) {
		return MemoryUtil.memASCII(this.get(index));
	}

	/** Frees memory from {@link #allocateDirect} or {@code memAllocPointer}. */
	public void free() {
		MemoryUtil.nmemFree(this.address);
	}

	private static long bufferAddress(@Nullable Buffer buffer) {
		if (buffer == null) {
			return MemoryUtil.NULL;
		}
		int size = buffer instanceof ByteBuffer ? 1 : buffer instanceof java.nio.ShortBuffer ? 2
				: buffer instanceof java.nio.LongBuffer || buffer instanceof java.nio.DoubleBuffer ? 8 : 4;
		return Native.address(buffer) + (long) size * buffer.position();
	}

	@Override
	public String toString() {
		return "PointerBuffer[address=" + this.address + ", position=" + this.position + ", limit=" + this.limit + ", capacity="
				+ this.capacity + "]";
	}
}
