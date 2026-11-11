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

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import org.lwjgl.BufferUtils;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Static stand-ins for the 1.21 {@code VertexBuffer} (a mesh uploaded once and drawn every frame: the Aether's and
 * Twilight Forest's skies, AE2's and GuideME's scenes, Ars Nouveau) on 1.12.2's VertexBuffer
 * (neoforge-1.21.1-redirects.tsv). The mesh is kept as a copy of its vertices and drawn from it with client arrays, in
 * the current GL matrix (the pose stack is the GL matrix here) and state; the shader is 1.12.2's fixed pipeline.
 */
@SideOnly(Side.CLIENT)
public final class VertexBuffers {
	private static final Map<VertexBuffer, Mesh> MESHES = Collections.synchronizedMap(new WeakHashMap<>());

	private VertexBuffers() {
	}

	private static final class Mesh {
		final ByteBuffer data;
		final VertexFormat format;
		final int mode;
		final int count;

		Mesh(ByteBuffer data, VertexFormat format, int mode, int count) {
			this.data = data;
			this.format = format;
			this.mode = mode;
			this.count = count;
		}
	}

	/** 1.21 {@code new VertexBuffer(usage)}: the usage hint means nothing for a client array mesh. */
	public static VertexBuffer newVertexBuffer(@Nullable Object usage) {
		return new VertexBuffer(DefaultVertexFormats.POSITION);
	}

	/** 1.21 {@code vertexBuffer.upload(mesh)} (the tables name it bufferData): the mesh's vertices kept. */
	public static void bufferData(VertexBuffer buffer, @Nullable MeshData mesh) {
		if (mesh == null) {
			MESHES.remove(buffer);
			return;
		}
		BufferBuilder built = mesh.finishForUpload();
		if (built == null) {
			return;
		}
		int count = built.getVertexCount();
		VertexFormat format = built.getVertexFormat();
		ByteBuffer source = built.getByteBuffer();
		int bytes = count * format.getSize();
		ByteBuffer copy = BufferUtils.createByteBuffer(bytes);
		ByteBuffer view = source.duplicate();
		view.position(0);
		view.limit(Math.min(bytes, view.capacity()));
		copy.put(view);
		copy.flip();
		MESHES.put(buffer, new Mesh(copy, format, built.getDrawMode(), count));
		built.reset();
	}

	/** 1.21 {@code vertexBuffer.upload(mesh)} under its own name. */
	public static void upload(VertexBuffer buffer, @Nullable MeshData mesh) {
		bufferData(buffer, mesh);
	}

	/** 1.21 {@code uploadIndexBuffer(result)}: a sorted index order (translucent meshes); the vertices are drawn in theirs. */
	public static void uploadIndexBuffer(VertexBuffer buffer, @Nullable Object indices) {
	}

	/** 1.21 {@code isInvalid()}: whether the buffer was closed. */
	public static boolean isInvalid(VertexBuffer buffer) {
		return false;
	}

	/** 1.21 {@code drawWithShader(modelView, projection, shader)}: drawn in the GL matrices and state of now. */
	public static void drawWithShader(VertexBuffer buffer, @Nullable Object modelView, @Nullable Object projection, @Nullable Object shader) {
		draw(buffer);
	}

	/** 1.21 {@code vertexBuffer.draw()}. */
	public static void draw(VertexBuffer buffer) {
		Mesh mesh = MESHES.get(buffer);
		if (mesh == null || mesh.count == 0) {
			return;
		}
		MultiBufferSource.INSTANCE.endBatch();
		// the pointers are into the client array: no buffer object bound
		OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
		boolean textured = false;
		List<VertexFormatElement> elements = mesh.format.getElements();
		int stride = mesh.format.getSize();
		for (int i = 0; i < elements.size(); i++) {
			textured |= elements.get(i).getUsage() == VertexFormatElement.EnumUsage.UV && elements.get(i).getIndex() == 0;
			elements.get(i).getUsage().preDraw(mesh.format, i, stride, mesh.data);
		}
		if (!textured) {
			GlStateManager.disableTexture2D();
		}
		GlStateManager.glDrawArrays(mesh.mode, 0, mesh.count);
		if (!textured) {
			GlStateManager.enableTexture2D();
		}
		for (int i = 0; i < elements.size(); i++) {
			elements.get(i).getUsage().postDraw(mesh.format, i, stride, mesh.data);
		}
	}

	/** 1.21 {@code vertexBuffer.close()}: the mesh and the 1.12.2 buffer freed. */
	public static void close(VertexBuffer buffer) {
		MESHES.remove(buffer);
		buffer.deleteGlBuffers();
	}

	/** 1.21 {@code vertexBuffer.getFormat()}: the uploaded mesh's format. */
	public static VertexFormat getFormat(VertexBuffer buffer) {
		Mesh mesh = MESHES.get(buffer);
		return mesh != null ? mesh.format : DefaultVertexFormats.POSITION;
	}

	/** 1.21 {@code VertexBuffer.unbind()} (static): nothing stays bound here. */
	public static void unbind() {
		OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
	}

	/** 1.21 {@code bind()}: nothing to bind for a client array mesh. */
	public static void bind(VertexBuffer buffer) {
	}

	/** The static 1.21 unbind() under 1.12.2's name (the tables rename it). */
	public static void unbindBuffer() {
		unbind();
	}
}
