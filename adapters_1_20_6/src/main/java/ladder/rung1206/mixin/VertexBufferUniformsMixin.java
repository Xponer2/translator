/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import ladder.rung1206.render.ShaderUniforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;

/**
 * 1.20.6's {@code VertexBuffer._drawWithShader} in 1.21's shape: the shader's default uniforms by 1.21's
 * setDefaultUniforms (ShaderUniformsMixin, the same settings in the same order), then apply, draw, clear.
 */
@Mixin(value = VertexBuffer.class, priority = 500)
abstract class VertexBufferUniformsMixin {
    @Shadow
    private VertexFormat.Mode mode;

    @Shadow
    public abstract void draw();

    // an overwrite, not an injector: the mods' points are inside 1.21's setDefaultUniforms, which this calls
    /**
     * @author ladder
     * @reason 1.21's shape of a shader draw (1.20.6's settings, in its order)
     */
    @Overwrite
    private void _drawWithShader(Matrix4f modelView, Matrix4f projection, ShaderInstance shader) {
        ((ShaderUniforms) (Object) shader).setDefaultUniforms(this.mode, modelView, projection, Minecraft.getInstance().getWindow());
        shader.apply();
        this.draw();
        shader.clear();
    }
}
