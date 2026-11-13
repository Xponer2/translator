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
	private static final float WIDTH = 360;
	private static final float HEIGHT = 75;
	private static int texture = -1;
	private static boolean off;

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
			float x0 = 320 - WIDTH / 2;
			float y1 = 240 + Display.getHeight() / 2f - 14;
			float y0 = y1 - HEIGHT;
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
			GL11.glVertex2f(x0 + WIDTH, y1);
			GL11.glTexCoord2f(1, 0);
			GL11.glVertex2f(x0 + WIDTH, y0);
			GL11.glEnd();
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
