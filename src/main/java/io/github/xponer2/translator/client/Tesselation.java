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

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The 1.21 way of drawing vertices on the 1.12.2 Tessellator (runtime, neoforge-1.21.1-redirects.tsv):
 * {@code Tesselator.getInstance().begin(mode, format)} begins its buffer, {@code buffer.addVertex(pose, x, y, z)} is the
 * buffer's {@link VertexConsumer}, {@code buffer.buildOrThrow()} is a {@link MeshData} that
 * {@code BufferUploader.drawWithShader} draws. The mode is the 1.21 {@code VertexFormat.Mode} (by name).
 */
@SideOnly(Side.CLIENT)
public final class Tesselation {
	private Tesselation() {
	}

	/** The GL primitive of a 1.21 VertexFormat.Mode. */
	static int glMode(Object mode) {
		switch (String.valueOf(mode)) {
		case "LINES":
		case "DEBUG_LINES":
			return GL11.GL_LINES;
		case "LINE_STRIP":
		case "DEBUG_LINE_STRIP":
			return GL11.GL_LINE_STRIP;
		case "TRIANGLES":
			return GL11.GL_TRIANGLES;
		case "TRIANGLE_STRIP":
			return GL11.GL_TRIANGLE_STRIP;
		case "TRIANGLE_FAN":
			return GL11.GL_TRIANGLE_FAN;
		default:
			return GL11.GL_QUADS;
		}
	}

	// --- Tessellator -------------------------------------------------------------------------------------------------

	/** Begins the tessellator's buffer (one still drawing from an earlier begin is drawn away first). */
	public static BufferBuilder begin(Tessellator tessellator, Object mode, VertexFormat format) {
		BufferBuilder buffer = tessellator.getBuffer();
		VertexConsumer consumer = VertexConsumer.into(buffer);
		try {
			buffer.begin(glMode(mode), format);
		} catch (IllegalStateException alreadyBuilding) {
			consumer.finishTarget();
			buffer.finishDrawing();
			buffer.begin(glMode(mode), format);
		}
		return buffer;
	}

	// --- BufferBuilder -----------------------------------------------------------------------------------------------

	public static VertexConsumer addVertex(BufferBuilder buffer, Object pose, float x, float y, float z) {
		return VertexConsumer.into(buffer).addVertex(x, y, z);
	}

	public static VertexConsumer addVertex(BufferBuilder buffer, float x, float y, float z) {
		return VertexConsumer.into(buffer).addVertex(x, y, z);
	}

	public static VertexConsumer addVertex(BufferBuilder buffer, org.joml.Vector3f position) {
		return VertexConsumer.into(buffer).addVertex(position.x, position.y, position.z);
	}

	public static VertexConsumer addVertex(BufferBuilder buffer, Object pose, org.joml.Vector3f position) {
		return VertexConsumer.into(buffer).addVertex(position.x, position.y, position.z);
	}

	public static VertexConsumer setColor(BufferBuilder buffer, int argb) {
		return VertexConsumer.into(buffer).setColor(argb);
	}

	public static VertexConsumer setColor(BufferBuilder buffer, float red, float green, float blue, float alpha) {
		return VertexConsumer.into(buffer).setColor(red, green, blue, alpha);
	}

	public static VertexConsumer setUv(BufferBuilder buffer, float u, float v) {
		return VertexConsumer.into(buffer).setUv(u, v);
	}

	public static VertexConsumer setLight(BufferBuilder buffer, int light) {
		return VertexConsumer.into(buffer).setLight(light);
	}

	public static VertexConsumer setNormal(BufferBuilder buffer, float x, float y, float z) {
		return VertexConsumer.into(buffer).setNormal(x, y, z);
	}

	public static void putBulkData(BufferBuilder buffer, PoseStack pose, net.minecraft.client.renderer.block.model.BakedQuad quad, float red, float green,
			float blue, float alpha, int light, int overlay) {
		VertexConsumer.into(buffer).putBulkData(pose, quad, red, green, blue, alpha, light, overlay);
	}

	public static void putBulkData(BufferBuilder buffer, PoseStack pose, net.minecraft.client.renderer.block.model.BakedQuad quad, float red, float green,
			float blue, float alpha, int light, int overlay, boolean readAlpha) {
		VertexConsumer.into(buffer).putBulkData(pose, quad, red, green, blue, alpha, light, overlay);
	}

	public static void putBulkData(BufferBuilder buffer, PoseStack pose, net.minecraft.client.renderer.block.model.BakedQuad quad, float[] brightness,
			float red, float green, float blue, float alpha, int[] lights, int overlay, boolean readAlpha) {
		VertexConsumer.into(buffer).putBulkData(pose, quad, brightness, red, green, blue, alpha, lights, overlay, readAlpha);
	}

	@Nullable
	public static MeshData build(BufferBuilder buffer) {
		return new MeshData(buffer, VertexConsumer.into(buffer));
	}

	public static MeshData buildOrThrow(BufferBuilder buffer) {
		return new MeshData(buffer, VertexConsumer.into(buffer));
	}
}
