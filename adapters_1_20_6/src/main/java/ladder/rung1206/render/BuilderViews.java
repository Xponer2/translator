/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.render;

import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.vertex.BufferBuilder;

/**
 * 1.21's BufferBuilder internals the renderer mods read and write (F rows), as views of 1.20.6's builder state. 1.21
 * writes a vertex at {@code vertexPointer} in native memory; 1.20.6 writes element by element at {@code nextElementByte}
 * of its direct ByteBuffer, so the current vertex starts at nextElementByte less the offset of the element being written
 * (a whole vertex back between vertices). No carried type here (see BuilderBuffers for the byte buffer).
 */
public final class BuilderViews {
    private BuilderViews() {
    }

    /** 1.21's {@code vertexSize}: the format's vertex size */
    public static int vertexSize(BufferBuilder builder) {
        return builder.format == null ? 0 : builder.format.getVertexSize();
    }

    /** 1.21's {@code vertexPointer}: the address of the vertex being written (the last one between vertices), or -1 */
    public static long vertexPointer(BufferBuilder builder) {
        if (!builder.building() || builder.format == null || builder.buffer == null) {
            return -1L;
        }
        int start = builder.elementIndex == 0 ? builder.nextElementByte - builder.format.getVertexSize()
                : builder.nextElementByte - builder.format.getOffset(builder.elementIndex);
        return start < 0 ? -1L : MemoryUtil.memAddress(builder.buffer) + start;
    }

    /** a write of {@code vertexPointer}: 1.20.6 derives it from its write position (the write itself changes nothing) */
    public static void setVertexPointer(BufferBuilder builder, long pointer) {
    }

    /** 1.21's {@code elementsToFill}: none left between vertices (1.20.6 writes a vertex's elements in order) */
    public static int elementsToFill(BufferBuilder builder) {
        return builder.elementIndex == 0 ? 0 : ~0;
    }

    /**
     * a write of {@code elementsToFill}: 0 after a mod wrote whole vertices itself (sodium's push), the write position
     * then at a vertex's start, as 1.20.6's own state is between vertices
     */
    public static void setElementsToFill(BufferBuilder builder, int mask) {
        if (mask == 0) {
            builder.elementIndex = 0;
            builder.currentElement = builder.format == null ? null : builder.format.getElements().get(0);
        }
    }
}
