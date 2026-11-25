/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.VertexFormat;

/**
 * 1.21's {@code ShaderInstance.setDefaultUniforms(mode, modelView, projection, window)}, which the adapters' mixin adds to
 * 1.20.6's ShaderInstance (ShaderUniformsMixin); 1.20.6's VertexBuffer calls it through this interface
 * (VertexBufferUniformsMixin), so the mods' mixins into it see the game's own draws.
 */
public interface ShaderUniforms {
    void setDefaultUniforms(VertexFormat.Mode mode, Matrix4f modelView, Matrix4f projection, Window window);
}
