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
import net.minecraft.item.ItemStack;

/**
 * 1.21 {@code ChestMenu}: 1 to 6 rows of 9 slots of a container over the player's inventory, the menu of the vanilla
 * chest types ({@link MenuType#GENERIC_9x3} ...) that mods open for their cabinets, drawers and crates. It was the 1.12.2
 * ContainerChest, which isn't a menu of the Translator: every mod's {@code createMenu} returning one failed to verify.
 */
public class ChestMenu extends AbstractContainerMenu {
	private final IInventory container;
	private final int containerRows;

	public ChestMenu(MenuType<?> type, int containerId, InventoryPlayer playerInventory, int rows) {
		this(type, containerId, playerInventory, new InventoryBasic("", false, 9 * rows), rows);
	}

	public ChestMenu(MenuType<?> type, int containerId, InventoryPlayer playerInventory, IInventory container, int rows) {
		super(type, containerId);
		if (container.getSizeInventory() < rows * 9) {
			throw new IllegalArgumentException("Container size " + container.getSizeInventory() + " is smaller than expected " + rows * 9);
		}
		this.container = container;
		this.containerRows = rows;
		container.openInventory(playerInventory.player);
		int offset = (rows - 4) * 18;
		for (int row = 0; row < rows; row++) {
			for (int column = 0; column < 9; column++) {
				this.addSlotToContainer(new Slot(container, column + row * 9, 8 + column * 18, 18 + row * 18));
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				this.addSlotToContainer(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 103 + row * 18 + offset));
			}
		}
		for (int column = 0; column < 9; column++) {
			this.addSlotToContainer(new Slot(playerInventory, column, 8 + column * 18, 161 + offset));
		}
	}

	public static ChestMenu oneRow(int containerId, InventoryPlayer playerInventory) {
		return new ChestMenu(MenuType.GENERIC_9x1, containerId, playerInventory, 1);
	}

	public static ChestMenu twoRows(int containerId, InventoryPlayer playerInventory) {
		return new ChestMenu(MenuType.GENERIC_9x2, containerId, playerInventory, 2);
	}

	public static ChestMenu threeRows(int containerId, InventoryPlayer playerInventory) {
		return new ChestMenu(MenuType.GENERIC_9x3, containerId, playerInventory, 3);
	}

	public static ChestMenu fourRows(int containerId, InventoryPlayer playerInventory) {
		return new ChestMenu(MenuType.GENERIC_9x4, containerId, playerInventory, 4);
	}

	public static ChestMenu fiveRows(int containerId, InventoryPlayer playerInventory) {
		return new ChestMenu(MenuType.GENERIC_9x5, containerId, playerInventory, 5);
	}

	public static ChestMenu sixRows(int containerId, InventoryPlayer playerInventory) {
		return new ChestMenu(MenuType.GENERIC_9x6, containerId, playerInventory, 6);
	}

	public static ChestMenu threeRows(int containerId, InventoryPlayer playerInventory, IInventory container) {
		return new ChestMenu(MenuType.GENERIC_9x3, containerId, playerInventory, container, 3);
	}

	public static ChestMenu sixRows(int containerId, InventoryPlayer playerInventory, IInventory container) {
		return new ChestMenu(MenuType.GENERIC_9x6, containerId, playerInventory, container, 6);
	}

	@Override
	public boolean stillValid(EntityPlayer player) {
		return this.container.isUsableByPlayer(player);
	}

	/** Shift-click: from the container to the player's inventory and back. */
	@Override
	public ItemStack quickMoveStack(EntityPlayer player, int index) {
		ItemStack moved = ItemStack.EMPTY;
		Slot slot = this.inventorySlots.get(index);
		if (slot != null && slot.getHasStack()) {
			ItemStack stack = slot.getStack();
			moved = stack.copy();
			int size = this.containerRows * 9;
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
		this.container.closeInventory(player);
	}

	public IInventory getContainer() {
		return this.container;
	}

	public int getRowCount() {
		return this.containerRows;
	}
}
