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

import java.nio.ByteBuffer;

import javax.annotation.Nullable;

/**
 * OpenGL calls of LWJGL 3 (Minecraft 1.21) that LWJGL 2 (Minecraft 1.12.2) makes with other parameters, and the GL
 * program of a 1.21 shader, which 1.12.2 doesn't have (it draws with the fixed pipeline).
 */
public final class GlBridges {
	private GlBridges() {
	}

	/**
	 * LWJGL 3 {@code GL15.glMapBuffer(target, access)}: the buffer's memory, sized by the buffer (LWJGL 2 takes the
	 * buffer to reuse, none here). Xaero's World Map uploads and reads its region textures through it: without it no
	 * region texture got its pixels and the map showed whatever the video memory held (other worlds' chunks).
	 */
	@Nullable
	public static ByteBuffer glMapBuffer(int target, int access) {
		return org.lwjgl.opengl.GL15.glMapBuffer(target, access, null);
	}

	/**
	 * 1.21 {@code shader.getId()}: the GL program of a shader. 1.12.2 draws the mods' render types with the fixed pipeline,
	 * so it is program 0 (no program), which is what a later {@code glUseProgram} of it restores (Xaero's map textures).
	 */
	public static int getId(@Nullable Object shader) {
		return 0;
	}
}
