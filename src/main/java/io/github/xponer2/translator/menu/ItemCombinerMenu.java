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

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * 1.21 {@code ItemCombinerMenu} (C row): a menu that combines its input items into one result, as an anvil or a
 * smithing table does. The subclass says where its slots are (createInputSlotDefinitions) and makes the result
 * (createResult, 1.12.2's name updateRepairOutput). The tables had made it 1.12.2's anvil container, whose constructor
 * takes a world and a position: Iron's Spellbooks' arcane anvil, and the smithing table of the 1.21 API, couldn't be
 * made at all.
 */
public abstract class ItemCombinerMenu extends AbstractContainerMenu {
	protected final ContainerLevelAccess access;
	protected final EntityPlayer player;
	protected final IInventory inputSlots;
	protected final InventoryCraftResult resultSlots = new InventoryCraftResult();
	private final List<Integer> inputSlotIndexes;
	private final int resultSlotIndex;

	public ItemCombinerMenu(@Nullable MenuType<?> menuType, int containerId, InventoryPlayer playerInventory, ContainerLevelAccess access) {
		super(menuType, containerId);
		this.access = access;
		this.player = playerInventory.player;
		ItemCombinerMenuSlotDefinition definition = this.createInputSlotDefinitions();
		this.inputSlots = this.createContainer(definition.getNumOfInputSlots());
		this.inputSlotIndexes = definition.getInputSlotIndexes();
		this.resultSlotIndex = definition.getResultSlotIndex();
		this.createInputSlots(definition);
		this.createResultSlot(definition);
		this.createInventorySlots(playerInventory);
	}

	protected abstract boolean mayPickup(EntityPlayer player, boolean hasStack);

	protected abstract void onTake(EntityPlayer player, ItemStack stack);

	protected abstract boolean isValidBlock(IBlockState state);

	/** 1.21 {@code createResult()}: the result of the inputs into the result slot. */
	public abstract void updateRepairOutput();

	protected abstract ItemCombinerMenuSlotDefinition createInputSlotDefinitions();

	/** The input slots (NeoForge makes it protected: Sophisticated Backpacks' anvil upgrade puts its own). */
	protected void createInputSlots(ItemCombinerMenuSlotDefinition definition) {
		for (ItemCombinerMenuSlotDefinition.SlotDefinition slot : definition.getSlots()) {
			this.addSlotToContainer(new Slot(this.inputSlots, slot.slotIndex(), slot.x(), slot.y()) {
				@Override
				public boolean isItemValid(ItemStack stack) {
					return slot.mayPlace().test(stack);
				}
			});
		}
	}

	protected void createResultSlot(ItemCombinerMenuSlotDefinition definition) {
		ItemCombinerMenuSlotDefinition.SlotDefinition result = definition.getResultSlot();
		this.addSlotToContainer(new Slot(this.resultSlots, result.slotIndex(), result.x(), result.y()) {
			@Override
			public boolean isItemValid(ItemStack stack) {
				return false;
			}

			@Override
			public boolean canTakeStack(EntityPlayer player) {
				return ItemCombinerMenu.this.mayPickup(player, this.getHasStack());
			}

			@Override
			public ItemStack onTake(EntityPlayer player, ItemStack stack) {
				ItemCombinerMenu.this.onTake(player, stack);
				return stack;
			}
		});
	}

	/** The player's inventory: three rows, then the hotbar. */
	protected void createInventorySlots(InventoryPlayer inventory) {
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				this.addSlotToContainer(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			this.addSlotToContainer(new Slot(inventory, column, 8 + column * 18, 142));
		}
	}

	/** The inputs; each change makes the result again. */
	protected InventoryBasic createContainer(int size) {
		return new SimpleContainer(size) {
			@Override
			public void markDirty() {
				super.markDirty();
				ItemCombinerMenu.this.onCraftMatrixChanged(this);
			}
		};
	}

	@Override
	public void onCraftMatrixChanged(IInventory inventory) {
		super.onCraftMatrixChanged(inventory);
		if (inventory == this.inputSlots) {
			this.updateRepairOutput();
		}
	}

	/** The inputs go back to the player when the menu closes (a menu of no block keeps them, as in 1.21). */
	@Override
	public void onContainerClosed(EntityPlayer player) {
		super.onContainerClosed(player);
		this.access.execute((level, pos) -> this.clearContainer(player, this.inputSlots));
	}

	@Override
	public boolean stillValid(EntityPlayer player) {
		return this.access.evaluate((level, pos) -> this.isValidBlock(level.getBlockState(pos))
				&& player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D, true);
	}

	/** Shift-click: the result into the inventory, an input out, an item into the inputs, or between inventory and hotbar. */
	@Override
	public ItemStack quickMoveStack(EntityPlayer player, int index) {
		Slot slot = index >= 0 && index < this.inventorySlots.size() ? this.inventorySlots.get(index) : null;
		if (slot == null || !slot.getHasStack()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getStack();
		ItemStack before = stack.copy();
		int inventoryStart = this.getResultSlot() + 1;
		int inventoryEnd = inventoryStart + 27;
		int hotbarEnd = inventoryEnd + 9;
		if (index == this.getResultSlot()) {
			if (!this.mergeItemStack(stack, inventoryStart, hotbarEnd, true)) {
				return ItemStack.EMPTY;
			}
			slot.onSlotChange(stack, before);
		} else if (this.inputSlotIndexes.contains(index)) {
			if (!this.mergeItemStack(stack, inventoryStart, hotbarEnd, false)) {
				return ItemStack.EMPTY;
			}
		} else if (this.canMoveIntoInputSlots(stack) && index >= inventoryStart && index < hotbarEnd) {
			if (!this.mergeItemStack(stack, this.getSlotToQuickMoveTo(before), this.getResultSlot(), false)) {
				return ItemStack.EMPTY;
			}
		} else if (index >= inventoryStart && index < inventoryEnd) {
			if (!this.mergeItemStack(stack, inventoryEnd, hotbarEnd, false)) {
				return ItemStack.EMPTY;
			}
		} else if (index >= inventoryEnd && index < hotbarEnd && !this.mergeItemStack(stack, inventoryStart, inventoryEnd, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.putStack(ItemStack.EMPTY);
		} else {
			slot.onSlotChanged();
		}
		if (stack.getCount() == before.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack);
		return before;
	}

	protected boolean canMoveIntoInputSlots(ItemStack stack) {
		return true;
	}

	public int getSlotToQuickMoveTo(ItemStack stack) {
		return this.inputSlots.isEmpty() ? 0 : this.inputSlotIndexes.get(0);
	}

	public int getResultSlot() {
		return this.resultSlotIndex;
	}

	/** 1.21 {@code canTakeItemForPickAll(stack, slot)}: a double click doesn't gather from the result slot. */
	@Override
	public boolean canMergeSlot(ItemStack stack, Slot slot) {
		return slot.inventory != this.resultSlots && super.canMergeSlot(stack, slot);
	}
}
