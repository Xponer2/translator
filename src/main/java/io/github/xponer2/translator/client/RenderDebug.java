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

import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

/**
 * With {@code -Dtranslator.debug.render=true}: what the 1.21 render types of the mods draw, every five seconds (the
 * batches, their vertices, the GL state at the draw). A mod's drawing that shows nothing (JourneyMap's map tiles) is
 * found by it without guessing.
 */
final class RenderDebug {
	static final boolean ENABLED = Boolean.getBoolean("translator.debug.render");
	private static final Logger LOGGER = LogManager.getLogger("Translator Render Debug");
	private static final Map<String, long[]> DRAWN = new TreeMap<>();
	private static final Map<String, String> STATE = new TreeMap<>();
	private static long last = System.currentTimeMillis();

	private RenderDebug() {
	}

	/** After a batch is drawn: a GL error it caused, once per type and error. */
	static void afterDraw(String type) {
		int error = GL11.glGetError();
		if (error != GL11.GL_NO_ERROR && GL_ERRORS.add(type + error)) {
			LOGGER.warn("{}: GL error 0x{} after its draw", type, Integer.toHexString(error));
		}
	}

	private static final java.util.Set<Integer> SAMPLED = new java.util.HashSet<>();
	private static final java.util.Set<String> SAMPLED_TYPES = new java.util.HashSet<>();
	private static final java.util.Set<String> GL_ERRORS = new java.util.HashSet<>();

	static void drawn(String type, int vertices, net.minecraft.client.renderer.BufferBuilder buffer) {
		int bound = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		if (SAMPLED_TYPES.add(type) && SAMPLED.size() < 400 && SAMPLED.add(bound)) {
			// what the bound texture holds, and the batch's first vertex (position, uv, color)
			int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
			int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
			int opaque = -1;
			if (width > 0 && height > 0 && width * height <= 4096 * 4096) {
				java.nio.ByteBuffer pixels = org.lwjgl.BufferUtils.createByteBuffer(width * height * 4);
				GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
				opaque = 0;
				for (int i = 3; i < width * height * 4; i += 4) {
					if (pixels.get(i) != 0) {
						opaque++;
					}
				}
			}
			String vertex = "";
			java.nio.ByteBuffer data = buffer.getByteBuffer();
			if (vertices > 0 && data.limit() >= 24) {
				vertex = String.format(" first vertex %.1f %.1f %.1f uv %.3f %.3f rgba %d %d %d %d", data.getFloat(0), data.getFloat(4),
						data.getFloat(8), data.getFloat(12), data.getFloat(16), data.get(20) & 255, data.get(21) & 255, data.get(22) & 255,
						data.get(23) & 255);
			}
			LOGGER.info("{}: texture {} is {}x{}, {} pixels not transparent;{}", type, bound, width, height, opaque, vertex);
		}
		long[] counts = DRAWN.computeIfAbsent(type, k -> new long[2]);
		counts[0]++;
		counts[1] += vertices;
		long now = System.currentTimeMillis();
		if (now - last >= 5000) {
			last = now;
			DRAWN.forEach((name, c) -> LOGGER.info("{}: {} batches, {} vertices; {}", name, c[0], c[1], STATE.get(name)));
			DRAWN.clear();
			STATE.clear();
		}
		if (STATE.containsKey(type)) {
			// the GL state once a type per report: each query stalls the pipeline (the game lagged with it every batch)
			return;
		}
		java.nio.FloatBuffer modelView = org.lwjgl.BufferUtils.createFloatBuffer(16);
		GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
		java.nio.ByteBuffer colorMask = org.lwjgl.BufferUtils.createByteBuffer(16);
		GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK, colorMask);
		java.nio.ByteBuffer depthMask = org.lwjgl.BufferUtils.createByteBuffer(16);
		GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK, depthMask);
		STATE.put(type, "depth func 0x" + Integer.toHexString(GL11.glGetInteger(GL11.GL_DEPTH_FUNC)) + ", depth write " + depthMask.get(0)
				+ ", color write " + colorMask.get(0) + colorMask.get(1) + colorMask.get(2) + colorMask.get(3) + ", alpha func 0x"
				+ Integer.toHexString(GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC)) + " " + GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF) + ", texture " + GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D) + (GL11.glIsEnabled(GL11.GL_TEXTURE_2D) ? "" : " (off)")
				+ ", depth " + (GL11.glIsEnabled(GL11.GL_DEPTH_TEST) ? "on" : "off") + ", blend " + (GL11.glIsEnabled(GL11.GL_BLEND) ? "on" : "off")
				+ ", alpha test " + (GL11.glIsEnabled(GL11.GL_ALPHA_TEST) ? "on" : "off") + ", cull " + (GL11.glIsEnabled(GL11.GL_CULL_FACE) ? "on" : "off")
				+ ", model z " + modelView.get(14));
	}
}
