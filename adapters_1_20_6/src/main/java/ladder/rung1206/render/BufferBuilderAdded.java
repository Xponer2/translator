/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.render;

import ladder.rung1206.shape.AddedTo;

/**
 * The members an adapters' mixin adds to BufferBuilder (BufferBuilderAddVertexMixin), for the other adapters' calls: the
 * build turns a cast to this class and its calls into BufferBuilder's own. Never instantiated.
 */
@AddedTo("com/mojang/blaze3d/vertex/BufferBuilder")
public abstract class BufferBuilderAdded {
    private BufferBuilderAdded() {
    }

    /** 1.21's {@code BufferBuilder.addVertex(x, y, z, color, u, v, overlay, light, nx, ny, nz)} */
    public abstract void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz);
}
