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
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 {@code ConfirmLinkScreen} (C row): asks before a link opens in the browser: open, copy or cancel (Patchouli's,
 * GuideMe's, Create's, the Aether's and Cloth Config's links, Jade's).
 */
@SideOnly(Side.CLIENT)
public class ConfirmLinkScreen extends ConfirmScreen {
	protected final String url;
	protected final boolean showWarning;

	public ConfirmLinkScreen(Object callback, String url, boolean trusted) {
		super(callback, new TextComponentTranslation(trusted ? "chat.link.confirmTrusted" : "chat.link.confirm"), new TextComponentString(url),
				new TextComponentTranslation("chat.link.open"), new TextComponentTranslation("gui.cancel"));
		this.url = url;
		this.showWarning = !trusted;
	}

	@Override
	protected void addButtons(int y) {
		this.addExitButton(Button.builder(this.yesButton, button -> this.answer(true)).bounds(this.width / 2 - 50 - 105, y, 100, 20).build());
		this.addExitButton(Button.builder(new TextComponentTranslation("chat.copy"), button -> {
			this.copyToClipboard();
			this.answer(false);
		}).bounds(this.width / 2 - 50, y, 100, 20).build());
		this.addExitButton(Button.builder(this.noButton, button -> this.answer(false)).bounds(this.width / 2 - 50 + 105, y, 100, 20).build());
	}

	public void copyToClipboard() {
		GuiScreen.setClipboardString(this.url);
	}

	/** 1.21 {@code confirmLinkNow(lastScreen, url)}: asks, opens the link if wanted, then back to that screen. */
	public static void confirmLinkNow(@Nullable GuiScreen lastScreen, String url) {
		confirmLinkNow(lastScreen, url, true);
	}

	public static void confirmLinkNow(@Nullable GuiScreen lastScreen, String url, boolean trusted) {
		Minecraft minecraft = Minecraft.getMinecraft();
		java.util.function.Consumer<Boolean> answer = open -> {
			if (open) {
				openLink(url);
			}
			minecraft.displayGuiScreen(lastScreen);
		};
		minecraft.displayGuiScreen(new ConfirmLinkScreen(answer, url, trusted));
	}

	/** 1.21 {@code confirmLink(lastScreen, url)}: a button press that asks so. */
	public static Button.OnPress confirmLink(@Nullable GuiScreen lastScreen, String url) {
		return button -> confirmLinkNow(lastScreen, url);
	}

	public static Button.OnPress confirmLink(@Nullable GuiScreen lastScreen, String url, boolean trusted) {
		return button -> confirmLinkNow(lastScreen, url, trusted);
	}

	/** Opens the link in the system's browser. */
	static void openLink(String url) {
		try {
			org.lwjgl.Sys.openURL(url);
		} catch (RuntimeException | LinkageError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("opening the link " + url, e);
		}
	}
}
