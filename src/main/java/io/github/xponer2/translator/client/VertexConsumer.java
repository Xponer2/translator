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

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/**
 * 1.21 vertex consumer as immediate quads on the 1.12.2 Tessellator (blocks atlas, position + uv + color). Each vertex
 * is taken with the GL matrix current when it was added (the PoseStack bridge is the GL matrix stack), so transforms
 * between vertices work like in 1.21. Drawn by {@link #flush()} (the renderer adapters flush after each render).
 */
public final class VertexConsumer {
	private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);

	private final RenderType type;
	private final Matrix4f begin = new Matrix4f();
	private final Matrix4f beginInverse = new Matrix4f();
	private final FloatBuffer beginBuffer = BufferUtils.createFloatBuffer(16);
	private boolean started;
	private boolean pending;
	private float x, y, z, u, v;
	private int red = 255, green = 255, blue = 255, alpha = 255;

	VertexConsumer(RenderType type) {
		this.type = type;
	}

	public RenderType type() {
		return this.type;
	}

	public VertexConsumer addVertex(Object pose, float x, float y, float z) {
		return this.addVertex(x, y, z);
	}

	public VertexConsumer addVertex(float x, float y, float z) {
		this.emit();
		if (!this.started) {
			this.start();
		}

		// position in the matrix of the batch start: begin^-1 * current * vertex
		var current = currentMatrix();
		var relative = Matrix4f.mul(this.beginInverse, current, null);
		var position = Matrix4f.transform(relative, new Vector4f(x, y, z, 1.0F), null);
		this.x = position.x;
		this.y = position.y;
		this.z = position.z;
		this.pending = true;
		return this;
	}

	public VertexConsumer setColor(int argb) {
		return this.setColor(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24);
	}

	public VertexConsumer setColor(int red, int green, int blue, int alpha) {
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.alpha = alpha;
		return this;
	}

	public VertexConsumer setColor(float red, float green, float blue, float alpha) {
		return this.setColor((int) (red * 255), (int) (green * 255), (int) (blue * 255), (int) (alpha * 255));
	}

	public VertexConsumer setUv(float u, float v) {
		this.u = u;
		this.v = v;
		return this;
	}

	public VertexConsumer tex(double u, double v) {
		return this.setUv((float) u, (float) v);
	}

	public VertexConsumer setLight(int light) {
		return this;
	}

	public VertexConsumer setOverlay(int overlay) {
		return this;
	}

	public VertexConsumer setNormal(float x, float y, float z) {
		return this;
	}

	public VertexConsumer setNormal(Object pose, float x, float y, float z) {
		return this;
	}

	/** Draws the vertices added so far. */
	public void flush() {
		this.emit();
		if (!this.started) {
			return;
		}

		this.started = false;
		GlStateManager.pushMatrix();
		GL11.glLoadMatrix(this.beginBuffer);
		Tessellator.getInstance().draw();
		GlStateManager.popMatrix();
		this.type.clearRenderState();
	}

	private void start() {
		this.started = true;
		this.type.setupRenderState();
		Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		this.begin.load(currentMatrix());
		Matrix4f.invert(this.begin, this.beginInverse);
		this.beginBuffer.clear();
		this.begin.store(this.beginBuffer);
		this.beginBuffer.flip();
		Tessellator.getInstance().getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
	}

	private void emit() {
		if (!this.pending) {
			return;
		}

		this.pending = false;
		Tessellator.getInstance().getBuffer().pos(this.x, this.y, this.z).tex(this.u, this.v)
				.color(this.red, this.green, this.blue, this.alpha).endVertex();
	}

	private static Matrix4f currentMatrix() {
		MATRIX.clear();
		GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX);
		var matrix = new Matrix4f();
		matrix.load(MATRIX);
		return matrix;
	}
}
