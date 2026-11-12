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

    @javax.annotation.Nullable
    private static java.lang.reflect.Method execute;

    /**
     * 1.21 {@code recordRenderCall(call)}: run on the render thread, at once when this is it, else at the start of the
     * next frame (the game's task queue). The carried one queued it where nothing on 1.12.2 replays: what mods' worker
     * threads left for the render thread never ran (JourneyMap's freed map textures stayed on the card).
     */
    public static void recordRenderCall(Object call) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            try {
                if (execute == null) {
                    execute = Class.forName("com.mojang.blaze3d.pipeline.RenderCall", false, net.minecraft.launchwrapper.Launch.classLoader)
                            .getMethod("execute");
                }
                execute.invoke(call);
            } catch (java.lang.reflect.InvocationTargetException e) {
                io.github.xponer2.translator.runtime.RuntimeStubs.failed("RenderSystem.recordRenderCall " + call.getClass().getName(), e.getCause());
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                io.github.xponer2.translator.runtime.RuntimeStubs.failed("RenderSystem.recordRenderCall", e);
            }
        });
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
        if (unit >= 0 && unit < SHADER_TEXTURES.length) {
            var bound = Minecraft.getMinecraft().getTextureManager().getTexture(texture);
            SHADER_TEXTURES[unit] = bound != null ? bound.getGlTextureId() : 0;
        }
    }

    /** The textures given to the 1.21 shader's samplers (0: the drawn one, 1: overlay, 2: light map). */
    private static final int[] SHADER_TEXTURES = new int[12];

    /**
     * 1.21 {@code setShaderTexture(unit, id)}: a texture by its GL id for the next draw. Sampler 0 is the texture
     * 1.12.2 draws with: bound now, through 1.12.2's state (the carried 1.21 one kept only the id, and JourneyMap's map
     * tiles were drawn with whatever texture was bound). The overlay and light map units are 1.12.2's own.
     */
    public static void setShaderTexture(int unit, int id) {
        if (unit >= 0 && unit < SHADER_TEXTURES.length) {
            SHADER_TEXTURES[unit] = id;
        }
        if (unit == 0) {
            GlStateManager.bindTexture(id);
        }
    }

    public static int getShaderTexture(int unit) {
        return unit >= 0 && unit < SHADER_TEXTURES.length ? SHADER_TEXTURES[unit] : 0;
    }

    /** 1.21 {@code bindTexture(id)}: through 1.12.2's state, which skips a texture it thinks bound. */
    public static void bindTexture(int id) {
        GlStateManager.bindTexture(id);
    }

    /**
     * 1.21 keeps a separate model view stack; here it is the GL matrix.
     */
    public static PoseStack getModelViewStack() {
        return PoseStack.INSTANCE;
    }

    public static void applyModelViewMatrix() {
        // the GL matrix is current, unless a mod changed 1.21's JOML model view stack (ModelViewStack)
        ModelViewStack.applyModelViewMatrix();
    }
}
