/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.render;

import com.mojang.blaze3d.vertex.VertexFormat;

/**
 * A 1.20.6 {@code BufferBuilder} as the vertex adapters see it (mixin.BufferBuilderMixin): its format, and its ends
 * (before them the vertices 1.21 code begun are written out).
 */
public interface PendingVertex {
    VertexFormat ladder$format();
}
