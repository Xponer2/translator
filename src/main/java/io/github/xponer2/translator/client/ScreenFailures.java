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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A 1.21.1 mod's screen that throws while it draws, ticks or takes a click or key (a part the Translator can't run
 * yet): closed with a line in the chat and the error logged once, instead of the game crashing with it.
 */
@SideOnly(Side.CLIENT)
final class ScreenFailures {
	private ScreenFailures() {
	}

	static void closeBroken(GuiScreen screen, Throwable error) {
		closeBroken(screen, "render", error);
	}

	/**
	 * The 1.21 screen's removed(), called when it closes: an error there (a screen whose init failed, Create's package
	 * port) is logged, the screen closes.
	 */
	static void removed(GuiScreen screen) {
		try {
			if (screen instanceof Screen plain) {
				plain.removed();
			} else if (screen instanceof AbstractContainerScreen<?> container) {
				container.removed();
			}
		} catch (RuntimeException | LinkageError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(screen.getClass().getName() + ".removed", e);
		}
	}

	/** {@code what}: render, tick or input. */
	static void closeBroken(GuiScreen screen, String what, Throwable error) {
		String name = screen.getClass().getName();
		io.github.xponer2.translator.runtime.RuntimeStubs.failed(name + "." + what, error);
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.currentScreen == screen) {
			try {
				mc.displayGuiScreen(null);
			} catch (RuntimeException | LinkageError closing) {
				// the screen's own onGuiClosed failed too: it goes anyway
				io.github.xponer2.translator.runtime.RuntimeStubs.failed(name + ".onGuiClosed", closing);
				mc.currentScreen = null;
				mc.setIngameFocus();
			}
			if (mc.player != null) {
				mc.player.sendMessage(new TextComponentString(TextFormatting.RED + "Translator: " + screen.getClass().getSimpleName()
						+ " can't be shown on 1.12.2 yet (" + error.getClass().getSimpleName() + ", see the log)"));
			}
		}
	}
}
