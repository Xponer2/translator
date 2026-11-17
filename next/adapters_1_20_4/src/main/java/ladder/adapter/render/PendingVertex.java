/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.render;

import com.mojang.blaze3d.vertex.VertexFormat;

/**
 * A 1.20.4 {@code BufferBuilder} as the vertex adapters see it (mixin.BufferBuilderMixin): its format, and its ends
 * (before them the vertices 1.21 code begun are written out).
 */
public interface PendingVertex {
    VertexFormat ladder$format();
}
