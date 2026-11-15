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
package io.github.xponer2.translator.selftest;

import java.nio.FloatBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The end of the automatic self-test: half a minute in the world as a player sees it, the debug screen off (the test
 * runs with it on, and JourneyMap hides its minimap under it: the lag of the render types it piled up never showed).
 * Logged for the regression checks (tools/regress.py, tools/progress/bugs.md): the frames a second with every HUD
 * drawn, the render type consumers kept, and the HUD matrix moving before the chat is drawn (the chat out of the
 * screen).
 */
@SideOnly(Side.CLIENT)
final class PlayCheck {
	private static final Logger LOGGER = LogManager.getLogger("Translator Self-Test");
	private static final int TICKS = 600;
	private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);

	private final Minecraft mc;
	private final boolean debug;
	private long fpsSum;
	private int samples;
	private int lowest = Integer.MAX_VALUE;
	private float baseX;
	private float baseY;
	private boolean moved;

	PlayCheck(Minecraft mc) {
		this.mc = mc;
		this.debug = mc.gameSettings.showDebugInfo;
		mc.gameSettings.showDebugInfo = false;
		if (mc.currentScreen != null) {
			mc.displayGuiScreen(null);
		}
		MinecraftForge.EVENT_BUS.register(this);
		LOGGER.info("Play check: 30 s in the world without the debug screen");
	}

	/** One client tick; true when done. */
	boolean tick(int ticks) {
		if (ticks > 40 && ticks % 20 == 0) {
			// the first two seconds left out: the debug screen just went away
			int fps = Minecraft.getDebugFPS();
			this.fpsSum += fps;
			this.samples++;
			this.lowest = Math.min(this.lowest, fps);
		}
		if (ticks < TICKS) {
			return false;
		}
		MinecraftForge.EVENT_BUS.unregister(this);
		this.mc.gameSettings.showDebugInfo = this.debug;
		LOGGER.info("Play check: {} fps on average, {} at the lowest; render buffers kept: {}; chat {}", this.samples == 0 ? 0 : this.fpsSum / this.samples,
				this.samples == 0 ? 0 : this.lowest, io.github.xponer2.translator.client.MultiBufferSource.kept(),
				this.moved ? "drawn off its place" : "in its place");
		return true;
	}

	/** The HUD's matrix when the HUD starts and when the chat is about to be drawn: they must be the same. */
	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
	public void onHud(RenderGameOverlayEvent.Pre event) {
		if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
			MATRIX.clear();
			GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX);
			this.baseX = MATRIX.get(12);
			this.baseY = MATRIX.get(13);
		} else if (event.getType() == RenderGameOverlayEvent.ElementType.CHAT && !this.moved) {
			MATRIX.clear();
			GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX);
			float dx = MATRIX.get(12) - this.baseX;
			float dy = MATRIX.get(13) - this.baseY;
			if (Math.abs(dx) > 0.5F || Math.abs(dy) > 0.5F) {
				this.moved = true;
				LOGGER.warn("HUD matrix moved before the chat: {} {} (the chat is drawn off its place)", dx, dy);
			}
		}
	}
}
