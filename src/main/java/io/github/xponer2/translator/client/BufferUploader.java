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

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Draws built vertices (1.21 {@code BufferUploader}, neoforge-1.21.1-runtime.tsv): with the GL state the mod set up
 * through {@link RenderSystem} (1.12.2 has no shaders, the fixed pipeline draws them).
 */
@SideOnly(Side.CLIENT)
public final class BufferUploader {
	private BufferUploader() {
	}

	public static void drawWithShader(@Nullable MeshData mesh) {
		if (mesh != null) {
			mesh.draw();
		}
	}

	public static void draw(@Nullable MeshData mesh) {
		drawWithShader(mesh);
	}

	public static void reset() {
	}

	public static void invalidate() {
	}
}
