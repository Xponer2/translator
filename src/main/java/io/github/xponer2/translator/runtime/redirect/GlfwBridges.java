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
package io.github.xponer2.translator.runtime.redirect;

import io.github.xponer2.translator.runtime.neoforge.KeyMapping;

/**
 * The GLFW calls of the 1.21.1 mods (LWJGL 3) on 1.12.2's LWJGL 2 input (neoforge-1.21.1-redirects.tsv): key and mouse
 * button state, the time, the cursor. Key codes are GLFW's, turned into LWJGL 2's; the window handle is ignored (there
 * is one window).
 */
public final class GlfwBridges {
	private static final int PRESS = 1;
	private static final int RELEASE = 0;

	private GlfwBridges() {
	}

	public static int glfwGetKey(long window, int key) {
		int code = KeyMapping.toLwjgl(key);
		return code > 0 && org.lwjgl.input.Keyboard.isCreated() && org.lwjgl.input.Keyboard.isKeyDown(code) ? PRESS : RELEASE;
	}

	public static int glfwGetMouseButton(long window, int button) {
		return org.lwjgl.input.Mouse.isCreated() && org.lwjgl.input.Mouse.isButtonDown(button) ? PRESS : RELEASE;
	}

	public static double glfwGetTime() {
		return System.nanoTime() / 1.0E9;
	}

	/** The cursor mode (hidden, captured): 1.12.2 grabs and frees the mouse itself. */
	public static void glfwSetInputMode(long window, int mode, int value) {
	}

	/** In window coordinates from the top, as GLFW counts; LWJGL 2 counts from the bottom. */
	public static void glfwSetCursorPos(long window, double x, double y) {
		if (org.lwjgl.input.Mouse.isCreated()) {
			org.lwjgl.input.Mouse.setCursorPosition((int) x, org.lwjgl.opengl.Display.getHeight() - 1 - (int) y);
		}
	}
}
