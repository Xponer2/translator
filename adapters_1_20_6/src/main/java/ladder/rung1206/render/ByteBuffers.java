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

    private ByteBuffers() {
    }

    /** the lower builder of a byte buffer (its capacity as the builder's first size) */
    public static synchronized BufferBuilder builder(ByteBufferBuilder bytes) {
        if (bytes instanceof BuilderBuffers.BuilderBytes view && view.builder() != null) {
            // (the byte buffer of a builder 1.20.6 made: that builder)
            return view.builder();
        }
        return BUILDERS.computeIfAbsent(bytes, b -> {
            BufferBuilder made = new BufferBuilder(Math.max(256, capacity(b)));
            MADE.put(made, new java.lang.ref.WeakReference<>(b));
            return made;
        });
    }

    /**
     * the byte buffer of a lower builder: the one it was made for, or for a builder 1.20.6 made, the view of its own
     * memory (BuilderBuffers: reserve makes room at its write position; the view holds the builder weakly, so neither
     * keeps the other alive)
     */
    public static synchronized ByteBufferBuilder bytes(BufferBuilder builder) {
        java.lang.ref.WeakReference<ByteBufferBuilder> made = MADE.get(builder);
        ByteBufferBuilder bytes = made == null ? null : made.get();
        if (bytes != null) {
            return bytes;
        }
        return BuilderBuffers.buffer(builder);
    }

    /** 1.21's {@code BufferSource.fixedBuffers}: 1.20.6's fixed builders seen as their byte buffers (a live view) */
    public static java.util.SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers(MultiBufferSource.BufferSource source) {
        return new FixedBuffers(source.fixedBuffers);
    }

    /** 1.21's {@code BufferSource.sharedBuffer}: the byte buffer of 1.20.6's shared builder */
    public static ByteBufferBuilder sharedBuffer(MultiBufferSource.BufferSource source) {
        return bytes(source.builder);
    }

    /**
     * the shared builder's byte buffer when the pack carries ByteBufferBuilder, else null (the vanilla draw path asks it
     * for 1.21's sortQuads call, whose 1.20.6 form uses no buffer; a pack whose mods never name ByteBufferBuilder has none)
     */
    public static ByteBufferBuilder sharedBufferIfCarried(MultiBufferSource.BufferSource source) {
        return carried() ? sharedBuffer(source) : null;
    }

    private static volatile Boolean carried;

    private static boolean carried() {
        Boolean known = carried;
        if (known == null) {
            try {
                Class.forName("ladder.carried_1_20_6.com.mojang.blaze3d.vertex.ByteBufferBuilder", false, ByteBuffers.class.getClassLoader());
                known = true;
            } catch (ClassNotFoundException | LinkageError e) {
                known = false;
            }
            carried = known;
        }
        return known;
    }

    /** 1.21's {@code BufferSource.lastSharedType}: 1.20.6's last state of the shared builder */
    public static RenderType lastSharedType(MultiBufferSource.BufferSource source) {
        return source.lastState.orElse(null);
    }

    /** a write of 1.21's {@code BufferSource.lastSharedType} (a buffer source of a mod's own): 1.20.6's last state */
    public static void setLastSharedType(MultiBufferSource.BufferSource source, RenderType type) {
        source.lastState = java.util.Optional.ofNullable(type);
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
            return new Reversed(this);
        }
    }

    /**
     * the fixed buffers in reverse order, backed by the same map (its changes show in both; an iteration walks the keys
     * as they were when it began, its removals going to the map)
     */
    static final class Reversed extends java.util.AbstractMap<RenderType, ByteBufferBuilder>
            implements java.util.SequencedMap<RenderType, ByteBufferBuilder> {
        private final FixedBuffers forward;

        Reversed(FixedBuffers forward) {
            this.forward = forward;
        }

        @Override
        public int size() {
            return this.forward.size();
        }

        @Override
        public boolean containsKey(Object key) {
            return this.forward.containsKey(key);
        }

        @Override
        public ByteBufferBuilder get(Object key) {
            return this.forward.get(key);
        }

        @Override
        public ByteBufferBuilder put(RenderType key, ByteBufferBuilder value) {
            return this.forward.put(key, value);
        }

        @Override
        public ByteBufferBuilder remove(Object key) {
            return this.forward.remove(key);
        }

        @Override
        public java.util.Set<Map.Entry<RenderType, ByteBufferBuilder>> entrySet() {
            return new java.util.AbstractSet<>() {
                @Override
                public int size() {
                    return Reversed.this.forward.size();
                }

                @Override
                public java.util.Iterator<Map.Entry<RenderType, ByteBufferBuilder>> iterator() {
                    java.util.List<RenderType> keys = new java.util.ArrayList<>(Reversed.this.forward.keySet());
                    java.util.Collections.reverse(keys);
                    java.util.Iterator<RenderType> it = keys.iterator();
                    return new java.util.Iterator<>() {
                        private RenderType last;

                        @Override
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        @Override
                        public Map.Entry<RenderType, ByteBufferBuilder> next() {
                            this.last = it.next();
                            return new java.util.AbstractMap.SimpleImmutableEntry<>(this.last, Reversed.this.forward.get(this.last));
                        }

                        @Override
                        public void remove() {
                            if (this.last == null) {
                                throw new IllegalStateException();
                            }
                            Reversed.this.forward.remove(this.last);
                            this.last = null;
                        }
                    };
                }
            };
        }

        @Override
        public java.util.SequencedMap<RenderType, ByteBufferBuilder> reversed() {
            return this.forward;
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
