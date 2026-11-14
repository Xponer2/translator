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
package io.github.xponer2.translator.runtime.redirect;

import javax.annotation.Nullable;

import io.github.xponer2.translator.client.AbstractContainerScreen;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

/**
 * 1.21's AbstractContainerScreen on 1.12.2's GuiContainer: the table maps the class to GuiContainer, so a chest's or
 * the inventory's screen is a container screen as in 1.21 (JEI's overlay and Mouse Tweaks looked only at the mods'
 * screens, which extend the facade), and the calls the mods make on one come here: the facade's own for a mod's
 * screen, 1.12.2's fields for the game's.
 */
public final class ContainerScreenBridges {
	private ContainerScreenBridges() {
	}

	/** 1.21 {@code screen.getMenu()}: the container it shows. */
	public static Container getMenu(GuiContainer screen) {
		return screen instanceof AbstractContainerScreen<?> own ? own.getMenu() : screen.inventorySlots;
	}

	/** 1.21 {@code screen.getTitle()}: the facade's, else the container's name if it has one. */
	public static ITextComponent getTitle(GuiContainer screen) {
		if (screen instanceof AbstractContainerScreen<?> own) {
			return own.getTitle();
		}
		return new TextComponentString("");
	}

	/** 1.21 {@code isHovering(slot, mouseX, mouseY)}: the mouse over the slot's 16x16 square. */
	public static boolean isHovering(GuiContainer screen, Slot slot, double mouseX, double mouseY) {
		return isHovering(screen, slot.xPos, slot.yPos, 16, 16, mouseX, mouseY);
	}

	/** 1.21 {@code isHovering(x, y, width, height, mouseX, mouseY)}: a square of the screen (its own coordinates) under the mouse. */
	public static boolean isHovering(GuiContainer screen, int x, int y, int width, int height, double mouseX, double mouseY) {
		double localX = mouseX - screen.getGuiLeft();
		double localY = mouseY - screen.getGuiTop();
		return localX >= x - 1 && localX < x + width + 1 && localY >= y - 1 && localY < y + height + 1;
	}

	/** 1.21 {@code findSlot(mouseX, mouseY)}: the slot under that point, or null. */
	@Nullable
	public static Slot findSlot(GuiContainer screen, double mouseX, double mouseY) {
		for (Slot slot : screen.inventorySlots.inventorySlots) {
			if (slot.isEnabled() && isHovering(screen, slot, mouseX, mouseY)) {
				return slot;
			}
		}
		return null;
	}
}
