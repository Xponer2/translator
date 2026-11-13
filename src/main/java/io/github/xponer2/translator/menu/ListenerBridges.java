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
package io.github.xponer2.translator.menu;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.item.ItemStack;

/**
 * 1.21 {@code ContainerListener} calls on 1.12.2's IContainerListener (runtime, a missing call on it comes here; the
 * menu's own {@code triggerSlotListeners} too). A server player is a listener in 1.12.2 and sends the change to its
 * client; in 1.21 its menu's synchronizer does that ({@link AbstractContainerMenu#synchronizer}), so a menu with one
 * doesn't send it twice (and not as 1.12.2's packet, whose count is a byte).
 */
public final class ListenerBridges {
	private ListenerBridges() {
	}

	/** 1.21 {@code listener.slotChanged(menu, slot, stack)}. */
	public static void slotChanged(IContainerListener listener, AbstractContainerMenu menu, int slot, ItemStack stack) {
		if (listener instanceof EntityPlayerMP && menu.synchronizer != null) {
			return;
		}
		listener.sendSlotContents(menu, slot, stack);
	}

	/** The same call by its 1.12.2 name (the table renames it), the menu typed as the 1.21 class. */
	public static void sendSlotContents(IContainerListener listener, AbstractContainerMenu menu, int slot, ItemStack stack) {
		slotChanged(listener, menu, slot, stack);
	}

	/** 1.21 {@code listener.dataChanged(menu, id, value)}. */
	public static void dataChanged(IContainerListener listener, AbstractContainerMenu menu, int id, int value) {
		if (listener instanceof EntityPlayerMP && menu.synchronizer != null) {
			return;
		}
		listener.sendWindowProperty(menu, id, value);
	}

	public static void sendWindowProperty(IContainerListener listener, AbstractContainerMenu menu, int id, int value) {
		dataChanged(listener, menu, id, value);
	}

	/**
	 * 1.12.2's {@code sendAllContents(menu, items)} on a 1.21 listener (a mod's screen listening to its menu:
	 * Supplementaries' presents and cannon): 1.21 has no such call, its listeners get the changes one by one. Nothing,
	 * without the "not run" warning each listener gave.
	 */
	public static void sendAllContents(IContainerListener listener, net.minecraft.inventory.Container menu, net.minecraft.util.NonNullList<ItemStack> items) {
	}

	/** 1.12.2's {@code sendAllWindowProperties(menu, inventory)} on a 1.21 listener: the same. */
	public static void sendAllWindowProperties(IContainerListener listener, net.minecraft.inventory.Container menu, net.minecraft.inventory.IInventory inventory) {
	}
}
