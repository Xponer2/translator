/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package io.github.xponer2.translator.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/**
 * The 1.21 render state calls on 1.12.2 {@code GlStateManager} (fixed function pipeline: no shaders, the "shader"
 * color and texture are the GL color and the bound texture). Client only.
 */
public final class RenderSystem {
    private RenderSystem() { }

    public static void enableBlend() {
        GlStateManager.enableBlend();
    }

    public static void disableBlend() {
        GlStateManager.disableBlend();
    }

    public static void defaultBlendFunc() {
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }

    public static void blendFunc(GlStateManager.SourceFactor source, GlStateManager.DestFactor dest) {
        GlStateManager.blendFunc(source, dest);
    }

    public static void blendFunc(int source, int dest) {
        GlStateManager.blendFunc(source, dest);
    }

    public static void enableDepthTest() {
        GlStateManager.enableDepth();
    }

    public static void disableDepthTest() {
        GlStateManager.disableDepth();
    }

    public static void setShaderColor(float r, float g, float b, float a) {
        GlStateManager.color(r, g, b, a);
    }

    public static void setShaderTexture(int unit, ResourceLocation texture) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
    }

    /**
     * 1.21 keeps a separate model view stack; here it is the GL matrix.
     */
    public static PoseStack getModelViewStack() {
        return PoseStack.INSTANCE;
    }

    public static void applyModelViewMatrix() {
        // the GL matrix is already current
    }
}
