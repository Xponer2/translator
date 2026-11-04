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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.client.GuiModList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * After the automatic self-test: the screens where mods without blocks and items show what they do, each saved as a
 * picture in {@code translator/selftest/screens/} (Chat Heads' heads in the chat, JEI's item list beside the
 * inventory, Controlling's search in the controls, the mods list, the world with a block looked at: Jade's tooltip,
 * AppleSkin's food bar, the minimaps). What a mod does there can only be seen; the pictures are looked at after the
 * run. A step that throws is logged and the tour goes on.
 */
@SideOnly(Side.CLIENT)
final class ScreenTour {
	private static final Logger LOGGER = LogManager.getLogger("Translator AutoTest");
	/** Ticks a screen is shown before its picture: long enough for it to draw (and the chat to show the line). */
	private static final int WAIT = 40;

	private final Minecraft mc;
	private final List<Step> steps = new ArrayList<>();
	private final File folder;
	private int index;
	private int ticks;

	ScreenTour(Minecraft mc) {
		this.mc = mc;
		this.folder = new File(mc.gameDir, "translator/selftest/screens");
		this.folder.mkdirs();
		this.steps.add(new Step("world", () -> {
			mc.displayGuiScreen(null);
			// a block to look at, in front of the player at eye height (Jade, The One Probe)
			var server = mc.getIntegratedServer();
			if (server != null && mc.player != null) {
				BlockPos front = new BlockPos(mc.player.getPositionEyes(1.0F).add(mc.player.getLookVec().scale(2.0)));
				server.addScheduledTask(() -> server.getWorld(mc.player.dimension).setBlockState(front, Blocks.CRAFTING_TABLE.getDefaultState()));
			}
		}));
		this.steps.add(new Step("chat", () -> {
			mc.displayGuiScreen(null);
			if (mc.player != null) {
				mc.player.sendChatMessage("Translator self-test: a chat line");
			}
		}));
		this.steps.add(new Step("inventory", () -> mc.displayGuiScreen(new GuiInventory(mc.player))));
		this.steps.add(new Step("controls", () -> mc.displayGuiScreen(new GuiControls(new GuiOptions(null, mc.gameSettings), mc.gameSettings))));
		this.steps.add(new Step("mods", () -> mc.displayGuiScreen(new GuiModList(null))));
		this.steps.add(new Step("done", () -> mc.displayGuiScreen(null)));
	}

	/** One tick of the tour; true when it is over. */
	boolean tick() {
		if (this.index >= this.steps.size()) {
			return true;
		}
		Step step = this.steps.get(this.index);
		if (this.ticks == 0) {
			try {
				step.action.run();
			} catch (Throwable t) {
				LOGGER.error("Screen tour: {} failed", step.name, t);
			}
		}
		this.ticks++;
		if (this.ticks > WAIT) {
			if (!"done".equals(step.name)) {
				this.picture(step.name);
			}
			this.index++;
			this.ticks = 0;
		}
		return false;
	}

	/** The last frame as a PNG (what the player sees now). */
	private void picture(String name) {
		try {
			Framebuffer buffer = this.mc.getFramebuffer();
			int width = buffer.framebufferTextureWidth;
			int height = buffer.framebufferTextureHeight;
			java.nio.IntBuffer pixels = org.lwjgl.BufferUtils.createIntBuffer(width * height);
			int[] data = new int[width * height];
			org.lwjgl.opengl.GL11.glPixelStorei(org.lwjgl.opengl.GL11.GL_PACK_ALIGNMENT, 1);
			org.lwjgl.opengl.GL11.glPixelStorei(org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT, 1);
			net.minecraft.client.renderer.GlStateManager.bindTexture(buffer.framebufferTexture);
			org.lwjgl.opengl.GL11.glGetTexImage(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0, org.lwjgl.opengl.GL12.GL_BGRA,
					org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixels);
			pixels.get(data);
			TextureUtil.processPixelValues(data, width, height);
			var image = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
			image.setRGB(0, 0, width, height, data, 0, width);
			javax.imageio.ImageIO.write(image, "png", new File(this.folder, name + ".png"));
			LOGGER.info("Screen tour: {} saved", name);
		} catch (Throwable t) {
			LOGGER.error("Screen tour: no picture of {}", name, t);
		}
	}

	private static final class Step {
		final String name;
		final Runnable action;

		Step(String name, Runnable action) {
			this.name = name;
			this.action = action;
		}
	}
}
