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

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 {@code KeyboardHandler} ({@code minecraft.keyboardHandler}): the clipboard, which 1.12.2 keeps in GuiScreen
 * (8 mods copy coordinates, ids and texts: JEI, Jade, Create, Mekanism).
 */
@SideOnly(Side.CLIENT)
public class KeyboardHandler {
	public static final KeyboardHandler INSTANCE = new KeyboardHandler();

	public String getClipboard() {
		return GuiScreen.getClipboardString();
	}

	public void setClipboard(String text) {
		GuiScreen.setClipboardString(text == null ? "" : text);
	}
}
