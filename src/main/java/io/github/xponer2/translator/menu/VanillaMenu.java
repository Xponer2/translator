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

import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

/**
 * 1.21 {@code player.containerMenu} while a vanilla 1.12.2 container is open (most of the time the player's own
 * inventory): 1.21 code reads it every tick and it is never null there (Sophisticated Core's tick handler takes
 * {@code containerMenu.getCarried()}). A view of that container: its slots and window id, the player's carried stack,
 * the container doing the work. One per container, so a mod comparing menus sees the same object.
 */
public final class VanillaMenu extends AbstractContainerMenu {
	private static final Map<Container, VanillaMenu> VIEWS = new WeakHashMap<>();

	private final Container container;

	private VanillaMenu(Container container, EntityPlayer player) {
		super(null, container.windowId);
		this.container = container;
		this.setPlayer(player);
		this.inventorySlots.addAll(container.inventorySlots);
	}

	/** The open menu of a player as 1.21 code sees it: null only when nothing is open. */
	@Nullable
	public static AbstractContainerMenu of(EntityPlayer player) {
		Container open = player.openContainer;
		if (open == null || open instanceof AbstractContainerMenu) {
			return (AbstractContainerMenu) open;
		}
		synchronized (VIEWS) {
			return VIEWS.computeIfAbsent(open, container -> new VanillaMenu(container, player));
		}
	}

	@Override
	public ItemStack quickMoveStack(EntityPlayer player, int index) {
		return this.container.transferStackInSlot(player, index);
	}

	@Override
	public boolean stillValid(EntityPlayer player) {
		return this.container.canInteractWith(player);
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, EntityPlayer player) {
		this.container.slotClick(slotId, button, clickType, player);
	}

	@Override
	public void detectAndSendChanges() {
		this.container.detectAndSendChanges();
	}
}
