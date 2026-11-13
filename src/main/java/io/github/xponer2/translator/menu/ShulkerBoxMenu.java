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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotShulkerBox;
import net.minecraft.item.ItemStack;

/**
 * 1.21 {@code ShulkerBoxMenu}: a 27 slot box (shulker boxes stay out) over the player's inventory. 1.12.2's
 * ContainerShulkerBox isn't a 1.21 menu: the menus mods made of it (Supplementaries' safe) didn't verify where the
 * mod returns one as its AbstractContainerMenu. The slots go in through {@link #addSlot}, which the mods override.
 */
public class ShulkerBoxMenu extends AbstractContainerMenu {
	private static final int CONTAINER_SIZE = 27;
	/** 1.21 {@code container} (the 1.21 name maps to 1.12.2's {@code inventory}): set before the slots go in. */
	public final IInventory inventory;

	public ShulkerBoxMenu(int containerId, InventoryPlayer playerInventory) {
		this(containerId, playerInventory, new InventoryBasic("", false, CONTAINER_SIZE));
	}

	public ShulkerBoxMenu(int containerId, InventoryPlayer playerInventory, IInventory container) {
		super(MenuType.SHULKER_BOX, containerId);
		if (container.getSizeInventory() < CONTAINER_SIZE) {
			throw new IllegalArgumentException("Container size " + container.getSizeInventory() + " is smaller than expected " + CONTAINER_SIZE);
		}
		this.inventory = container;
		container.openInventory(playerInventory.player);
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				this.addSlot(new SlotShulkerBox(container, column + row * 9, 8 + column * 18, 18 + row * 18));
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
		}
	}

	@Override
	public boolean stillValid(EntityPlayer player) {
		return this.inventory.isUsableByPlayer(player);
	}

	/** Shift-click: from the box to the player's inventory and back. */
	@Override
	public ItemStack quickMoveStack(EntityPlayer player, int index) {
		ItemStack moved = ItemStack.EMPTY;
		Slot slot = this.inventorySlots.get(index);
		if (slot != null && slot.getHasStack()) {
			ItemStack stack = slot.getStack();
			moved = stack.copy();
			int size = this.inventory.getSizeInventory();
			if (index < size) {
				if (!this.mergeItemStack(stack, size, this.inventorySlots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.mergeItemStack(stack, 0, size, false)) {
				return ItemStack.EMPTY;
			}
			if (stack.isEmpty()) {
				slot.putStack(ItemStack.EMPTY);
			} else {
				slot.onSlotChanged();
			}
		}
		return moved;
	}

	@Override
	public void onContainerClosed(EntityPlayer player) {
		super.onContainerClosed(player);
		this.inventory.closeInventory(player);
	}
}
