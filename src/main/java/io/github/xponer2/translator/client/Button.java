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

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;

/**
 * 1.21 button: a widget that calls its OnPress when clicked. Draws the vanilla 1.12.2 button unless a subclass
 * draws its own in renderWidget.
 */
public class Button extends AbstractWidget {
	public static final CreateNarration DEFAULT_NARRATION = Supplier::get;

	protected final OnPress onPress;
	protected final CreateNarration createNarration;

	protected Button(int x, int y, int width, int height, ITextComponent message, OnPress onPress, CreateNarration createNarration) {
		super(x, y, width, height, message);
		this.onPress = onPress;
		this.createNarration = createNarration;
	}

	public void onPress() {
		this.onPress.onPress(this);
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		this.onPress();
	}

	@Override
	protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
		this.renderDefaultButton(Minecraft.getMinecraft(), mouseX, mouseY, partialTicks);
	}

	@FunctionalInterface
	public interface OnPress {
		void onPress(Button button);
	}

	@FunctionalInterface
	public interface CreateNarration {
		ITextComponent createNarrationMessage(Supplier<ITextComponent> message);
	}
}
