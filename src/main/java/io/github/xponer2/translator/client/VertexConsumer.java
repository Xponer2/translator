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
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormatElement;

/**
 * 1.21 vertex consumer as immediate quads on the 1.12.2 Tessellator (blocks atlas, position + uv + color). Each vertex
 * is taken with the GL matrix current when it was added (the PoseStack bridge is the GL matrix stack), so transforms
 * between vertices work like in 1.21. Drawn by {@link #flush()} (the renderer adapters flush after each render).
 * <p>
 * The consumer of a buffer a mod began itself ({@code Tesselator.begin(mode, format)}, see {@link Tesselation}) writes
 * each vertex into that buffer in its format's order, and is drawn by {@code BufferUploader.drawWithShader}.
 */
public final class VertexConsumer {
	private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
	private static final Map<BufferBuilder, VertexConsumer> TARGETS = new WeakHashMap<>();

	@Nullable
	private final RenderType type;
	@Nullable
	private final BufferBuilder target;
	private final Matrix4f begin = new Matrix4f();
	private final Matrix4f beginInverse = new Matrix4f();
	private final FloatBuffer beginBuffer = BufferUtils.createFloatBuffer(16);
	private boolean started;
	private boolean pending;
	private float x, y, z, u, v;
	private float normalX, normalY = 1.0F, normalZ;
	private int red = 255, green = 255, blue = 255, alpha = 255;
	private int lightU = 240, lightV = 240;

	VertexConsumer(RenderType type) {
		this.type = type;
		this.target = null;
	}

	private VertexConsumer(BufferBuilder target) {
		this.type = null;
		this.target = target;
	}

	/** The consumer writing into a buffer the mod began (one per buffer). */
	public static VertexConsumer into(BufferBuilder buffer) {
		synchronized (TARGETS) {
			return TARGETS.computeIfAbsent(buffer, VertexConsumer::new);
		}
	}

	public RenderType type() {
		return this.type != null ? this.type : RenderType.solid();
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

	public VertexConsumer setWhiteAlpha(int alpha) {
		return this.setColor(255, 255, 255, alpha);
	}

	public VertexConsumer setUv(float u, float v) {
		this.u = u;
		this.v = v;
		return this;
	}

	public VertexConsumer tex(double u, double v) {
		return this.setUv((float) u, (float) v);
	}

	/** The overlay (hurt flash) texture coordinates: 1.12.2 has no overlay. */
	public VertexConsumer setUv1(int u, int v) {
		return this;
	}

	/** The lightmap coordinates (block light, sky light). */
	public VertexConsumer setUv2(int u, int v) {
		this.lightU = u;
		this.lightV = v;
		return this;
	}

	public VertexConsumer setLight(int light) {
		return this.setUv2(light & 0xFFFF, light >> 16 & 0xFFFF);
	}

	public VertexConsumer setOverlay(int overlay) {
		return this;
	}

	public VertexConsumer setNormal(float x, float y, float z) {
		this.normalX = x;
		this.normalY = y;
		this.normalZ = z;
		return this;
	}

	public VertexConsumer setNormal(Object pose, float x, float y, float z) {
		return this.setNormal(x, y, z);
	}

	/** 1.21 {@code addVertex(x, y, z, color, u, v, overlay, light, nx, ny, nz)}: a whole vertex at once. */
	public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
		this.addVertex(x, y, z).setColor(color).setUv(u, v).setLight(light).setNormal(normalX, normalY, normalZ);
	}

	/** The four vertices of a baked quad (position and texture from its 1.12.2 vertex data), tinted and lit. */
	public void putBulkData(Object pose, net.minecraft.client.renderer.block.model.BakedQuad quad, float red, float green, float blue, float alpha,
			int light, int overlay) {
		int[] data = quad.getVertexData();
		var format = quad.getFormat();
		int stride = format.getIntegerSize();
		int uv = format.hasUvOffset(0) ? format.getUvOffsetById(0) / 4 : -1;
		for (int i = 0; i < 4 && (i + 1) * stride <= data.length; i++) {
			int base = i * stride;
			this.addVertex(Float.intBitsToFloat(data[base]), Float.intBitsToFloat(data[base + 1]), Float.intBitsToFloat(data[base + 2]))
					.setColor(red, green, blue, alpha).setLight(light);
			if (uv >= 0) {
				this.setUv(Float.intBitsToFloat(data[base + uv]), Float.intBitsToFloat(data[base + uv + 1]));
			}
		}
	}

	public void putBulkData(Object pose, net.minecraft.client.renderer.block.model.BakedQuad quad, float red, float green, float blue, float alpha,
			int light, int overlay, boolean readAlpha) {
		this.putBulkData(pose, quad, red, green, blue, alpha, light, overlay);
	}

	public void putBulkData(Object pose, net.minecraft.client.renderer.block.model.BakedQuad quad, float[] brightness, float red, float green, float blue,
			float alpha, int[] lights, int overlay, boolean readAlpha) {
		this.putBulkData(pose, quad, red, green, blue, alpha, lights.length > 0 ? lights[0] : 0xF000F0, overlay);
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

	/** The last vertex of a mod's buffer written, its vertices drawn in the matrix they were added in. */
	void drawTarget() {
		this.emit();
		boolean added = this.started;
		this.started = false;
		if (added) {
			GlStateManager.pushMatrix();
			GL11.glLoadMatrix(this.beginBuffer);
		}
		if (this.target == Tessellator.getInstance().getBuffer()) {
			Tessellator.getInstance().draw();
		} else {
			this.target.finishDrawing();
			new net.minecraft.client.renderer.WorldVertexBufferUploader().draw(this.target);
		}
		if (added) {
			GlStateManager.popMatrix();
		}
	}

	/** The last vertex written, nothing drawn (a buffer the mod draws or discards itself). */
	void finishTarget() {
		this.emit();
		this.started = false;
	}

	private void start() {
		this.started = true;
		if (this.target == null) {
			this.type.setupRenderState();
			Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		}
		this.begin.load(currentMatrix());
		Matrix4f.invert(this.begin, this.beginInverse);
		this.beginBuffer.clear();
		this.begin.store(this.beginBuffer);
		this.beginBuffer.flip();
		if (this.target == null) {
			Tessellator.getInstance().getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
		}
	}

	private void emit() {
		if (!this.pending) {
			return;
		}

		this.pending = false;
		if (this.target == null) {
			Tessellator.getInstance().getBuffer().pos(this.x, this.y, this.z).tex(this.u, this.v)
					.color(this.red, this.green, this.blue, this.alpha).endVertex();
			return;
		}
		// the elements of the buffer's format in order (padding is skipped by the buffer)
		for (VertexFormatElement element : this.target.getVertexFormat().getElements()) {
			switch (element.getUsage()) {
			case POSITION:
				this.target.pos(this.x, this.y, this.z);
				break;
			case UV:
				if (element.getIndex() == 0) {
					this.target.tex(this.u, this.v);
				} else {
					this.target.lightmap(this.lightV, this.lightU);
				}
				break;
			case COLOR:
				this.target.color(this.red, this.green, this.blue, this.alpha);
				break;
			case NORMAL:
				this.target.normal(this.normalX, this.normalY, this.normalZ);
				break;
			default:
				break;
			}
		}
		this.target.endVertex();
	}

	private static Matrix4f currentMatrix() {
		MATRIX.clear();
		GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX);
		var matrix = new Matrix4f();
		matrix.load(MATRIX);
		return matrix;
	}
}
