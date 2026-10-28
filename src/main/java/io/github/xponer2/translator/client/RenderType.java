package io.github.xponer2.translator.client;

/**
 * A render state of 1.21 ({@code RenderType}): here the GL state set up before drawing with it and reset after.
 * Client only.
 */
public class RenderType {
    private final String name;
    private final Runnable setup;
    private final Runnable clear;

    public RenderType(String name, Runnable setup, Runnable clear) {
        this.name = name;
        this.setup = setup;
        this.clear = clear;
    }

    // the 1.21 chunk/entity layers used by immediate rendering (blocks atlas)
    private static final RenderType SOLID = new RenderType("solid", () -> {
        net.minecraft.client.renderer.GlStateManager.disableBlend();
        net.minecraft.client.renderer.GlStateManager.enableAlpha();
    }, () -> { });
    private static final RenderType CUTOUT = new RenderType("cutout", () -> {
        net.minecraft.client.renderer.GlStateManager.disableBlend();
        net.minecraft.client.renderer.GlStateManager.enableAlpha();
        net.minecraft.client.renderer.GlStateManager.alphaFunc(516, 0.1F);
    }, () -> { });
    private static final RenderType TRANSLUCENT = new RenderType("translucent", () -> {
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        net.minecraft.client.renderer.GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        net.minecraft.client.renderer.GlStateManager.depthMask(false);
    }, () -> {
        net.minecraft.client.renderer.GlStateManager.depthMask(true);
        net.minecraft.client.renderer.GlStateManager.disableBlend();
    });

    public static RenderType solid() {
        return SOLID;
    }

    public static RenderType cutout() {
        return CUTOUT;
    }

    public static RenderType cutoutMipped() {
        return CUTOUT;
    }

    public static RenderType translucent() {
        return TRANSLUCENT;
    }

    public void setupRenderState() {
        this.setup.run();
    }

    public void clearRenderState() {
        this.clear.run();
    }

    @Override
    public String toString() {
        return this.name;
    }
}
