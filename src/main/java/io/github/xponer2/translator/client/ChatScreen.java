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

import java.io.IOException;

import io.github.xponer2.translator.runtime.neoforge.KeyMapping;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 {@code ChatScreen} for the mods' screens that extend it (the table's E line: only those). 1.12.2's chat makes its
 * input a GuiTextField, the mods read it as 1.21's EditBox (JourneyMap's chat on the full screen map threw a
 * ClassCastException from its right-click menu). Their 1.21 methods (render, keyPressed, charTyped, tick, removed)
 * run 1.12.2's chat; 1.12.2's calls go to them, so a mod's override is what runs.
 */
@SideOnly(Side.CLIENT)
public class ChatScreen extends GuiChat {
	public ChatScreen(String initial) {
		super(initial);
	}

	// --- 1.12.2 calls, to the 1.21 methods a mod overrides --------------------------------------------------------------

	@Override
	public void initGui() {
		this.init();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.render(new GuiGraphics(this.mc), mouseX, mouseY, partialTicks);
	}

	@Override
	public void updateScreen() {
		this.tick();
	}

	@Override
	public void onGuiClosed() {
		this.removed();
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (typedChar >= ' ' && typedChar != 127 && this.charTyped(typedChar, 0)) {
			return;
		}
		this.chatKey(typedChar, keyCode);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		this.mouseClicked((double) mouseX, mouseY, mouseButton);
	}

	// --- 1.21 methods, run as 1.12.2's chat ------------------------------------------------------------------------

	/** 1.21 {@code init()}: 1.12.2's chat set up, its input made an EditBox. */
	protected void init() {
		super.initGui();
		GuiTextField old = this.inputField;
		EditBox input = new EditBox(this.fontRenderer, old.x, old.y, old.width, old.height, new TextComponentString(""));
		input.setMaxStringLength(old.getMaxStringLength());
		input.setEnableBackgroundDrawing(false);
		input.setFocused(true);
		input.setText(old.getText());
		input.setCanLoseFocus(false);
		this.inputField = input;
		replaceTabCompleter(this, input);
	}

	public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
		super.drawScreen(mouseX, mouseY, partialTick);
	}

	public void tick() {
		super.updateScreen();
	}

	public void removed() {
		super.onGuiClosed();
	}

	/** 1.21 {@code charTyped(c, modifiers)}: the character into the input. */
	public boolean charTyped(char c, int modifiers) {
		return this.inputField.textboxKeyTyped(c, 0);
	}

	/** 1.21 {@code keyPressed(glfwKey, scancode, modifiers)}: 1.12.2's chat keys (enter sends, escape, history, editing). */
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		try {
			this.chatKey('\0', KeyMapping.toLwjgl(keyCode));
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		try {
			super.mouseClicked((int) mouseX, (int) mouseY, button);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	/** 1.21 {@code handleChatInput(text, addToHistory)}: the line sent as chat or a command, as 1.12.2's chat sends it. */
	public void handleChatInput(String text, boolean addToHistory) {
		this.sendChatMessage(text, addToHistory);
	}

	/** 1.12.2's chat key handling. */
	private void chatKey(char typedChar, int keyCode) throws IOException {
		super.keyTyped(typedChar, keyCode);
	}

	/** 1.12.2's tab completer holds the input it was made with: one for the EditBox. */
	private static void replaceTabCompleter(GuiChat chat, GuiTextField input) {
		for (java.lang.reflect.Field field : GuiChat.class.getDeclaredFields()) {
			if (net.minecraft.util.TabCompleter.class.isAssignableFrom(field.getType())) {
				try {
					field.setAccessible(true);
					field.set(chat, new GuiChat.ChatTabCompleter(input));
				} catch (ReflectiveOperationException | RuntimeException e) {
					// the old completer stays (it completes into the old field)
				}
			}
		}
	}
}
