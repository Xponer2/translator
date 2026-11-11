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
public class RenderType extends RenderStateShard {

    @javax.annotation.Nullable
    private net.minecraft.client.renderer.vertex.VertexFormat format;
    @javax.annotation.Nullable
    private Object mode;
    private int bufferSize = 256;
    private boolean sortOnUpload;
    private boolean affectsCrumbling;

    public RenderType(String name, Runnable setup, Runnable clear) {
        super(name, setup, clear);
    }

    /** 1.21 {@code new RenderType(name, format, mode, bufferSize, crumbling, sortOnUpload, setup, clear)}. */
    public RenderType(String name, net.minecraft.client.renderer.vertex.VertexFormat format, Object mode, int bufferSize, boolean affectsCrumbling,
            boolean sortOnUpload, Runnable setup, Runnable clear) {
        this(name, setup, clear);
        this.format = format;
        this.mode = mode;
        this.bufferSize = bufferSize;
        this.sortOnUpload = sortOnUpload;
        this.affectsCrumbling = affectsCrumbling;
    }

    public boolean affectsCrumbling() {
        return this.affectsCrumbling;
    }

    public net.minecraft.client.renderer.vertex.VertexFormat format() {
        return this.format != null ? this.format : net.minecraft.client.renderer.vertex.DefaultVertexFormats.BLOCK;
    }

    /** The 1.21 vertex mode (VertexFormat.Mode, carried with the API), null for the Translator's own types (quads). */
    @javax.annotation.Nullable
    public Object mode() {
        if (this.mode == null && this.modeName != null) {
            // a Translator type of another primitive (lines): the carried enum constant, looked up when first asked
            this.mode = carriedMode(this.modeName);
        }
        return this.mode;
    }

    /** The 1.21 mode by name of the Translator's own types that aren't quads (the carried enum isn't there offline). */
    @javax.annotation.Nullable
    private String modeName;

    /** The GL primitive the vertices are drawn as. */
    int glMode() {
        Object mode = this.mode != null ? this.mode : this.modeName;
        return mode != null ? Tesselation.glMode(mode) : org.lwjgl.opengl.GL11.GL_QUADS;
    }

