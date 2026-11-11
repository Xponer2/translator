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

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 {@code RenderStateShard}: one part of a render type's GL state (blending, depth test, culling, the texture, the
 * line width, ...), set up before drawing and cleared after, here with 1.12.2's GlStateManager. Mods extend RenderType
 * to reach these constants (JourneyMap's map tiles and the own render types of many mods): without them the class of
 * every such render type failed, and its drawing with it. Shaders are 1.21's; here a shader state only says whether
 * the vertices are textured.
 */
@SideOnly(Side.CLIENT)
public abstract class RenderStateShard {
	protected final String name;
	protected Runnable setupState;
	private final Runnable clearState;

	public RenderStateShard(String name, Runnable setup, Runnable clear) {
		this.name = name;
		this.setupState = setup;
		this.clearState = clear;
	}

	public void setupRenderState() {
		this.setupState.run();
	}

	public void clearRenderState() {
		this.clearState.run();
	}

	@Override
	public String toString() {
		return this.name;
	}

	private static void blend(int src, int dst, int srcAlpha, int dstAlpha) {
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(src, dst, srcAlpha, dstAlpha);
	}

	private static void noBlend() {
		GlStateManager.disableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
	}

	public static class TransparencyStateShard extends RenderStateShard {
		public TransparencyStateShard(String name, Runnable setup, Runnable clear) {
			super(name, setup, clear);
		}
	}

	/** A shader of 1.21: the Translator draws with the fixed pipeline, textured unless its name says otherwise. */
	public static class ShaderStateShard extends RenderStateShard {
		private final boolean textured;

		public ShaderStateShard(Supplier<?> shader) {
			this("shader", true);
		}

		public ShaderStateShard() {
			this("no_shader", true);
		}

		ShaderStateShard(String name, boolean textured) {
			// the 1.21 shaders without a texture (gui, lines, position_color) drop no pixel by its alpha: 1.12.2's alpha
			// test (0.1) left out the faint parts of slot highlights and gradients
			super(name, textured ? GlStateManager::enableTexture2D : () -> {
				GlStateManager.disableTexture2D();
				GlStateManager.disableAlpha();
			}, textured ? GlStateManager::enableTexture2D : () -> {
				GlStateManager.enableTexture2D();
				GlStateManager.enableAlpha();
			});
			this.textured = textured;
		}

		public boolean textured() {
			return this.textured;
		}
	}

	public static class BooleanStateShard extends RenderStateShard {
		protected final boolean enabled;

		public BooleanStateShard(String name, Runnable setup, Runnable clear, boolean enabled) {
			super(name, setup, clear);
			this.enabled = enabled;
		}
	}

	public static class EmptyTextureStateShard extends RenderStateShard {
		public EmptyTextureStateShard(Runnable setup, Runnable clear) {
			super("texture", setup, clear);
		}

		public EmptyTextureStateShard() {
			super("no_texture", () -> { }, () -> { });
		}

		/** The texture the vertices are drawn with, empty when the shard binds its own (or none). */
		protected Optional<ResourceLocation> cutoutTexture() {
			return Optional.empty();
		}

		@Nullable
		ResourceLocation location() {
			return this.cutoutTexture().orElse(null);
		}
	}

	public static class TextureStateShard extends EmptyTextureStateShard {
		private final Optional<ResourceLocation> texture;
		protected boolean blur;
		protected boolean mipmap;

		public TextureStateShard(ResourceLocation texture, boolean blur, boolean mipmap) {
			super(() -> {
				GlStateManager.enableTexture2D();
				Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
				int filter = blur ? GL11.GL_LINEAR : GL11.GL_NEAREST;
				GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
				GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
			}, () -> { });
			this.texture = Optional.of(texture);
			this.blur = blur;
			this.mipmap = mipmap;
		}

		@Override
		protected Optional<ResourceLocation> cutoutTexture() {
			return this.texture;
		}
	}

	public static class TexturingStateShard extends RenderStateShard {
		public TexturingStateShard(String name, Runnable setup, Runnable clear) {
			super(name, setup, clear);
		}
	}

	public static final class OffsetTexturingStateShard extends TexturingStateShard {
		public OffsetTexturingStateShard(float u, float v) {
			super("offset_texturing", () -> {
				GlStateManager.matrixMode(GL11.GL_TEXTURE);
				GlStateManager.pushMatrix();
				GlStateManager.translate(u, v, 0.0F);
				GlStateManager.matrixMode(GL11.GL_MODELVIEW);
			}, () -> {
				GlStateManager.matrixMode(GL11.GL_TEXTURE);
				GlStateManager.popMatrix();
				GlStateManager.matrixMode(GL11.GL_MODELVIEW);
			});
		}
	}

	public static class LightmapStateShard extends BooleanStateShard {
		public LightmapStateShard(boolean lightmap) {
			super("lightmap", () -> {
				if (lightmap) {
					Minecraft.getMinecraft().entityRenderer.enableLightmap();
				}
			}, () -> {
				if (lightmap) {
					Minecraft.getMinecraft().entityRenderer.disableLightmap();
				}
			}, lightmap);
		}
	}

	public static class OverlayStateShard extends BooleanStateShard {
		public OverlayStateShard(boolean overlay) {
			super("overlay", () -> { }, () -> { }, overlay);
		}
	}

	public static class CullStateShard extends BooleanStateShard {
		public CullStateShard(boolean cull) {
			super("cull", () -> {
				if (cull) {
					GlStateManager.enableCull();
				} else {
					GlStateManager.disableCull();
				}
			}, GlStateManager::enableCull, cull);
		}
	}

	public static class DepthTestStateShard extends RenderStateShard {
		private final String functionName;

		public DepthTestStateShard(String functionName, int function) {
			super("depth_test", () -> {
				if (function == GL11.GL_ALWAYS) {
					GlStateManager.disableDepth();
				} else {
					GlStateManager.enableDepth();
					GlStateManager.depthFunc(function);
				}
			}, () -> {
				GlStateManager.enableDepth();
				GlStateManager.depthFunc(GL11.GL_LEQUAL);
			});
			this.functionName = functionName;
		}

		@Override
		public String toString() {
			return this.name + "[" + this.functionName + "]";
		}
	}

	public static class WriteMaskStateShard extends RenderStateShard {
		public WriteMaskStateShard(boolean color, boolean depth) {
			super("write_mask_state", () -> {
				if (!depth) {
					GlStateManager.depthMask(false);
				}
				if (!color) {
					GlStateManager.colorMask(false, false, false, false);
				}
			}, () -> {
				GlStateManager.depthMask(true);
				GlStateManager.colorMask(true, true, true, true);
			});
		}
	}

	public static class LayeringStateShard extends RenderStateShard {
		public LayeringStateShard(String name, Runnable setup, Runnable clear) {
			super(name, setup, clear);
		}
	}

	public static class OutputStateShard extends RenderStateShard {
		public OutputStateShard(String name, Runnable setup, Runnable clear) {
			super(name, setup, clear);
		}
	}

	public static class LineStateShard extends RenderStateShard {
		public LineStateShard(OptionalDouble width) {
			// no width given: 1.21's own, 2.5 pixels at 1920 wide and more on wider windows (RenderType.lines, block outlines)
			super("line_width", () -> GlStateManager.glLineWidth(width.isPresent() ? (float) width.getAsDouble()
					: Math.max(2.5F, Minecraft.getMinecraft().displayWidth / 1920.0F * 2.5F)), () -> GlStateManager.glLineWidth(1.0F));
		}
	}

	public static class ColorLogicStateShard extends RenderStateShard {
		public ColorLogicStateShard(String name, Runnable setup, Runnable clear) {
			super(name, setup, clear);
		}
	}

	private static ShaderStateShard shader(String name, boolean textured) {
		return new ShaderStateShard(name, textured);
	}

	public static final TransparencyStateShard NO_TRANSPARENCY = new TransparencyStateShard("no_transparency", RenderStateShard::noBlend, () -> { });
	public static final TransparencyStateShard ADDITIVE_TRANSPARENCY = new TransparencyStateShard("additive_transparency", () -> blend(1, 1, 1, 1),
			RenderStateShard::noBlend);
	public static final TransparencyStateShard LIGHTNING_TRANSPARENCY = new TransparencyStateShard("lightning_transparency",
			() -> blend(770, 1, 1, 1), RenderStateShard::noBlend);
	public static final TransparencyStateShard GLINT_TRANSPARENCY = new TransparencyStateShard("glint_transparency", () -> blend(768, 1, 0, 1),
			RenderStateShard::noBlend);
	public static final TransparencyStateShard CRUMBLING_TRANSPARENCY = new TransparencyStateShard("crumbling_transparency",
			() -> blend(774, 768, 1, 0), RenderStateShard::noBlend);
	public static final TransparencyStateShard TRANSLUCENT_TRANSPARENCY = new TransparencyStateShard("translucent_transparency",
			() -> blend(770, 771, 1, 771), RenderStateShard::noBlend);

	public static final ShaderStateShard NO_SHADER = shader("no_shader", true);
	public static final ShaderStateShard POSITION_COLOR_LIGHTMAP_SHADER = shader("position_color_lightmap", false);
	public static final ShaderStateShard POSITION_SHADER = shader("position", false);
	public static final ShaderStateShard POSITION_COLOR_TEX_LIGHTMAP_SHADER = shader("position_color_tex_lightmap", true);
	public static final ShaderStateShard POSITION_TEX_SHADER = shader("position_tex", true);
	public static final ShaderStateShard POSITION_TEX_COLOR_SHADER = shader("position_tex_color", true);
	public static final ShaderStateShard POSITION_COLOR_SHADER = shader("position_color", false);
	public static final ShaderStateShard RENDERTYPE_SOLID_SHADER = shader("rendertype_solid", true);
	public static final ShaderStateShard RENDERTYPE_CUTOUT_MIPPED_SHADER = shader("rendertype_cutout_mipped", true);
	public static final ShaderStateShard RENDERTYPE_CUTOUT_SHADER = shader("rendertype_cutout", true);
	public static final ShaderStateShard RENDERTYPE_TRANSLUCENT_SHADER = shader("rendertype_translucent", true);
	public static final ShaderStateShard RENDERTYPE_TRANSLUCENT_MOVING_BLOCK_SHADER = shader("rendertype_translucent_moving_block", true);
	public static final ShaderStateShard RENDERTYPE_ARMOR_CUTOUT_NO_CULL_SHADER = shader("rendertype_armor_cutout_no_cull", true);
	public static final ShaderStateShard RENDERTYPE_ENTITY_SOLID_SHADER = shader("rendertype_entity_solid", true);
	public static final ShaderStateShard RENDERTYPE_ENTITY_CUTOUT_SHADER = shader("rendertype_entity_cutout", true);
	public static final ShaderStateShard RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER = shader("rendertype_entity_cutout_no_cull", true);
	public static final ShaderStateShard RENDERTYPE_ENTITY_CUTOUT_NO_CULL_Z_OFFSET_SHADER = shader("rendertype_entity_cutout_no_cull_z_offset", true);
	public static final ShaderStateShard RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER = shader("rendertype_item_entity_translucent_cull", true);
	public static final ShaderStateShard RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER = shader("rendertype_entity_translucent_cull", true);
	public static final ShaderStateShard RENDERTYPE_ENTITY_TRANSLUCENT_SHADER = shader("rendertype_entity_translucent", true);
	public static final ShaderStateShard RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER = shader("rendertype_entity_translucent_emissive", true);
	public static final ShaderStateShard RENDERTYPE_ENTITY_SMOOTH_CUTOUT_SHADER = shader("rendertype_entity_smooth_cutout", true);
	public static final ShaderStateShard RENDERTYPE_BEACON_BEAM_SHADER = shader("rendertype_beacon_beam", true);
	public static final ShaderStateShard RENDERTYPE_ENTITY_DECAL_SHADER = shader("rendertype_entity_decal", true);
	public static final ShaderStateShard RENDERTYPE_ENTITY_NO_OUTLINE_SHADER = shader("rendertype_entity_no_outline", true);
	public static final ShaderStateShard RENDERTYPE_ENTITY_SHADOW_SHADER = shader("rendertype_entity_shadow", true);
	public static final ShaderStateShard RENDERTYPE_ENTITY_ALPHA_SHADER = shader("rendertype_entity_alpha", true);
	public static final ShaderStateShard RENDERTYPE_EYES_SHADER = shader("rendertype_eyes", true);
	public static final ShaderStateShard RENDERTYPE_ENERGY_SWIRL_SHADER = shader("rendertype_energy_swirl", true);
	public static final ShaderStateShard RENDERTYPE_LEASH_SHADER = shader("rendertype_leash", false);
	public static final ShaderStateShard RENDERTYPE_WATER_MASK_SHADER = shader("rendertype_water_mask", false);
	public static final ShaderStateShard RENDERTYPE_OUTLINE_SHADER = shader("rendertype_outline", true);
	public static final ShaderStateShard RENDERTYPE_ARMOR_GLINT_SHADER = shader("rendertype_armor_glint", true);
	public static final ShaderStateShard RENDERTYPE_ARMOR_ENTITY_GLINT_SHADER = shader("rendertype_armor_entity_glint", true);
	public static final ShaderStateShard RENDERTYPE_GLINT_TRANSLUCENT_SHADER = shader("rendertype_glint_translucent", true);
	public static final ShaderStateShard RENDERTYPE_GLINT_SHADER = shader("rendertype_glint", true);
	public static final ShaderStateShard RENDERTYPE_ENTITY_GLINT_SHADER = shader("rendertype_entity_glint", true);
	public static final ShaderStateShard RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER = shader("rendertype_entity_glint_direct", true);
	public static final ShaderStateShard RENDERTYPE_CRUMBLING_SHADER = shader("rendertype_crumbling", true);
	public static final ShaderStateShard RENDERTYPE_TEXT_SHADER = shader("rendertype_text", true);
	public static final ShaderStateShard RENDERTYPE_TEXT_BACKGROUND_SHADER = shader("rendertype_text_background", false);
	public static final ShaderStateShard RENDERTYPE_TEXT_INTENSITY_SHADER = shader("rendertype_text_intensity", true);
	public static final ShaderStateShard RENDERTYPE_TEXT_SEE_THROUGH_SHADER = shader("rendertype_text_see_through", true);
	public static final ShaderStateShard RENDERTYPE_TEXT_BACKGROUND_SEE_THROUGH_SHADER = shader("rendertype_text_background_see_through", false);
	public static final ShaderStateShard RENDERTYPE_TEXT_INTENSITY_SEE_THROUGH_SHADER = shader("rendertype_text_intensity_see_through", true);
	public static final ShaderStateShard RENDERTYPE_LIGHTNING_SHADER = shader("rendertype_lightning", false);
	public static final ShaderStateShard RENDERTYPE_TRIPWIRE_SHADER = shader("rendertype_tripwire", true);
	public static final ShaderStateShard RENDERTYPE_END_PORTAL_SHADER = shader("rendertype_end_portal", true);
	public static final ShaderStateShard RENDERTYPE_END_GATEWAY_SHADER = shader("rendertype_end_gateway", true);
	public static final ShaderStateShard RENDERTYPE_CLOUDS_SHADER = shader("rendertype_clouds", false);
	public static final ShaderStateShard RENDERTYPE_LINES_SHADER = shader("rendertype_lines", false);
	public static final ShaderStateShard RENDERTYPE_GUI_SHADER = shader("rendertype_gui", false);
	public static final ShaderStateShard RENDERTYPE_GUI_OVERLAY_SHADER = shader("rendertype_gui_overlay", false);
	public static final ShaderStateShard RENDERTYPE_GUI_TEXT_HIGHLIGHT_SHADER = shader("rendertype_gui_text_highlight", false);
	public static final ShaderStateShard RENDERTYPE_GUI_GHOST_RECIPE_OVERLAY_SHADER = shader("rendertype_gui_ghost_recipe_overlay", false);
	public static final ShaderStateShard RENDERTYPE_BREEZE_WIND_SHADER = shader("rendertype_breeze_wind", true);

	public static final TextureStateShard BLOCK_SHEET_MIPPED = new TextureStateShard(
			net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE, false, true);
	public static final TextureStateShard BLOCK_SHEET = new TextureStateShard(net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE,
			false, false);
	public static final EmptyTextureStateShard NO_TEXTURE = new EmptyTextureStateShard();

	public static final TexturingStateShard DEFAULT_TEXTURING = new TexturingStateShard("default_texturing", () -> { }, () -> { });
	public static final TexturingStateShard GLINT_TEXTURING = new TexturingStateShard("glint_texturing", () -> { }, () -> { });
	public static final TexturingStateShard ENTITY_GLINT_TEXTURING = new TexturingStateShard("entity_glint_texturing", () -> { }, () -> { });
	public static final LightmapStateShard LIGHTMAP = new LightmapStateShard(true);
	public static final LightmapStateShard NO_LIGHTMAP = new LightmapStateShard(false);
	public static final OverlayStateShard OVERLAY = new OverlayStateShard(true);
	public static final OverlayStateShard NO_OVERLAY = new OverlayStateShard(false);
	public static final CullStateShard CULL = new CullStateShard(true);
	public static final CullStateShard NO_CULL = new CullStateShard(false);
	public static final DepthTestStateShard NO_DEPTH_TEST = new DepthTestStateShard("always", GL11.GL_ALWAYS);
	public static final DepthTestStateShard EQUAL_DEPTH_TEST = new DepthTestStateShard("==", GL11.GL_EQUAL);
	public static final DepthTestStateShard LEQUAL_DEPTH_TEST = new DepthTestStateShard("<=", GL11.GL_LEQUAL);
	public static final DepthTestStateShard GREATER_DEPTH_TEST = new DepthTestStateShard(">", GL11.GL_GREATER);
	public static final WriteMaskStateShard COLOR_DEPTH_WRITE = new WriteMaskStateShard(true, true);
	public static final WriteMaskStateShard COLOR_WRITE = new WriteMaskStateShard(true, false);
	public static final WriteMaskStateShard DEPTH_WRITE = new WriteMaskStateShard(false, true);
	public static final LayeringStateShard NO_LAYERING = new LayeringStateShard("no_layering", () -> { }, () -> { });
	public static final LayeringStateShard POLYGON_OFFSET_LAYERING = new LayeringStateShard("polygon_offset_layering", () -> {
		GlStateManager.doPolygonOffset(-1.0F, -10.0F);
		GlStateManager.enablePolygonOffset();
	}, () -> {
		GlStateManager.doPolygonOffset(0.0F, 0.0F);
		GlStateManager.disablePolygonOffset();
	});
	public static final LayeringStateShard VIEW_OFFSET_Z_LAYERING = new LayeringStateShard("view_offset_z_layering", () -> { }, () -> { });
	public static final OutputStateShard MAIN_TARGET = new OutputStateShard("main_target", () -> { }, () -> { });
	// 1.21 draws it into the glowing outline buffer; 1.12.2 builds that buffer itself, so nothing is drawn (an
	// invisible glowing mob's model drawn into the world through walls instead)
	public static final OutputStateShard OUTLINE_TARGET = new OutputStateShard("outline_target", () -> {
		GlStateManager.colorMask(false, false, false, false);
		GlStateManager.depthMask(false);
	}, () -> {
		GlStateManager.colorMask(true, true, true, true);
		GlStateManager.depthMask(true);
	});
	public static final OutputStateShard TRANSLUCENT_TARGET = new OutputStateShard("translucent_target", () -> { }, () -> { });
	public static final OutputStateShard PARTICLES_TARGET = new OutputStateShard("particles_target", () -> { }, () -> { });
	public static final OutputStateShard WEATHER_TARGET = new OutputStateShard("weather_target", () -> { }, () -> { });
	public static final OutputStateShard CLOUDS_TARGET = new OutputStateShard("clouds_target", () -> { }, () -> { });
	public static final OutputStateShard ITEM_ENTITY_TARGET = new OutputStateShard("item_entity_target", () -> { }, () -> { });
	public static final LineStateShard DEFAULT_LINE = new LineStateShard(OptionalDouble.of(1.0D));
	public static final ColorLogicStateShard NO_COLOR_LOGIC = new ColorLogicStateShard("no_color_logic", () -> { }, () -> { });
	public static final ColorLogicStateShard OR_REVERSE_COLOR_LOGIC = new ColorLogicStateShard("or_reverse", () -> {
		GlStateManager.enableColorLogic();
		GlStateManager.colorLogicOp(GlStateManager.LogicOp.OR_REVERSE);
	}, GlStateManager::disableColorLogic);
}
