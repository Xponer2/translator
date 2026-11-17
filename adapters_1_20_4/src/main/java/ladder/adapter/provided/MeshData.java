/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.provided;

import java.nio.ByteBuffer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;

/**
 * 1.21's {@code MeshData} (a finished buffer of vertices) over 1.20.4's {@code BufferBuilder.RenderedBuffer}: what
 * {@code BufferBuilder.buildOrThrow()} gives and the uploaders take (ladder.adapter.render.RenderAdapters).
 */
public final class MeshData implements AutoCloseable {
    private final BufferBuilder.RenderedBuffer buffer;

    public MeshData(BufferBuilder.RenderedBuffer buffer) {
        this.buffer = buffer;
    }

    /** the 1.20.4 buffer under it */
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

    @Override
    public void close() {
        this.buffer.release();
    }

    /** 1.21's {@code MeshData.DrawState} */
    public record DrawState(VertexFormat format, int vertexCount, int indexCount, VertexFormat.Mode mode, VertexFormat.IndexType indexType) {
    }
}