    @javax.annotation.Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object carriedMode(String name) {
        try {
            return Enum.valueOf((Class) Class.forName("com.mojang.blaze3d.vertex.VertexFormat$Mode", true, net.minecraft.launchwrapper.Launch.classLoader),
                    name);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** Whether the vertices are drawn with a texture (the immediate consumer binds one only then). */
    boolean textured() {
        return true;
    }

    /** Whether the vertices are pulled a little towards the camera (1.21 VIEW_OFFSET_Z_LAYERING: outlines over faces). */
    boolean viewOffsetZ() {
        return false;
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

    // --- the 1.21 GUI and line types, with the shards 1.21 gives them (14 mods fill slot highlights and overlays with
    // guiOverlay, 11 draw block outlines with lines; without them the call was left empty and nothing drawn)

    private static final RenderType GUI = create("gui", net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR, null, 786432,
            CompositeState.builder().setShaderState(RENDERTYPE_GUI_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));
    private static final RenderType GUI_OVERLAY = create("gui_overlay", net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR, null,
            1536, CompositeState.builder().setShaderState(RENDERTYPE_GUI_OVERLAY_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    private static final RenderType GUI_TEXT_HIGHLIGHT = create("gui_text_highlight", net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR,
            null, 1536, CompositeState.builder().setShaderState(RENDERTYPE_GUI_TEXT_HIGHLIGHT_SHADER).setTransparencyState(NO_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST).setColorLogicState(OR_REVERSE_COLOR_LOGIC).createCompositeState(false));
    private static final RenderType GUI_GHOST_RECIPE_OVERLAY = create("gui_ghost_recipe_overlay",
            net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR, null, 1536, CompositeState.builder()
                    .setShaderState(RENDERTYPE_GUI_GHOST_RECIPE_OVERLAY_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(GREATER_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    private static final RenderType LINES = lineType("lines", "LINES");
    private static final RenderType LINE_STRIP = lineType("line_strip", "LINE_STRIP");
    private static final java.util.Map<net.minecraft.util.ResourceLocation, RenderType> OUTLINE = new java.util.concurrent.ConcurrentHashMap<>();

    private static RenderType lineType(String name, String mode) {
        RenderType type = create(name, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR, null, 1536, CompositeState.builder()
                .setShaderState(RENDERTYPE_LINES_SHADER).setLineState(new LineStateShard(java.util.OptionalDouble.empty()))
                .setLayeringState(VIEW_OFFSET_Z_LAYERING).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setOutputState(ITEM_ENTITY_TARGET)
                .setWriteMaskState(COLOR_DEPTH_WRITE).setCullState(NO_CULL).createCompositeState(false));
        type.modeName = mode;
        return type;
    }

    public static RenderType gui() {
        return GUI;
    }

    public static RenderType guiOverlay() {
        return GUI_OVERLAY;
    }

    public static RenderType guiTextHighlight() {
        return GUI_TEXT_HIGHLIGHT;
    }

    public static RenderType guiGhostRecipeOverlay() {
        return GUI_GHOST_RECIPE_OVERLAY;
    }

    public static RenderType lines() {
        return LINES;
    }

    public static RenderType lineStrip() {
        return LINE_STRIP;
    }

    /** 1.21 {@code outline(texture)}: the glowing outline of a model, which 1.12.2 draws itself (see OUTLINE_TARGET). */
    public static RenderType outline(net.minecraft.util.ResourceLocation texture) {
        return OUTLINE.computeIfAbsent(texture, location -> create("outline", net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX_COLOR,
                null, 1536, CompositeState.builder().setShaderState(RENDERTYPE_OUTLINE_SHADER).setTextureState(new TextureStateShard(location, false, false))
                        .setCullState(NO_CULL).setDepthTestState(NO_DEPTH_TEST).setOutputState(OUTLINE_TARGET)
                        .createCompositeState(OutlineProperty.IS_OUTLINE)));
    }

    /** Whether the type binds its texture itself (a mod's texture state): the consumer's binding comes first then. */
    public boolean bindsOwnTexture() {
        return false;
    }

    // --- 1.21 composite render types: RenderType.create(name, format, mode, size, ..., CompositeState) ------------------

    /** 1.21 {@code RenderType.create(...)}: a type whose GL state is that of its shards, in the order 1.21 sets them. */
    public static CompositeRenderType create(String name, net.minecraft.client.renderer.vertex.VertexFormat format, Object mode, int bufferSize,
            boolean affectsCrumbling, boolean sortOnUpload, CompositeState state) {
        return new CompositeRenderType(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, state);
    }

    public static CompositeRenderType create(String name, net.minecraft.client.renderer.vertex.VertexFormat format, Object mode, int bufferSize,
            CompositeState state) {
        return create(name, format, mode, bufferSize, false, false, state);
    }

    /** 1.21 {@code RenderType.OutlineProperty}. */
    public enum OutlineProperty {
        NONE,
        IS_OUTLINE,
        AFFECTS_OUTLINE
    }

    /** 1.21 {@code RenderType.CompositeState}: the shards of a type. */
    public static final class CompositeState {
        final RenderStateShard.EmptyTextureStateShard textureState;
        final RenderStateShard.ShaderStateShard shaderState;
        final java.util.List<RenderStateShard> states;
        final OutlineProperty outlineProperty;

        CompositeState(RenderStateShard.EmptyTextureStateShard texture, RenderStateShard.ShaderStateShard shader,
                java.util.List<RenderStateShard> states, OutlineProperty outline) {
            this.textureState = texture;
            this.shaderState = shader;
            this.states = states;
            this.outlineProperty = outline;
        }

        public static CompositeStateBuilder builder() {
            return new CompositeStateBuilder();
        }

        /** 1.21 {@code RenderType.CompositeState.CompositeStateBuilder}. */
        public static final class CompositeStateBuilder {
            private RenderStateShard.EmptyTextureStateShard textureState = RenderStateShard.NO_TEXTURE;
            private RenderStateShard.ShaderStateShard shaderState = RenderStateShard.NO_SHADER;
            private RenderStateShard.TransparencyStateShard transparencyState = RenderStateShard.NO_TRANSPARENCY;
            private RenderStateShard.DepthTestStateShard depthTestState = RenderStateShard.LEQUAL_DEPTH_TEST;
            private RenderStateShard.CullStateShard cullState = RenderStateShard.CULL;
            private RenderStateShard.LightmapStateShard lightmapState = RenderStateShard.NO_LIGHTMAP;
            private RenderStateShard.OverlayStateShard overlayState = RenderStateShard.NO_OVERLAY;
            private RenderStateShard.LayeringStateShard layeringState = RenderStateShard.NO_LAYERING;
            private RenderStateShard.OutputStateShard outputState = RenderStateShard.MAIN_TARGET;
            private RenderStateShard.TexturingStateShard texturingState = RenderStateShard.DEFAULT_TEXTURING;
            private RenderStateShard.WriteMaskStateShard writeMaskState = RenderStateShard.COLOR_DEPTH_WRITE;
            private RenderStateShard.LineStateShard lineState = RenderStateShard.DEFAULT_LINE;
            private RenderStateShard.ColorLogicStateShard colorLogicState = RenderStateShard.NO_COLOR_LOGIC;

            CompositeStateBuilder() {
            }

            public CompositeStateBuilder setTextureState(RenderStateShard.EmptyTextureStateShard state) {
                this.textureState = state;
                return this;
            }

            public CompositeStateBuilder setShaderState(RenderStateShard.ShaderStateShard state) {
                this.shaderState = state;
                return this;
            }

            public CompositeStateBuilder setTransparencyState(RenderStateShard.TransparencyStateShard state) {
                this.transparencyState = state;
                return this;
            }

            public CompositeStateBuilder setDepthTestState(RenderStateShard.DepthTestStateShard state) {
                this.depthTestState = state;
                return this;
            }

            public CompositeStateBuilder setCullState(RenderStateShard.CullStateShard state) {
                this.cullState = state;
                return this;
            }

            public CompositeStateBuilder setLightmapState(RenderStateShard.LightmapStateShard state) {
                this.lightmapState = state;
                return this;
            }

            public CompositeStateBuilder setOverlayState(RenderStateShard.OverlayStateShard state) {
                this.overlayState = state;
                return this;
            }

            public CompositeStateBuilder setLayeringState(RenderStateShard.LayeringStateShard state) {
                this.layeringState = state;
                return this;
            }

            public CompositeStateBuilder setOutputState(RenderStateShard.OutputStateShard state) {
                this.outputState = state;
                return this;
            }

            public CompositeStateBuilder setTexturingState(RenderStateShard.TexturingStateShard state) {
                this.texturingState = state;
                return this;
            }

            public CompositeStateBuilder setWriteMaskState(RenderStateShard.WriteMaskStateShard state) {
                this.writeMaskState = state;
                return this;
            }

            public CompositeStateBuilder setLineState(RenderStateShard.LineStateShard state) {
                this.lineState = state;
                return this;
            }

            public CompositeStateBuilder setColorLogicState(RenderStateShard.ColorLogicStateShard state) {
                this.colorLogicState = state;
                return this;
            }

            public CompositeState createCompositeState(boolean outline) {
                return this.createCompositeState(outline ? OutlineProperty.AFFECTS_OUTLINE : OutlineProperty.NONE);
            }

            public CompositeState createCompositeState(OutlineProperty outline) {
                // the order 1.21 sets them in: texture, shader, transparency, depth test, cull, lightmap, overlay, layering,
                // output, texturing, write mask, line, color logic
                var states = java.util.Arrays.<RenderStateShard>asList(this.textureState, this.shaderState, this.transparencyState,
                        this.depthTestState, this.cullState, this.lightmapState, this.overlayState, this.layeringState, this.outputState,
                        this.texturingState, this.writeMaskState, this.lineState, this.colorLogicState);
                return new CompositeState(this.textureState, this.shaderState, states, outline);
            }
        }
    }

    /** 1.21 {@code RenderType.CompositeRenderType}: a type made of a composite state. */
    public static final class CompositeRenderType extends RenderType {
        private final CompositeState state;
        private final boolean ownTexture;

        CompositeRenderType(String name, net.minecraft.client.renderer.vertex.VertexFormat format, Object mode, int bufferSize, boolean affectsCrumbling,
                boolean sortOnUpload, CompositeState state) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, () -> state.states.forEach(shard -> {
                // a mod's shard constant that couldn't be made is null: left out rather than failing every frame
                if (shard != null) {
                    shard.setupRenderState();
                }
            }), () -> state.states.forEach(shard -> {
                if (shard != null) {
                    shard.clearRenderState();
                }
            }));
            this.state = state;
            var location = state.textureState == null ? null : state.textureState.location();
            this.setTexture(location);
            this.ownTexture = location == null && state.textureState != null && state.textureState != RenderStateShard.NO_TEXTURE;
            this.textured = state.shaderState == null || state.shaderState.textured() || location != null;
            this.viewOffsetZ = state.states.contains(RenderStateShard.VIEW_OFFSET_Z_LAYERING);
        }

        private final boolean textured;
        private final boolean viewOffsetZ;

        @Override
        boolean textured() {
            return this.textured;
        }

        @Override
        boolean viewOffsetZ() {
            return this.viewOffsetZ;
        }

        public CompositeState state() {
            return this.state;
        }

        public boolean isOutline() {
            return this.state.outlineProperty == OutlineProperty.IS_OUTLINE;
        }

        @Override
        public boolean bindsOwnTexture() {
            return this.ownTexture;
        }
    }

    void setTexture(@javax.annotation.Nullable net.minecraft.util.ResourceLocation texture) {
        this.texture = texture;
    }
}
