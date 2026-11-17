/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
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
