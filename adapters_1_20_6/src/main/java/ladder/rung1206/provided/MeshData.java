/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided;

import java.nio.ByteBuffer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;

/**
 * 1.21's {@code MeshData} (a finished buffer of vertices) over 1.20.6's {@code BufferBuilder.RenderedBuffer}: what
 * {@code BufferBuilder.buildOrThrow()} gives and the uploaders take (ladder.rung1206.render.VertexAdapters).
 */
public final class MeshData implements AutoCloseable {
    private final BufferBuilder.RenderedBuffer buffer;

    public MeshData(BufferBuilder.RenderedBuffer buffer) {
        this.buffer = buffer;
    }

    /** the 1.20.6 buffer under it */
    public BufferBuilder.RenderedBuffer buffer() {
        return this.buffer;
    }

    public ByteBuffer vertexBuffer() {
        return this.buffer.vertexBuffer();
    }

    public ByteBuffer indexBuffer() {
        return this.buffer.indexBuffer();
    }

    public DrawState drawState() {
        BufferBuilder.DrawState state = this.buffer.drawState();
        return new DrawState(state.format(), state.vertexCount(), state.indexCount(), state.mode(), state.indexType());
    }

    /**
     * 1.21's {@code sortQuads(buffer, sorting)}: sorts the quads' indices for translucent drawing. 1.20.6 sorts while its
     * builder ends (setQuadSorting), and this buffer has ended: its quads stay in their order (a translucent batch may
     * overlap in another order); no sort state is kept.
     */
    public ladder.carried_1_20_6.com.mojang.blaze3d.vertex.MeshData$SortState sortQuads(
            ladder.carried_1_20_6.com.mojang.blaze3d.vertex.ByteBufferBuilder buffer, com.mojang.blaze3d.vertex.VertexSorting sorting) {
        return null;
    }

    @Override
    public void close() {
        this.buffer.release();
    }

    /** 1.21's {@code MeshData.DrawState} */
    public record DrawState(VertexFormat format, int vertexCount, int indexCount, VertexFormat.Mode mode, VertexFormat.IndexType indexType) {
    }
}
