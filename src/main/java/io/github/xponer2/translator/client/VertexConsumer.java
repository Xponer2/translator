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

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 {@code VertexConsumer}, an interface as 1.21's: the mods implement it with their own consumers (Create's Ponder
 * scenes, Patchouli's and Ars Nouveau's fluid and tint wrappers, Moonlight's quad builder, Xaero's radar icons,
 * GuideMe's scenes, NeoForge's own multi consumer). The game's buffers are {@link BufferVertexConsumer}. The six
 * methods of 1.21 are abstract, the rest default as 1.21's (and NeoForge's extension) do.
 */
@SideOnly(Side.CLIENT)
public interface VertexConsumer {
	VertexConsumer addVertex(float x, float y, float z);

	VertexConsumer setColor(int red, int green, int blue, int alpha);

	VertexConsumer setUv(float u, float v);

	VertexConsumer setUv1(int u, int v);

	VertexConsumer setUv2(int u, int v);

	VertexConsumer setNormal(float x, float y, float z);

	/** The consumer writing into a buffer a mod began (one per buffer). */
	static BufferVertexConsumer into(BufferBuilder buffer) {
		return BufferVertexConsumer.into(buffer);
	}

	/** The consumer writing positions as they are into a buffer 1.12.2 began and draws. */
	static BufferVertexConsumer raw(BufferBuilder buffer) {
		return BufferVertexConsumer.raw(buffer);
	}

	/** The render type the vertices are drawn with (a mod's own consumer: a plain one). */
	default RenderType type() {
		return RenderType.solid();
	}

	/** 1.21 {@code addVertex(pose, x, y, z)}: the pose is the GL matrix here (PoseStack), already applied. */
	default VertexConsumer addVertex(Object pose, float x, float y, float z) {
		return this.addVertex(x, y, z);
	}

	default VertexConsumer addVertex(org.joml.Vector3f position) {
		return this.addVertex(position.x(), position.y(), position.z());
	}

	default VertexConsumer addVertex(Object pose, org.joml.Vector3f position) {
		return this.addVertex(pose, position.x(), position.y(), position.z());
	}

	default VertexConsumer setColor(int argb) {
		return this.setColor(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24);
	}

	default VertexConsumer setColor(float red, float green, float blue, float alpha) {
		return this.setColor((int) (red * 255), (int) (green * 255), (int) (blue * 255), (int) (alpha * 255));
	}

	default VertexConsumer setWhiteAlpha(int alpha) {
		return this.setColor(255, 255, 255, alpha);
	}

	/** The packed light (block light low, sky light high), as 1.21's: the second UV. */
	default VertexConsumer setLight(int light) {
		return this.setUv2(light & 0xFFFF, light >> 16 & 0xFFFF);
	}

	default VertexConsumer setOverlay(int overlay) {
		return this.setUv1(overlay & 0xFFFF, overlay >> 16 & 0xFFFF);
	}

	default VertexConsumer setNormal(Object pose, float x, float y, float z) {
		return this.setNormal(x, y, z);
	}

	/** The Translator's own name of setUv (its Tesselation bridge). */
	default VertexConsumer tex(double u, double v) {
		return this.setUv((float) u, (float) v);
	}

	/** 1.21 {@code addVertex(x, y, z, color, u, v, overlay, light, nx, ny, nz)}: a whole vertex at once. */
	default void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
		this.addVertex(x, y, z).setColor(color).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(normalX, normalY, normalZ);
	}

	/** The four vertices of a baked quad (position and texture from its 1.12.2 vertex data), tinted and lit. */
	default void putBulkData(Object pose, net.minecraft.client.renderer.block.model.BakedQuad quad, float red, float green, float blue, float alpha,
			int light, int overlay) {
		int[] data = quad.getVertexData();
		var format = quad.getFormat();
		int stride = format.getIntegerSize();
		int uv = format.hasUvOffset(0) ? format.getUvOffsetById(0) / 4 : -1;
		for (int i = 0; i < 4 && (i + 1) * stride <= data.length; i++) {
			int base = i * stride;
			this.addVertex(Float.intBitsToFloat(data[base]), Float.intBitsToFloat(data[base + 1]), Float.intBitsToFloat(data[base + 2]))
					.setColor(red, green, blue, alpha);
			if (uv >= 0) {
				this.setUv(Float.intBitsToFloat(data[base + uv]), Float.intBitsToFloat(data[base + uv + 1]));
			}
			this.setOverlay(overlay).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
		}
	}

	default void putBulkData(Object pose, net.minecraft.client.renderer.block.model.BakedQuad quad, float red, float green, float blue, float alpha,
			int light, int overlay, boolean readAlpha) {
		this.putBulkData(pose, quad, red, green, blue, alpha, light, overlay);
	}

	default void putBulkData(Object pose, net.minecraft.client.renderer.block.model.BakedQuad quad, float[] brightness, float red, float green, float blue,
			float alpha, int[] lights, int overlay, boolean readAlpha) {
		this.putBulkData(pose, quad, red, green, blue, alpha, lights.length > 0 ? lights[0] : 0xF000F0, overlay);
	}

	/** NeoForge {@code misc(element, values)}: an element of a format the vertex has no place for here. */
	default VertexConsumer misc(net.minecraft.client.renderer.vertex.VertexFormatElement element, int... values) {
		return this;
	}

	/** NeoForge {@code applyBakedLighting(light, data)} / {@code applyBakedNormals}: left to the caller's values. */
	default int applyBakedLighting(int light, java.nio.ByteBuffer data) {
		return light;
	}

	default void applyBakedNormals(org.joml.Vector3f generated, java.nio.ByteBuffer data, org.joml.Matrix3f normalTransform) {
	}
}
