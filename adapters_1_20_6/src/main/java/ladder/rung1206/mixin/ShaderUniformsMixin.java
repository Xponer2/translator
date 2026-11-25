/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

import ladder.rung1206.render.ShaderUniforms;
import net.minecraft.client.renderer.ShaderInstance;

/**
 * 1.21's {@code ShaderInstance.setDefaultUniforms} on 1.20.6 (added to the class): what 1.20.6's VertexBuffer sets before
 * a draw, in 1.21's method (the samplers, the matrices, colour, glint, fog, texture matrix, game time, screen size, line
 * width, the lights). Mods aim at the calls inside it (embeddium's samplers, sodium-extra's fog).
 */
@Mixin(ShaderInstance.class)
abstract class ShaderUniformsMixin implements ShaderUniforms {
    @Override
    public void setDefaultUniforms(VertexFormat.Mode mode, Matrix4f modelView, Matrix4f projection, Window window) {
        ShaderInstance self = (ShaderInstance) (Object) this;
        for (int i = 0; i < 12; i++) {
            int texture = RenderSystem.getShaderTexture(i);
            self.setSampler("Sampler" + i, texture);
        }
        if (self.MODEL_VIEW_MATRIX != null) {
            self.MODEL_VIEW_MATRIX.set(modelView);
        }
        if (self.PROJECTION_MATRIX != null) {
            self.PROJECTION_MATRIX.set(projection);
        }
        if (self.COLOR_MODULATOR != null) {
            self.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        }
        if (self.GLINT_ALPHA != null) {
            self.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
        }
        if (self.FOG_START != null) {
            self.FOG_START.set(RenderSystem.getShaderFogStart());
        }
        if (self.FOG_END != null) {
            self.FOG_END.set(RenderSystem.getShaderFogEnd());
        }
        if (self.FOG_COLOR != null) {
            self.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        }
        if (self.FOG_SHAPE != null) {
            self.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
        }
        if (self.TEXTURE_MATRIX != null) {
            self.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        }
        if (self.GAME_TIME != null) {
            self.GAME_TIME.set(RenderSystem.getShaderGameTime());
        }
        if (self.SCREEN_SIZE != null) {
            self.SCREEN_SIZE.set((float) window.getWidth(), (float) window.getHeight());
        }
        if (self.LINE_WIDTH != null && (mode == VertexFormat.Mode.LINES || mode == VertexFormat.Mode.LINE_STRIP)) {
            self.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
        }
        RenderSystem.setupShaderLights(self);
    }
}
