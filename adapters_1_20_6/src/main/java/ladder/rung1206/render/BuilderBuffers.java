/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.render;

import java.util.Map;
import java.util.WeakHashMap;

import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.vertex.BufferBuilder;

import ladder.carried_1_20_6.com.mojang.blaze3d.vertex.ByteBufferBuilder;

/**
 * 1.21's {@code BufferBuilder.buffer} (an F row): the builder's memory as a byte buffer whose {@code reserve(n)} makes
 * room in 1.20.6's own ByteBuffer and gives the address of the write position, moved on by n (the renderer mods copy
 * whole vertices there, then count them in {@code vertices}, which 1.20.6 has as it is). Loaded only by mods that read
 * the field (they name ByteBufferBuilder, so the pack carries it).
 */
public final class BuilderBuffers {
    private static final Map<BufferBuilder, BuilderBytes> VIEWS = new WeakHashMap<>();

    private BuilderBuffers() {
    }

    public static synchronized ByteBufferBuilder buffer(BufferBuilder builder) {
        return VIEWS.computeIfAbsent(builder, BuilderBytes::new);
    }

    /** the byte buffer view: its own (tiny) memory is never written; reserve goes to the builder's */
    static final class BuilderBytes extends ByteBufferBuilder {
        private final java.lang.ref.WeakReference<BufferBuilder> builder;

        BuilderBytes(BufferBuilder builder) {
            super(8);
            this.builder = new java.lang.ref.WeakReference<>(builder);
        }

        /** the builder this byte buffer is the memory of (null once it is gone) */
        BufferBuilder builder() {
            return this.builder.get();
        }

        @Override
        public long reserve(int bytes) {
            BufferBuilder b = this.builder.get();
            if (b == null) {
                throw new IllegalStateException("the builder of this byte buffer is gone");
            }
            // (room for one more vertex after these bytes too: 1.20.6 writes a vertex's elements without a check, its
            // endVertex having made room for the next one; a resize keeps nextElementByte, an offset, and the buffer is
            // read after it)
            b.ensureCapacity(bytes + (b.format == null ? 0 : b.format.getVertexSize()));
            long address = MemoryUtil.memAddress(b.buffer) + b.nextElementByte;
            b.nextElementByte += bytes;
            return address;
        }
    }
}
