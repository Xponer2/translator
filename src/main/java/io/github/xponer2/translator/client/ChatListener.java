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

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 {@code ChatListener} (C row; {@code minecraft.getChatListener()}): the game's messages to the player go to
 * 1.12.2's chat or, as overlay, above the hotbar (Jade's toggle messages).
 */
@SideOnly(Side.CLIENT)
public class ChatListener {
	public static final ChatListener INSTANCE = new ChatListener();

	/** 1.21 {@code handleSystemMessage(message, overlay)}. */
	public void handleSystemMessage(@Nullable ITextComponent message, boolean overlay) {
		Minecraft minecraft = Minecraft.getMinecraft();
		if (message == null || minecraft.ingameGUI == null) {
			return;
		}
		if (overlay) {
			minecraft.ingameGUI.setOverlayMessage(message, false);
		} else {
			minecraft.ingameGUI.getChatGUI().printChatMessage(message);
		}
	}

	/** 1.21 {@code handleDisguisedChatMessage(message, chatType)}: the message as chat. */
	public void handleDisguisedChatMessage(@Nullable ITextComponent message, @Nullable Object chatType) {
		this.handleSystemMessage(message, false);
	}

	/** 1.21 {@code queueSize()} / {@code clearQueue()}: 1.12.2 shows messages at once. */
	public long queueSize() {
		return 0L;
	}

	public void clearQueue() {
	}
}
