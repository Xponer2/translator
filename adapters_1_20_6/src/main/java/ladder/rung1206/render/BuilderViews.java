/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.render;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormatElement;

/**
 * 1.21's BufferBuilder internals the renderer mods read and write (F rows), as views of 1.20.6's builder state. 1.21
 * writes a vertex at {@code vertexPointer} in native memory; 1.20.6 writes element by element at {@code nextElementByte}
 * of its direct ByteBuffer, so the current vertex starts at nextElementByte less the offset of the element being written
 * (a whole vertex back between vertices), and the elements still to fill are the format's from that element on (1.21
 * leaves the position out: a vertex begins with it).
 *
 * <p>A mod's write of {@code vertexPointer} or {@code elementsToFill} is kept for the vertex it was made at (the
 * builder's write position and vertex count then): reading it back gives the written value until the builder moves on.
 * No carried type here (see BuilderBuffers for the byte buffer).
 */
public final class BuilderViews {
    /** a mod's writes at one write position: the pointer and the mask it set (or none) */
    private static final class Written {
        int vertices;
        int nextByte;
        boolean hasPointer;
        long pointer;
        boolean hasMask;
        int mask;

        boolean current(BufferBuilder builder) {
            return builder.vertices == this.vertices && builder.nextElementByte == this.nextByte;
        }
    }

    private static final Map<BufferBuilder, Written> WRITTEN = new WeakHashMap<>();

    private BuilderViews() {
    }

    private static synchronized Written written(BufferBuilder builder, boolean create) {
        Written w = WRITTEN.get(builder);
        if (w != null && !w.current(builder)) {
            // (the builder moved on since: those writes were of another vertex)
            WRITTEN.remove(builder);
            w = null;
        }
        if (w == null && create) {
            w = new Written();
            w.vertices = builder.vertices;
            w.nextByte = builder.nextElementByte;
            WRITTEN.put(builder, w);
        }
        return w;
    }

    private static final Map<com.mojang.blaze3d.vertex.VertexFormat, int[]> OFFSETS = new WeakHashMap<>();

    /** 1.21's {@code offsetsByElement}: the format's offset of each element by its 1.21 id (-1: not in the format) */
    public static int[] offsetsByElement(BufferBuilder builder) {
        com.mojang.blaze3d.vertex.VertexFormat format = builder.format;
        if (format == null) {
            int[] none = new int[32];
            java.util.Arrays.fill(none, -1);
            return none;
        }
        synchronized (OFFSETS) {
            return OFFSETS.computeIfAbsent(format, f -> {
                int[] offsets = new int[32];
                java.util.Arrays.fill(offsets, -1);
                List<VertexFormatElement> elements = f.getElements();
                for (int i = 0; i < elements.size(); i++) {
                    int id = VertexFormats.id(elements.get(i));
                    if (id >= 0 && id < offsets.length) {
                        offsets[id] = f.getOffset(i);
                    }
                }
                return offsets;
            });
        }
    }

    /** 1.21's {@code vertexSize}: the format's vertex size */
    public static int vertexSize(BufferBuilder builder) {
        return builder.format == null ? 0 : builder.format.getVertexSize();
    }

    /** 1.21's {@code vertexPointer}: the address of the vertex being written (the last one between vertices), or -1 */
    public static long vertexPointer(BufferBuilder builder) {
        Written w = written(builder, false);
        if (w != null && w.hasPointer) {
            return w.pointer;
        }
        if (!builder.building() || builder.format == null || builder.buffer == null) {
            return -1L;
        }
        int start = builder.elementIndex == 0 ? builder.nextElementByte - builder.format.getVertexSize()
                : builder.nextElementByte - builder.format.getOffset(builder.elementIndex);
        return start < 0 ? -1L : MemoryUtil.memAddress(builder.buffer) + start;
    }

    /** a write of {@code vertexPointer}: kept, and read back, while the builder stays at this vertex */
    public static void setVertexPointer(BufferBuilder builder, long pointer) {
        Written w = written(builder, true);
        synchronized (BuilderViews.class) {
            w.hasPointer = true;
            w.pointer = pointer;
        }
    }

    /** 1.21's {@code elementsToFill}: the masks of the format's elements not yet written in this vertex (none between) */
    public static int elementsToFill(BufferBuilder builder) {
        Written w = written(builder, false);
        if (w != null && w.hasMask) {
            return w.mask;
        }
        if (builder.elementIndex == 0 || builder.format == null) {
            return 0;
        }
        List<VertexFormatElement> elements = builder.format.getElements();
        int mask = 0;
        for (int i = builder.elementIndex; i < elements.size(); i++) {
            VertexFormatElement element = elements.get(i);
            if (element.getUsage() != VertexFormatElement.Usage.POSITION) {
                mask |= VertexFormats.mask(element);
            }
        }
        return mask;
    }

    /**
     * a write of {@code elementsToFill}: 0 after a mod wrote whole vertices itself (sodium's push) puts 1.20.6 at a
     * vertex's start, as its own state is between vertices; another mask is kept, and read back, while the builder stays
     * at this vertex (the mod writes those elements at the pointer itself)
     */
    public static void setElementsToFill(BufferBuilder builder, int mask) {
        if (mask == 0) {
            builder.elementIndex = 0;
            builder.currentElement = builder.format == null ? null : builder.format.getElements().get(0);
        }
        Written w = written(builder, true);
        synchronized (BuilderViews.class) {
            w.hasMask = true;
            w.mask = mask;
        }
    }
}
