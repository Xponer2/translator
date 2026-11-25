/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.render;

import ladder.rung1206.provided.MeshData;
import ladder.rung1206.shape.AddedTo;

/**
 * The members an adapters' mixin adds to RenderType (RenderTypeDrawMixin), for the other adapters' calls: the build
 * turns a cast to this class and its calls into RenderType's own, so the bytecode makes 1.21's call (a mod's point).
 * Never instantiated.
 */
@AddedTo("net/minecraft/client/renderer/RenderType")
public abstract class RenderTypeAdded {
    private RenderTypeAdded() {
    }

    /** 1.21's {@code RenderType.draw(mesh)} */
    public abstract void draw(MeshData mesh);
}
