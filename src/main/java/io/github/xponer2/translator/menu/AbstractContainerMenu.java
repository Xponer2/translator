package io.github.xponer2.translator.menu;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;

/**
 * 1.21 container on the 1.12.2 Container: keeps its menu type and window id, and syncs its {@link ContainerData}
 * to the client as window properties (1.21 data slots).
 */
public abstract class AbstractContainerMenu extends Container {
	@Nullable
	private final MenuType<?> menuType;
	private final List<ContainerData> dataSlots = new ArrayList<>();
	private final List<int[]> lastData = new ArrayList<>();

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

	// 1.21 names of the slot helpers; `slots` is the same list as inventorySlots
	public final List<net.minecraft.inventory.Slot> slots = this.inventorySlots;

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

	protected void addDataSlots(ContainerData data) {
		this.dataSlots.add(data);
		this.lastData.add(new int[data.getCount()]);
	}

	@Override
	public void addListener(IContainerListener listener) {
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
