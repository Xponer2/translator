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

    // --- the render thread: 1.12.2 renders on the client thread, which also loads the mods -----------------------------
    // Without these the 1.21 RenderSystem carried for the calls missing here checked its own render thread, which
    // nothing sets on 1.12.2: "Rendersystem called from wrong thread" on every frame of JourneyMap's minimap.

    public static boolean isOnRenderThread() {
        return Minecraft.getMinecraft().isCallingFromMinecraftThread();
    }

    public static boolean isOnRenderThreadOrInit() {
        return isOnRenderThread();
    }

    public static boolean isInInitPhase() {
        return false;
    }

    public static void assertOnRenderThread() {
        if (!isOnRenderThread()) {
            throw new IllegalStateException("Rendersystem called from wrong thread");
        }
    }

    public static void assertOnRenderThreadOrInit() {
        assertOnRenderThread();
    }

    public static void initRenderThread() {
    }

    public static void initGameThread(boolean assertNotRenderThread) {
    }

    public static void beginInitialization() {
    }

    public static void finishInitialization() {
    }

    /** 1.21 {@code clear(mask, checkError)}: the buffers cleared (1.12.2's GL clear). */
    public static void clear(int mask, boolean checkError) {
        GlStateManager.clear(mask);
    }

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

    public static void blendFuncSeparate(GlStateManager.SourceFactor source, GlStateManager.DestFactor dest,
            GlStateManager.SourceFactor sourceAlpha, GlStateManager.DestFactor destAlpha) {
        GlStateManager.tryBlendFuncSeparate(source, dest, sourceAlpha, destAlpha);
    }

    public static void blendFuncSeparate(int source, int dest, int sourceAlpha, int destAlpha) {
        GlStateManager.tryBlendFuncSeparate(source, dest, sourceAlpha, destAlpha);
    }

    public static void enableDepthTest() {
        GlStateManager.enableDepth();
    }

    public static void disableDepthTest() {
        GlStateManager.disableDepth();
    }

    /** 1.21's shader color, kept apart from the GL color 1.12.2's text drawing changes (sprites are drawn in it). */
    private static final float[] SHADER_COLOR = {1.0F, 1.0F, 1.0F, 1.0F};

    public static void setShaderColor(float r, float g, float b, float a) {
        SHADER_COLOR[0] = r;
        SHADER_COLOR[1] = g;
        SHADER_COLOR[2] = b;
        SHADER_COLOR[3] = a;
        GlStateManager.color(r, g, b, a);
    }

    public static float[] getShaderColor() {
        return SHADER_COLOR;
    }

    /** The GL color set to the shader color again (after text drawing changed it). */
    static void applyShaderColor() {
        GlStateManager.color(SHADER_COLOR[0], SHADER_COLOR[1], SHADER_COLOR[2], SHADER_COLOR[3]);
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
