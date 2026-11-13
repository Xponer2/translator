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

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/**
 * The Translator's banner at the bottom of Forge's loading screen (core.SplashBanner calls {@link #draw()} in the
 * splash thread before each frame is shown). Drawn in the splash's own space (640 x 480 around the centre, one unit a
 * pixel, y down). Any failure turns it off: the loading screen is never broken by it.
 */
public final class TranslatorSplash {
	private static int texture = -1;
	private static boolean off;
	private static boolean logged;

	private TranslatorSplash() {
	}

	public static void draw() {
		if (off) {
			return;
		}
		try {
			if (texture < 0) {
				texture = load();
			}
			// its own projection in window pixels: the splash leaves its matrices as its last drawing needed them
			int w = Display.getWidth();
			int h = Display.getHeight();
			// the splash lays its things out in 640 x 480 around the middle: under that when there is room, else small in
			// the bottom left corner beside its 400 wide bars (Forge's anvil is in the other one)
			float free = (h - 480) / 2f;
			float width;
			float height;
			float x0;
			if (free >= 60) {
				height = Math.min(free - 16, 120);
				width = height * 720 / 150;
				x0 = (w - width) / 2;
			} else {
				width = Math.max(120, Math.min(300, (w - 400) / 2f - 16));
				height = width * 150 / 720;
				x0 = 10;
			}
			float y1 = h - 10;
			float y0 = y1 - height;
			if (!logged) {
				logged = true;
				org.apache.logging.log4j.LogManager.getLogger("Translator").info("Loading screen banner at x {}..{}, y {}..{} of {}x{} (texture {})",
						x0, x0 + width, y0, y1, w, h, texture);
			}
			GL11.glViewport(0, 0, w, h);
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glPushMatrix();
			GL11.glLoadIdentity();
			GL11.glOrtho(0, w, h, 0, -1, 1);
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			GL11.glPushMatrix();
			GL11.glLoadIdentity();
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glDisable(GL11.GL_DEPTH_TEST);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glColor4f(1, 1, 1, 1);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glTexCoord2f(0, 0);
			GL11.glVertex2f(x0, y0);
			GL11.glTexCoord2f(0, 1);
			GL11.glVertex2f(x0, y1);
			GL11.glTexCoord2f(1, 1);
			GL11.glVertex2f(x0 + width, y1);
			GL11.glTexCoord2f(1, 0);
			GL11.glVertex2f(x0 + width, y0);
			GL11.glEnd();
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glPopMatrix();
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			GL11.glPopMatrix();
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
			GL11.glDisable(GL11.GL_TEXTURE_2D);
		} catch (Throwable t) {
			off = true;
			org.apache.logging.log4j.LogManager.getLogger("Translator").warn("The loading screen banner is off", t);
		}
	}

	private static int load() throws java.io.IOException {
		BufferedImage image;
		try (var in = TranslatorSplash.class.getResourceAsStream("/assets/translator/textures/gui/splash_banner.png")) {
			if (in == null) {
				throw new java.io.IOException("no banner");
			}
			image = javax.imageio.ImageIO.read(in);
		}
		int w = image.getWidth();
		int h = image.getHeight();
		int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);
		ByteBuffer buffer = BufferUtils.createByteBuffer(w * h * 4);
		for (int argb : pixels) {
			buffer.put((byte) (argb >> 16)).put((byte) (argb >> 8)).put((byte) argb).put((byte) (argb >>> 24));
		}
		buffer.flip();
		int id = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		return id;
	}
}
