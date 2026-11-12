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

import org.apache.logging.log4j.LogManager;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Called at the start of 1.12.2's BufferBuilder.begin (core.BufferBuilderBegin): a batch a failed render left open ends. */
@SideOnly(Side.CLIENT)
public final class TessellatorGuard {
	private static boolean logged;

	private TessellatorGuard() {
	}

	public static void beforeBegin(Object builder) {
		if (!(builder instanceof BufferBuilder buffer) || !BufferVertexConsumer.drawing(buffer)) {
			return;
		}
		if (!logged) {
			logged = true;
			LogManager.getLogger("Translator Runtime").warn("A render left the Tessellator in a batch (a 1.21.1 mod's render that failed half-way):"
					+ " its vertices were dropped so the game draws on", new IllegalStateException("Already building"));
		}
		try {
			buffer.finishDrawing();
			buffer.reset();
		} catch (IllegalStateException e) {
			// not in a batch after all (the drawing flag couldn't be read): begin goes on as 1.12.2's
		}
	}
}
