/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.render;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;

import ladder.carried_1_20_6.com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * 1.21's byte buffers (ByteBufferBuilder: the memory a BufferBuilder of one batch writes into, kept by the caller) on
 * 1.20.6's builders, which keep their own memory: each byte buffer gets one lower BufferBuilder of its size, begun for each
 * batch made over it; the buffer sources take those builders.
 */
public final class ByteBuffers {
    private static final Map<ByteBufferBuilder, BufferBuilder> BUILDERS = new WeakHashMap<>();

    private ByteBuffers() {
    }

    /** the lower builder of a byte buffer (its capacity as the builder's first size) */
    public static synchronized BufferBuilder builder(ByteBufferBuilder bytes) {
        return BUILDERS.computeIfAbsent(bytes, b -> new BufferBuilder(Math.max(256, capacity(b))));
    }

    private static int capacity(ByteBufferBuilder bytes) {
        try {
            java.lang.reflect.Field f = ByteBufferBuilder.class.getDeclaredField("capacity");
            f.setAccessible(true);
            return f.getInt(bytes);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 256;
        }
    }

    /** 1.21's {@code new BufferBuilder(bytes, mode, format)}: the byte buffer's builder, begun for this batch */
    public static BufferBuilder create(ByteBufferBuilder bytes, VertexFormat.Mode mode, VertexFormat format) {
        BufferBuilder builder = builder(bytes);
        if (builder.building()) {
            // 1.21 starts a new batch over the same memory: one left unfinished is dropped
            builder.end().release();
        }
        builder.begin(mode, format);
        return builder;
    }

    /** 1.21's {@code MultiBufferSource.immediate(bytes)} */
    public static MultiBufferSource.BufferSource immediate(ByteBufferBuilder bytes) {
        return MultiBufferSource.immediate(builder(bytes));
    }

    /** 1.21's {@code MultiBufferSource.immediateWithBuffers(fixed, shared)} */
    public static MultiBufferSource.BufferSource immediateWithBuffers(java.util.SequencedMap<RenderType, ByteBufferBuilder> fixed, ByteBufferBuilder shared) {
        Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();
        fixed.forEach((type, bytes) -> builders.put(type, builder(bytes)));
        return MultiBufferSource.immediateWithBuffers(builders, builder(shared));
    }

    /** 1.21's {@code new MultiBufferSource.BufferSource(shared, fixed)} */
    public static MultiBufferSource.BufferSource bufferSource(ByteBufferBuilder shared, java.util.SequencedMap<RenderType, ByteBufferBuilder> fixed) {
        return immediateWithBuffers(fixed, shared);
    }

    // a subclass's super(shared, fixed) of 1.21's BufferSource (a K row): 1.20.6's takes the byte buffers' builders

    public static BufferBuilder sharedBuilder(ByteBufferBuilder shared, java.util.SequencedMap<RenderType, ByteBufferBuilder> fixed) {
        return builder(shared);
    }

    public static Map<RenderType, BufferBuilder> fixedBuilders(ByteBufferBuilder shared, java.util.SequencedMap<RenderType, ByteBufferBuilder> fixed) {
        Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();
        fixed.forEach((type, bytes) -> builders.put(type, builder(bytes)));
        return builders;
    }
}
