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

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The 1.21 calls that draw a baked block or item model into a vertex consumer, in a block entity renderer or an
 * entity renderer ({@code tesselateBlock}, {@code renderModel}, {@code renderBatched}, {@code renderModelLists}): the
 * model's quads, read from their 1.12.2 vertex data, tinted as the game tints them, at the current pose.
 */
@SideOnly(Side.CLIENT)
public final class ModelRendering {
	private static final EnumFacing[] SIDES_AND_NONE = {EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST,
			EnumFacing.EAST, null};

	private ModelRendering() {
	}

	// --- ModelBlockRenderer -------------------------------------------------------------------------------------------

	public static void tesselateBlock(BlockModelRenderer renderer, IBlockAccess level, IBakedModel model, IBlockState state, BlockPos pos,
			PoseStack poseStack, VertexConsumer buffer, boolean checkSides, Random random, long seed, int overlay) {
		block(model, state, level, pos, buffer, checkSides, -1, light(level, pos, state), overlay);
	}

	public static void tesselateBlock(BlockModelRenderer renderer, IBlockAccess level, IBakedModel model, IBlockState state, BlockPos pos,
			PoseStack poseStack, VertexConsumer buffer, boolean checkSides, Random random, long seed, int overlay, ModelData data, RenderType type) {
		tesselateBlock(renderer, level, model, state, pos, poseStack, buffer, checkSides, random, seed, overlay);
	}

	/** 1.21 {@code renderModel(pose, buffer, state, model, red, green, blue, light, overlay)}: the model in one color. */
	public static void renderModel(BlockModelRenderer renderer, PoseStack pose, VertexConsumer buffer, @Nullable IBlockState state, IBakedModel model,
			float red, float green, float blue, int light, int overlay) {
		int color = 0xFF000000 | clamp(red) << 16 | clamp(green) << 8 | clamp(blue);
		for (EnumFacing side : SIDES_AND_NONE) {
			quads(model.getQuads(state, side, 0L), buffer, color, light, overlay, null, null, null, null);
		}
	}

	public static void renderModel(BlockModelRenderer renderer, PoseStack pose, VertexConsumer buffer, @Nullable IBlockState state, IBakedModel model,
			float red, float green, float blue, int light, int overlay, ModelData data, RenderType type) {
		renderModel(renderer, pose, buffer, state, model, red, green, blue, light, overlay);
	}

	// --- BlockRenderDispatcher ----------------------------------------------------------------------------------------

	public static void renderBatched(BlockRendererDispatcher dispatcher, IBlockState state, BlockPos pos, IBlockAccess level, PoseStack poseStack,
			VertexConsumer buffer, boolean checkSides, Random random) {
		block(dispatcher.getModelForState(state), state, level, pos, buffer, checkSides, -1, light(level, pos, state), OverlayTexture.NO_OVERLAY);
	}

	public static void renderBatched(BlockRendererDispatcher dispatcher, IBlockState state, BlockPos pos, IBlockAccess level, PoseStack poseStack,
			VertexConsumer buffer, boolean checkSides, Random random, ModelData data, RenderType type) {
		renderBatched(dispatcher, state, pos, level, poseStack, buffer, checkSides, random);
	}

	// --- ItemRenderer -------------------------------------------------------------------------------------------------

	/** 1.21 {@code itemRenderer.renderModelLists(model, stack, light, overlay, pose, buffer)}: an item model's quads. */
	public static void renderModelLists(RenderItem renderer, IBakedModel model, ItemStack stack, int light, int overlay, PoseStack poseStack,
			VertexConsumer buffer) {
		for (EnumFacing side : SIDES_AND_NONE) {
			quads(model.getQuads(null, side, 0L), buffer, -1, light, overlay, stack, null, null, null);
		}
	}

	// --- the quads ----------------------------------------------------------------------------------------------------

	private static void block(IBakedModel model, IBlockState state, IBlockAccess level, BlockPos pos, VertexConsumer buffer, boolean checkSides,
			int color, int light, int overlay) {
		IBlockState actual = level == null ? state : state.getActualState(level, pos);
		IBlockState extended = level == null ? actual : actual.getBlock().getExtendedState(actual, level, pos);
		for (EnumFacing side : SIDES_AND_NONE) {
			if (side != null && checkSides && level != null && !actual.shouldSideBeRendered(level, pos, side)) {
				continue;
			}
			quads(model.getQuads(extended, side, 0L), buffer, color, light, overlay, null, actual, level, pos);
		}
	}

	private static int light(@Nullable IBlockAccess level, BlockPos pos, IBlockState state) {
		return level == null ? 0xF000F0 : state.getPackedLightmapCoords(level, pos);
	}

	private static void quads(List<BakedQuad> quads, VertexConsumer buffer, int color, int light, int overlay, @Nullable ItemStack stack,
			@Nullable IBlockState state, @Nullable IBlockAccess level, @Nullable BlockPos pos) {
		Minecraft mc = Minecraft.getMinecraft();
		for (BakedQuad quad : quads) {
			int tinted = color;
			if (quad.hasTintIndex()) {
				int tint = stack != null ? mc.getItemColors().colorMultiplier(stack, quad.getTintIndex())
						: state != null ? mc.getBlockColors().colorMultiplier(state, level, pos, quad.getTintIndex()) : -1;
				tinted = multiply(color, tint);
			}
			emit(quad, buffer, tinted, light, overlay);
		}
	}

	/** One quad's four vertices from its vertex data (position and texture by the format's offsets). */
	static void emit(BakedQuad quad, VertexConsumer buffer, int color, int light, int overlay) {
		VertexFormat format = quad.getFormat();
		int[] data = quad.getVertexData();
		int stride = format.getIntegerSize();
		int position = -1;
		int uv = -1;
		for (int i = 0; i < format.getElementCount(); i++) {
			VertexFormatElement element = format.getElement(i);
			if (element.getUsage() == VertexFormatElement.EnumUsage.POSITION) {
				position = format.getOffset(i) / 4;
			} else if (element.getUsage() == VertexFormatElement.EnumUsage.UV && element.getIndex() == 0) {
				uv = format.getOffset(i) / 4;
			}
		}
		if (position < 0 || uv < 0) {
			return;
		}
		EnumFacing face = quad.getFace();
		float nx = face == null ? 0 : face.getXOffset();
		float ny = face == null ? 1 : face.getYOffset();
		float nz = face == null ? 0 : face.getZOffset();
		for (int v = 0; v < 4; v++) {
			int base = v * stride;
			buffer.addVertex(Float.intBitsToFloat(data[base + position]), Float.intBitsToFloat(data[base + position + 1]),
					Float.intBitsToFloat(data[base + position + 2]), color, Float.intBitsToFloat(data[base + uv]),
					Float.intBitsToFloat(data[base + uv + 1]), overlay, light, nx, ny, nz);
		}
	}

	private static int multiply(int color, int tint) {
		if (tint == -1) {
			return color;
		}
		int r = (color >> 16 & 0xFF) * (tint >> 16 & 0xFF) / 255;
		int g = (color >> 8 & 0xFF) * (tint >> 8 & 0xFF) / 255;
		int b = (color & 0xFF) * (tint & 0xFF) / 255;
		return (color & 0xFF000000) | r << 16 | g << 8 | b;
	}

	private static int clamp(float channel) {
		return Math.max(0, Math.min(255, (int) (channel * 255.0F)));
	}
}
