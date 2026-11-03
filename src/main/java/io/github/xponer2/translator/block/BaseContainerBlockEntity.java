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
package io.github.xponer2.translator.block;

import javax.annotation.Nullable;

import io.github.xponer2.translator.menu.AbstractContainerMenu;
import io.github.xponer2.translator.menu.MenuProvider;
import io.github.xponer2.translator.registry.BlockEntityType;
import io.github.xponer2.translator.registry.HolderLookup;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;

/**
 * 1.21 {@code BaseContainerBlockEntity} (neoforge-1.21.1-runtime.tsv): a block entity with slots and a menu. Mods
 * implement it under the 1.21 names (getItems, getContainerSize, createMenu, getDefaultName); the 1.12.2 inventory
 * (hoppers, comparators, the menu's slots) reaches them through IInventory. Its custom name is saved as 1.21 does.
 */
public abstract class BaseContainerBlockEntity extends BlockEntity implements IInventory, MenuProvider {
	@Nullable
	private ITextComponent name;

	protected BaseContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, IBlockState state) {
		super(type, pos, state);
	}

	// --- 1.21: what a mod implements ------------------------------------------------------------------------------

	protected abstract ITextComponent getDefaultName();

	protected abstract NonNullList<ItemStack> getItems();

	protected abstract void setItems(NonNullList<ItemStack> items);

	protected abstract AbstractContainerMenu createMenu(int id, InventoryPlayer inventory);

	public abstract int getContainerSize();

	// --- 1.21 Container, the defaults ------------------------------------------------------------------------------

	public boolean isEmpty() {
		for (ItemStack stack : this.getItems()) {
			if (!stack.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public ItemStack getItem(int slot) {
		return this.getItems().get(slot);
	}

	public ItemStack removeItem(int slot, int count) {
		ItemStack taken = ItemStackHelper.getAndSplit(this.getItems(), slot, count);
		if (!taken.isEmpty()) {
			this.setChanged();
		}
		return taken;
	}

	public ItemStack removeItemNoUpdate(int slot) {
		return ItemStackHelper.getAndRemove(this.getItems(), slot);
	}

	public void setItem(int slot, ItemStack stack) {
		this.getItems().set(slot, stack);
		if (stack.getCount() > this.getMaxStackSize()) {
			stack.setCount(this.getMaxStackSize());
		}
		this.setChanged();
	}

	public int getMaxStackSize() {
		return 64;
	}

	public boolean stillValid(EntityPlayer player) {
		return this.world != null && this.world.getTileEntity(this.pos) == this
				&& player.getDistanceSq(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) <= 64;
	}

	public void startOpen(EntityPlayer player) {
	}

	public void stopOpen(EntityPlayer player) {
	}

	public boolean canPlaceItem(int slot, ItemStack stack) {
		return true;
	}

	public void clearContent() {
		this.getItems().clear();
	}

	/** 1.21 lock check (a lock key the player must hold): 1.12.2 containers here have no lock. */
	public boolean canOpen(EntityPlayer player) {
		return true;
	}

	public void setCustomName(@Nullable ITextComponent name) {
		this.name = name;
	}

	@Nullable
	public ITextComponent getCustomName() {
		return this.name;
	}

	/** 1.12.2 IWorldNameable (its getName is the string; 1.21's text is getDisplayName here). */
	@Override
	public String getName() {
		return this.getDisplayName().getFormattedText();
	}

	// --- the menu ---------------------------------------------------------------------------------------------------

	@Override
	@Nullable
	public Container createMenu(int windowId, InventoryPlayer playerInventory, EntityPlayer player) {
		return this.canOpen(player) ? this.createMenu(windowId, playerInventory) : null;
	}

	@Override
	public ITextComponent getDisplayName() {
		return this.name != null ? this.name : this.getDefaultName();
	}

	// --- 1.21 save and load: the custom name ---------------------------------------------------------------------------

	@Override
	protected void loadAdditional(NBTTagCompound tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.hasKey("CustomName", 8)) {
			try {
				this.name = ITextComponent.Serializer.jsonToComponent(tag.getString("CustomName"));
			} catch (RuntimeException e) {
				this.name = null;
			}
		}
	}

	@Override
	protected void saveAdditional(NBTTagCompound tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		if (this.name != null) {
			tag.setString("CustomName", ITextComponent.Serializer.componentToJson(this.name));
		}
	}

	// --- 1.12.2 IInventory: the 1.21 methods ----------------------------------------------------------------------------

	@Override
	public int getSizeInventory() {
		return this.getContainerSize();
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return this.getItem(slot);
	}

	@Override
	public ItemStack decrStackSize(int slot, int count) {
		return this.removeItem(slot, count);
	}

	@Override
	public ItemStack removeStackFromSlot(int slot) {
		return this.removeItemNoUpdate(slot);
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) {
		this.setItem(slot, stack);
	}

	@Override
	public int getInventoryStackLimit() {
		return this.getMaxStackSize();
	}

	@Override
	public boolean isUsableByPlayer(EntityPlayer player) {
		return this.stillValid(player);
	}

	@Override
	public void openInventory(EntityPlayer player) {
		this.startOpen(player);
	}

	@Override
	public void closeInventory(EntityPlayer player) {
		this.stopOpen(player);
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		return this.canPlaceItem(slot, stack);
	}

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {
	}

	@Override
	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clear() {
		this.clearContent();
	}

	@Override
	public boolean hasCustomName() {
		return this.name != null;
	}
}
