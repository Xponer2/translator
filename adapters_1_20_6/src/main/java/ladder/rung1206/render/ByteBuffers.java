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
    /** the byte buffers of the builders made for them (weakly: a builder lives as long as its byte buffer) */
    private static final Map<BufferBuilder, java.lang.ref.WeakReference<ByteBufferBuilder>> MADE = new WeakHashMap<>();
    /** the byte buffers standing for 1.20.6's own builders (the buffer sources' shared and fixed ones, kept all game) */
    private static final Map<BufferBuilder, ByteBufferBuilder> STANDING = new WeakHashMap<>();

    private ByteBuffers() {
    }

    /** the lower builder of a byte buffer (its capacity as the builder's first size) */
    public static synchronized BufferBuilder builder(ByteBufferBuilder bytes) {
        return BUILDERS.computeIfAbsent(bytes, b -> {
            BufferBuilder made = new BufferBuilder(Math.max(256, capacity(b)));
            MADE.put(made, new java.lang.ref.WeakReference<>(b));
            return made;
        });
    }

    /** the byte buffer of a lower builder: the one it was made for, or a small one standing for a builder 1.20.6 made */
    public static synchronized ByteBufferBuilder bytes(BufferBuilder builder) {
        java.lang.ref.WeakReference<ByteBufferBuilder> made = MADE.get(builder);
        ByteBufferBuilder bytes = made == null ? null : made.get();
        if (bytes != null) {
            return bytes;
        }
        return STANDING.computeIfAbsent(builder, b -> {
            ByteBufferBuilder standing = new ByteBufferBuilder(256);
            BUILDERS.put(standing, b);
            return standing;
        });
    }

    /** 1.21's {@code BufferSource.fixedBuffers}: 1.20.6's fixed builders seen as their byte buffers (a live view) */
    public static java.util.SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers(MultiBufferSource.BufferSource source) {
        return new FixedBuffers(source.fixedBuffers);
    }

    /** 1.21's {@code BufferSource.sharedBuffer}: the byte buffer of 1.20.6's shared builder */
    public static ByteBufferBuilder sharedBuffer(MultiBufferSource.BufferSource source) {
        return bytes(source.builder);
    }

    /** 1.21's {@code BufferSource.lastSharedType}: 1.20.6's last state of the shared builder */
    public static RenderType lastSharedType(MultiBufferSource.BufferSource source) {
        return source.lastState.orElse(null);
    }

    /** a map of render types to byte buffers over 1.20.6's map of render types to builders (its order, its changes) */
    static final class FixedBuffers extends java.util.AbstractMap<RenderType, ByteBufferBuilder>
            implements java.util.SequencedMap<RenderType, ByteBufferBuilder> {
        private final Map<RenderType, BufferBuilder> lower;

        FixedBuffers(Map<RenderType, BufferBuilder> lower) {
            this.lower = lower;
        }

        @Override
        public int size() {
            return lower.size();
        }

        @Override
        public boolean containsKey(Object key) {
            return lower.containsKey(key);
        }

        @Override
        public ByteBufferBuilder get(Object key) {
            BufferBuilder builder = lower.get(key);
            return builder == null ? null : bytes(builder);
        }

        @Override
        public ByteBufferBuilder put(RenderType key, ByteBufferBuilder value) {
            BufferBuilder old = lower.put(key, builder(value));
            return old == null ? null : bytes(old);
        }

        @Override
        public ByteBufferBuilder remove(Object key) {
            BufferBuilder old = lower.remove(key);
            return old == null ? null : bytes(old);
        }

        @Override
        public java.util.Set<RenderType> keySet() {
            return lower.keySet();
        }

        @Override
        public java.util.Set<Map.Entry<RenderType, ByteBufferBuilder>> entrySet() {
            return new java.util.AbstractSet<>() {
                @Override
                public int size() {
                    return lower.size();
                }

                @Override
                public java.util.Iterator<Map.Entry<RenderType, ByteBufferBuilder>> iterator() {
                    java.util.Iterator<Map.Entry<RenderType, BufferBuilder>> it = lower.entrySet().iterator();
                    return new java.util.Iterator<>() {
                        @Override
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        @Override
                        public Map.Entry<RenderType, ByteBufferBuilder> next() {
                            Map.Entry<RenderType, BufferBuilder> e = it.next();
                            return new java.util.AbstractMap.SimpleImmutableEntry<>(e.getKey(), bytes(e.getValue()));
                        }

                        @Override
                        public void remove() {
                            it.remove();
                        }
                    };
                }
            };
        }

        @Override
        public java.util.SequencedMap<RenderType, ByteBufferBuilder> reversed() {
            return new LinkedHashMap<>(this).reversed();
        }
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
