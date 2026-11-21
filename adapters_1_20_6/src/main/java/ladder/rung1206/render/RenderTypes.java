/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.render;

import ladder.rung1206.mixin.RenderTypeAccess;
import ladder.rung1206.provided.MeshData;
import net.minecraft.client.renderer.RenderType;

/** 1.21's render type members on 1.20.6: whether its quads are sorted on upload, drawing a finished mesh with its state. */
public final class RenderTypes {
    private RenderTypes() {
    }

    /** 1.21's {@code renderType.sortOnUpload()} (and its public field) */
    public static boolean sortOnUpload(RenderType type) {
        return ((RenderTypeAccess) type).ladder$sortOnUpload();
    }

    /** 1.21's {@code renderType.draw(mesh)}: the type's render state around the mesh's draw */
    public static void draw(RenderType type, MeshData mesh) {
        type.setupRenderState();
        VertexAdapters.drawWithShader(mesh);
        type.clearRenderState();
    }
}
