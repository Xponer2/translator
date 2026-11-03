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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

/**
 * 1.21 container on the 1.12.2 Container: keeps its menu type and window id, and syncs its {@link ContainerData}
 * to the client as window properties (1.21 data slots).
 */
public abstract class AbstractContainerMenu extends Container {
	@Nullable
	private final MenuType<?> menuType;
	private final List<ContainerData> dataSlots = new ArrayList<>();
	private final List<int[]> lastData = new ArrayList<>();
	/** The player using the menu (1.12.2 keeps the carried stack in the player's inventory). */
	@Nullable
	private EntityPlayer player;
	private ItemStack clickResult = ItemStack.EMPTY;

	protected AbstractContainerMenu(@Nullable MenuType<?> menuType, int windowId) {
		this.menuType = menuType;
		this.windowId = windowId;
	}

	public MenuType<?> getType() {
		if (this.menuType == null) {
			throw new UnsupportedOperationException("Unable to construct this menu by type");
		}

		return this.menuType;
	}

	// 1.21 names of Container#transferStackInSlot / #canInteractWith; a subclass may override either name

	public net.minecraft.item.ItemStack quickMoveStack(net.minecraft.entity.player.EntityPlayer player, int index) {
		return net.minecraft.item.ItemStack.EMPTY;
	}

	public boolean stillValid(net.minecraft.entity.player.EntityPlayer player) {
		return true;
	}

	@Override
	public net.minecraft.item.ItemStack transferStackInSlot(net.minecraft.entity.player.EntityPlayer player, int index) {
		return this.quickMoveStack(player, index);
	}

	@Override
	public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) {
		return this.stillValid(player);
	}

	// 1.21 names of the slot helpers; `slots` is the same list as inventorySlots. It stays a List (released ports
	// read it so); the list is a NonNullList as in 1.21, for original 1.21.1 jars (VanillaRedirects.inventorySlots)
	public final List<net.minecraft.inventory.Slot> slots = this.shareSlots();

	private NonNullList<net.minecraft.inventory.Slot> shareSlots() {
		NonNullList<net.minecraft.inventory.Slot> list = NonNullList.create();
		list.addAll(this.inventorySlots);
		this.inventorySlots = list;
		return list;
	}

	protected net.minecraft.inventory.Slot addSlot(net.minecraft.inventory.Slot slot) {
		return this.addSlotToContainer(slot);
	}

	protected boolean moveItemStackTo(net.minecraft.item.ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
		return this.mergeItemStack(stack, startIndex, endIndex, reverseDirection);
	}

	public void broadcastChanges() {
		this.detectAndSendChanges();
	}

	public net.minecraft.inventory.Slot getSlot(int index) {
		return this.inventorySlots.get(index);
	}

	// --- the player and the carried stack ------------------------------------------------------------------------------

	void setPlayer(EntityPlayer player) {
		this.player = player;
	}

	@Nullable
	private EntityPlayer player() {
		if (this.player == null) {
			for (IContainerListener listener : this.listeners) {
				if (listener instanceof EntityPlayer listening) {
					this.player = listening;
				}
			}
		}
		return this.player;
	}

	public ItemStack getCarried() {
		EntityPlayer user = this.player();
		return user == null ? ItemStack.EMPTY : user.inventory.getItemStack();
	}

	public void setCarried(ItemStack stack) {
		EntityPlayer user = this.player();
		if (user != null) {
			user.inventory.setItemStack(stack);
		}
	}

	// --- clicks: 1.21 clicked(..) returns nothing, 1.12.2 slotClick(..) returns the clicked stack ----------------------

	public void clicked(int slotId, int button, ClickType clickType, EntityPlayer player) {
		this.clickResult = super.slotClick(slotId, button, clickType, player);
	}

	@Override
	public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
		this.player = player;
		this.clickResult = ItemStack.EMPTY;
		this.clicked(slotId, dragType, clickType, player);
		return this.clickResult;
	}

	/** 1.21 {@code stillValid(access, player, block)}: the block is still there and the player is near it. */
	protected static boolean stillValid(ContainerLevelAccess access, EntityPlayer player, Block block) {
		return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() == block
				&& player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D, true);
	}

	protected void addDataSlots(ContainerData data) {
		this.dataSlots.add(data);
		this.lastData.add(new int[data.getCount()]);
	}

	@Override
	public void addListener(IContainerListener listener) {
		if (listener instanceof EntityPlayer listening) {
			this.player = listening;
		}
		super.addListener(listener);
		this.forEachDataSlot((index, value) -> listener.sendWindowProperty(this, index, value));
	}

	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();

		int index = 0;
		for (int d = 0; d < this.dataSlots.size(); d++) {
			var data = this.dataSlots.get(d);
			var last = this.lastData.get(d);
			for (int i = 0; i < data.getCount(); i++, index++) {
				int value = data.get(i);
				if (value != last[i]) {
					last[i] = value;
					for (var listener : this.listeners) {
						listener.sendWindowProperty(this, index, value);
					}
				}
			}
		}
	}

	@Override
	public void updateProgressBar(int id, int value) {
		int index = id;
		for (var data : this.dataSlots) {
			if (index < data.getCount()) {
				data.set(index, value);
				return;
			}

			index -= data.getCount();
		}
	}

	private void forEachDataSlot(DataSlotConsumer consumer) {
		int index = 0;
		for (var data : this.dataSlots) {
			for (int i = 0; i < data.getCount(); i++, index++) {
				consumer.accept(index, data.get(i));
			}
		}
	}

	@FunctionalInterface
	private interface DataSlotConsumer {
		void accept(int index, int value);
	}
}
