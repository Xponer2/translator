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

import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;

/**
 * The 1.21 {@code DefaultVertexFormat} formats 1.12.2's DefaultVertexFormats lacks, as the nearest 1.12.2 one (the
 * Translator's immediate drawing writes position, texture and color anyway). The carried class's initializer built
 * them from 1.21 elements 1.12.2 hasn't and failed: JourneyMap's render types read them.
 */
public final class VertexFormats {
	public static final VertexFormat POSITION_COLOR_TEX_LIGHTMAP = DefaultVertexFormats.POSITION_TEX_LMAP_COLOR;
	public static final VertexFormat POSITION_COLOR_LIGHTMAP = DefaultVertexFormats.POSITION_COLOR;
	public static final VertexFormat POSITION_COLOR_NORMAL = DefaultVertexFormats.POSITION_COLOR;
	public static final VertexFormat POSITION_TEX_COLOR_NORMAL = DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL;
	public static final VertexFormat BLIT_SCREEN = DefaultVertexFormats.POSITION_TEX_COLOR;
	public static final VertexFormat EMPTY = new VertexFormat();

	private VertexFormats() {
	}
}
