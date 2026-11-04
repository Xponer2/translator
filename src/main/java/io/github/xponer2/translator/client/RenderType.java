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

/**
 * A render state of 1.21 ({@code RenderType}): here the GL state set up before drawing with it and reset after.
 * Client only.
 */
public class RenderType {
    private final String name;
    private final Runnable setup;
    private final Runnable clear;

    @javax.annotation.Nullable
    private net.minecraft.client.renderer.vertex.VertexFormat format;
    @javax.annotation.Nullable
    private Object mode;
    private int bufferSize = 256;
    private boolean sortOnUpload;

    public RenderType(String name, Runnable setup, Runnable clear) {
        this.name = name;
        this.setup = setup;
        this.clear = clear;
    }

    /** 1.21 {@code new RenderType(name, format, mode, bufferSize, crumbling, sortOnUpload, setup, clear)}. */
    public RenderType(String name, net.minecraft.client.renderer.vertex.VertexFormat format, Object mode, int bufferSize, boolean affectsCrumbling,
            boolean sortOnUpload, Runnable setup, Runnable clear) {
        this(name, setup, clear);
        this.format = format;
        this.mode = mode;
        this.bufferSize = bufferSize;
        this.sortOnUpload = sortOnUpload;
    }

    public net.minecraft.client.renderer.vertex.VertexFormat format() {
        return this.format != null ? this.format : net.minecraft.client.renderer.vertex.DefaultVertexFormats.BLOCK;
    }

    /** The 1.21 vertex mode (VertexFormat.Mode, carried with the API), null for the Translator's own types (quads). */
    @javax.annotation.Nullable
    public Object mode() {
        return this.mode;
    }

    public int bufferSize() {
        return this.bufferSize;
    }

    public boolean sortOnUpload() {
        return this.sortOnUpload;
    }

    /** The chunk layer of a block render type (solid 0, cutout mipped 1, cutout 2, translucent 3), -1 for the others. */
    public int getChunkLayerId() {
        switch (this.name) {
        case "solid":
            return 0;
        case "cutout_mipped":
            return 1;
        case "cutout":
            return 2;
        case "translucent":
            return 3;
        default:
            return -1;
        }
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

    // --- entity layers: a texture of their own (the immediate consumer binds it), culling and blending as in 1.21 ---

    @javax.annotation.Nullable
    private net.minecraft.util.ResourceLocation texture;
    private static final java.util.Map<String, RenderType> ENTITY = new java.util.concurrent.ConcurrentHashMap<>();

    /** The texture an entity layer draws with; null for the blocks atlas. */
    @javax.annotation.Nullable
    public net.minecraft.util.ResourceLocation texture() {
        return this.texture;
    }

    private static RenderType entity(String kind, net.minecraft.util.ResourceLocation texture, boolean cull, boolean blend) {
        return ENTITY.computeIfAbsent(kind + ' ' + texture, key -> {
            var type = new RenderType(kind, () -> {
                if (cull) {
                    net.minecraft.client.renderer.GlStateManager.enableCull();
                } else {
                    net.minecraft.client.renderer.GlStateManager.disableCull();
                }
                net.minecraft.client.renderer.GlStateManager.enableAlpha();
                net.minecraft.client.renderer.GlStateManager.alphaFunc(516, 0.1F);
                if (blend) {
                    net.minecraft.client.renderer.GlStateManager.enableBlend();
                    net.minecraft.client.renderer.GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                } else {
                    net.minecraft.client.renderer.GlStateManager.disableBlend();
                }
            }, () -> {
                net.minecraft.client.renderer.GlStateManager.enableCull();
                net.minecraft.client.renderer.GlStateManager.disableBlend();
            });
            type.texture = texture;
            return type;
        });
    }

    public static RenderType entitySolid(net.minecraft.util.ResourceLocation texture) {
        return entity("entity_solid", texture, true, false);
    }

    public static RenderType entityCutout(net.minecraft.util.ResourceLocation texture) {
        return entity("entity_cutout", texture, true, false);
    }

    public static RenderType entityCutoutNoCull(net.minecraft.util.ResourceLocation texture) {
        return entity("entity_cutout_no_cull", texture, false, false);
    }

    public static RenderType entityCutoutNoCull(net.minecraft.util.ResourceLocation texture, boolean outline) {
        return entityCutoutNoCull(texture);
    }

    public static RenderType entityCutoutNoCullZOffset(net.minecraft.util.ResourceLocation texture) {
        return entityCutoutNoCull(texture);
    }

    public static RenderType entitySmoothCutout(net.minecraft.util.ResourceLocation texture) {
        return entity("entity_smooth_cutout", texture, true, false);
    }

    public static RenderType entityTranslucent(net.minecraft.util.ResourceLocation texture) {
        return entity("entity_translucent", texture, false, true);
    }

    public static RenderType entityTranslucent(net.minecraft.util.ResourceLocation texture, boolean outline) {
        return entityTranslucent(texture);
    }

    public static RenderType entityTranslucentCull(net.minecraft.util.ResourceLocation texture) {
        return entity("entity_translucent_cull", texture, true, true);
    }

    public static RenderType itemEntityTranslucentCull(net.minecraft.util.ResourceLocation texture) {
        return entityTranslucentCull(texture);
    }

    public static RenderType entityNoOutline(net.minecraft.util.ResourceLocation texture) {
        return entity("entity_no_outline", texture, false, true);
    }

    public static RenderType entityDecal(net.minecraft.util.ResourceLocation texture) {
        return entity("entity_decal", texture, true, true);
    }

    public static RenderType armorCutoutNoCull(net.minecraft.util.ResourceLocation texture) {
        return entity("armor_cutout_no_cull", texture, false, false);
    }

    public static RenderType eyes(net.minecraft.util.ResourceLocation texture) {
        return entity("eyes", texture, false, true);
    }

    public static RenderType energySwirl(net.minecraft.util.ResourceLocation texture, float u, float v) {
        return entity("energy_swirl", texture, false, true);
    }

    public static RenderType solid() {
        return SOLID;
    }

    public static RenderType cutout() {
        return CUTOUT;
    }

    public static RenderType cutoutMipped() {
        return CUTOUT;
    }

    /** The block layers (Registrate checks a block's layer is one of them). */
    public static java.util.List<RenderType> chunkBufferLayers() {
        return java.util.Arrays.asList(SOLID, CUTOUT, TRANSLUCENT);
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
