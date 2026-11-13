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

	private int stateId;

	/** 1.21 {@code getStateId()}: the version of the menu's contents the client last saw (sent with a click). */
	public int getStateId() {
		return this.stateId;
	}

	public int incrementStateId() {
		this.stateId = this.stateId + 1 & 32767;
		return this.stateId;
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

	// --- 1.21 sync: what the client has (remote*), sent through the player's synchronizer -------------------------------
	// (1.12.2's own sync writes a stack's count as a byte: 256 items came as none; a mod's synchronizer sends more)

	/** The stacks the client has, by slot (1.21 {@code remoteSlots}). */
	public final NonNullList<ItemStack> remoteSlots = NonNullList.create();
	/** The carried stack the client has. */
	public ItemStack remoteCarried = ItemStack.EMPTY;
	/** What sends the changes to the player's client; null without a player (the client's own menu). */
	@Nullable
	public ContainerSynchronizer synchronizer;
	/** While set, nothing is sent (1.21 {@code suppressRemoteUpdates()}: a mod changes slots it sends itself). */
	public boolean suppressRemoteUpdates;
	/** 1.12.2 sent the whole menu with its own packets (a click the client saw otherwise): ours again after it. */
	private boolean resendAll;

	@Override
	protected net.minecraft.inventory.Slot addSlotToContainer(net.minecraft.inventory.Slot slot) {
		net.minecraft.inventory.Slot added = super.addSlotToContainer(slot);
		this.remoteSlots.add(ItemStack.EMPTY);
		return added;
	}

	/** 1.21 {@code setSynchronizer}: the menu's player sends with it from now on; all of the menu first. */
	public void setSynchronizer(ContainerSynchronizer synchronizer) {
		this.synchronizer = synchronizer;
		this.sendAllDataToRemote();
	}

	/** 1.21 {@code sendAllDataToRemote}: every slot, the carried stack and the data slots, to the client. */
	public void sendAllDataToRemote() {
		for (int i = 0; i < this.inventorySlots.size() && i < this.remoteSlots.size(); i++) {
			this.remoteSlots.set(i, this.inventorySlots.get(i).getStack().copy());
		}
		this.remoteCarried = this.getCarried().copy();
		int[] data = this.dataValues();
		this.rememberData(data);
		if (this.synchronizer != null) {
			this.synchronizer.sendInitialData(this, this.remoteSlots, this.remoteCarried, data);
		}
	}

	/** 1.21 {@code broadcastFullState}: the listeners' and the client's copies made anew. */
	public void broadcastFullState() {
		for (int i = 0; i < this.inventorySlots.size(); i++) {
			ItemStack stack = this.inventorySlots.get(i).getStack();
			this.triggerSlotListeners(i, stack, memoize(stack));
		}
		this.sendAllDataToRemote();
	}

	/**
	 * 1.21 {@code broadcastChanges}: each slot that changed to the listeners and the client, the carried stack, the data
	 * slots. 1.12.2 calls it every tick ({@link #detectAndSendChanges}); a mod's menu may do it its own way
	 * (Sophisticated Core: its upgrade slots, stacks of thousands).
	 */
	public void broadcastChanges() {
		for (int i = 0; i < this.inventorySlots.size(); i++) {
			ItemStack stack = this.inventorySlots.get(i).getStack();
			java.util.function.Supplier<ItemStack> copy = memoize(stack);
			this.triggerSlotListeners(i, stack, copy);
			this.synchronizeSlotToRemote(i, stack, copy);
		}
		this.synchronizeCarriedToRemote();
		this.broadcastData();
	}

	private static java.util.function.Supplier<ItemStack> memoize(ItemStack stack) {
		ItemStack[] copy = new ItemStack[1];
		return () -> {
			if (copy[0] == null) {
				copy[0] = stack.copy();
			}
			return copy[0];
		};
	}

	/** 1.12.2's tick sync: the 1.21 one ({@link #broadcastChanges}, a mod's own if it has one). */
	@Override
	public void detectAndSendChanges() {
		if (this.resendAll) {
			this.resendAll = false;
			this.sendAllDataToRemote();
		}
		this.broadcastChanges();
	}

	/**
	 * A click the server saw otherwise than the client: the whole menu is sent again, as 1.21 does by its state id.
	 * 1.12.2 also stopped taking the player's clicks until the client confirmed (its transaction lock), which 1.21 has
	 * not: a click in between was dropped without a word (Macaw's drawers, Iron's scroll forge, Supplementaries'
	 * presents, the cooking pot: the client held the item, the server nothing). The menu keeps taking clicks.
	 */
	@Override
	public void setCanCraft(EntityPlayer player, boolean canCraft) {
		super.setCanCraft(player, true);
		if (!canCraft) {
			this.resendAll = true;
		}
	}

	/** 1.21 {@code triggerSlotListeners}: a changed slot to the menu's listeners (not the player: its synchronizer sends). */
	protected void triggerSlotListeners(int slot, ItemStack stack, java.util.function.Supplier<ItemStack> copy) {
		if (slot >= this.inventoryItemStacks.size()) {
			return;
		}
		ItemStack last = this.inventoryItemStacks.get(slot);
		if (!ItemStack.areItemStacksEqual(last, stack)) {
			ItemStack now = copy.get();
			this.inventoryItemStacks.set(slot, now);
			for (IContainerListener listener : this.listeners) {
				ListenerBridges.slotChanged(listener, this, slot, now);
			}
		}
	}

	/** 1.21 {@code synchronizeSlotToRemote}: a slot the client has otherwise, sent. */
	protected void synchronizeSlotToRemote(int slot, ItemStack stack, java.util.function.Supplier<ItemStack> copy) {
		if (this.suppressRemoteUpdates || slot >= this.remoteSlots.size()) {
			return;
		}
		ItemStack remote = this.remoteSlots.get(slot);
		if (!ItemStack.areItemStacksEqual(remote, stack)) {
			ItemStack now = copy.get();
			this.remoteSlots.set(slot, now);
			if (this.synchronizer != null) {
				this.synchronizer.sendSlotChange(this, slot, now);
			}
		}
	}

	/** 1.21 {@code synchronizeCarriedToRemote}: the carried stack, when the client has another. */
	protected void synchronizeCarriedToRemote() {
		if (this.suppressRemoteUpdates) {
			return;
		}
		ItemStack carried = this.getCarried();
		if (!ItemStack.areItemStacksEqual(carried, this.remoteCarried)) {
			this.remoteCarried = carried.copy();
			if (this.synchronizer != null) {
				this.synchronizer.sendCarriedChange(this, this.remoteCarried);
			}
		}
	}

	public void setRemoteSlot(int slot, ItemStack stack) {
		this.setRemoteSlotNoCopy(slot, stack.copy());
	}

	public void setRemoteSlotNoCopy(int slot, ItemStack stack) {
		if (slot >= 0 && slot < this.remoteSlots.size()) {
			this.remoteSlots.set(slot, stack);
		}
	}

	public void setRemoteCarried(ItemStack stack) {
		this.remoteCarried = stack.copy();
	}

	public void suppressRemoteUpdates() {
		this.suppressRemoteUpdates = true;
	}

	public void resumeRemoteUpdates() {
		this.suppressRemoteUpdates = false;
	}

	// --- the client side: 1.12.2's packets set the slots through the 1.21 methods (a mod's menu overrides them) --------

	/** 1.21 {@code setItem(slot, stateId, stack)}: a slot the server sent. */
	public void setItem(int slot, int stateId, ItemStack stack) {
		this.getSlot(slot).putStack(stack);
		this.stateId = stateId;
	}

	@Override
	public void putStackInSlot(int slot, ItemStack stack) {
		this.setItem(slot, this.stateId, stack);
	}

	/** 1.21 {@code initializeContents(stateId, items, carried)}: the whole menu the server sent. */
	public void initializeContents(int stateId, List<ItemStack> items, ItemStack carried) {
		for (int i = 0; i < items.size() && i < this.inventorySlots.size(); i++) {
			this.getSlot(i).putStack(items.get(i));
		}
		if (this.player != null) {
			this.setCarried(carried);
		}
		this.stateId = stateId;
	}

	@Override
	public void setAll(List<ItemStack> items) {
		this.initializeContents(this.stateId, items, this.getCarried());
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
		this.doClick(slotId, button, clickType, player);
	}

	/** 1.21 {@code createCarriedSlotAccess}: the carried stack as a slot (an item clicked onto another takes it from there). */
	protected SlotAccess createCarriedSlotAccess() {
		return SlotAccess.of(this::getCarried, this::setCarried);
	}

	/** 1.21 {@code doClick}: the click itself, 1.12.2's (a mod's menu may have its own: Sophisticated Core). */
	protected void doClick(int slotId, int button, ClickType clickType, EntityPlayer player) {
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

	/**
	 * 1.21 {@code addDataSlot(slot)}: one synced int. DataSlot is the carried 1.21 class (its get() and set(int) are read
	 * by name); Iron Furnaces syncs its furnace's progress with them.
	 */
	/** 1.21 {@code checkContainerSize(container, size)}: a container smaller than the menu needs is an error. */
	protected static void checkContainerSize(net.minecraft.inventory.IInventory container, int size) {
		if (container.getSizeInventory() < size) {
			throw new IllegalArgumentException("Container size " + container.getSizeInventory() + " is smaller than expected " + size);
		}
	}

	/** 1.21 {@code checkContainerDataCount(data, count)}: fewer synced values than the menu needs is an error (Aether's furnaces). */
	protected static void checkContainerDataCount(ContainerData data, int count) {
		if (data.getCount() < count) {
			throw new IllegalArgumentException("Container data count " + data.getCount() + " is smaller than expected " + count);
		}
	}

	/** 1.21 {@code clearContainer(player, container)}: the items back to the player (1.12.2's takes the world too). */
	protected void clearContainer(EntityPlayer player, net.minecraft.inventory.IInventory container) {
		this.clearContainer(player, player.world, container);
	}

	protected <T> T addDataSlot(T slot) {
		java.lang.reflect.Method get;
		java.lang.reflect.Method set;
		try {
			get = slot.getClass().getMethod("get");
			set = slot.getClass().getMethod("set", int.class);
			get.setAccessible(true);
			set.setAccessible(true);
		} catch (NoSuchMethodException | RuntimeException e) {
			return slot;
		}
		this.addDataSlots(new ContainerData() {
			@Override
			public int get(int index) {
				try {
					return (Integer) get.invoke(slot);
				} catch (ReflectiveOperationException e) {
					return 0;
				}
			}

			@Override
			public void set(int index, int value) {
				try {
					set.invoke(slot, value);
				} catch (ReflectiveOperationException e) {
					// the slot's own error: the value stays
				}
			}

			@Override
			public int getCount() {
				return 1;
			}
		});
		return slot;
	}

	protected void addDataSlots(ContainerData data) {
		this.dataSlots.add(data);
		this.lastData.add(new int[data.getCount()]);
	}

	/**
	 * A server player gets the menu through a synchronizer (1.21 {@code ServerPlayer.initMenu}: the whole menu, then the
	 * changes), not 1.12.2's container packets; another listener as in 1.12.2.
	 */
	@Override
	public void addListener(IContainerListener listener) {
		if (listener instanceof net.minecraft.entity.player.EntityPlayerMP serverPlayer) {
			if (this.listeners.contains(listener)) {
				throw new IllegalArgumentException("Listener already listening");
			}
			this.player = serverPlayer;
			this.listeners.add(listener);
			this.setSynchronizer(new PlayerSynchronizer(serverPlayer));
			return;
		}
		if (listener instanceof EntityPlayer listening) {
			this.player = listening;
		}
		super.addListener(listener);
		this.forEachDataSlot((index, value) -> listener.sendWindowProperty(this, index, value));
	}

	/** The data slots' values that changed: to the client (its synchronizer) and the other listeners. */
	private void broadcastData() {
		int index = 0;
		for (int d = 0; d < this.dataSlots.size(); d++) {
			var data = this.dataSlots.get(d);
			var last = this.lastData.get(d);
			for (int i = 0; i < data.getCount(); i++, index++) {
				int value = data.get(i);
				if (value != last[i]) {
					last[i] = value;
					if (this.synchronizer != null) {
						this.synchronizer.sendDataChange(this, index, value);
					}
					for (var listener : this.listeners) {
						ListenerBridges.dataChanged(listener, this, index, value);
					}
				}
			}
		}
	}

	private int[] dataValues() {
		int count = 0;
		for (var data : this.dataSlots) {
			count += data.getCount();
		}
		int[] values = new int[count];
		this.forEachDataSlot((index, value) -> values[index] = value);
		return values;
	}

	private void rememberData(int[] values) {
		int index = 0;
		for (int d = 0; d < this.dataSlots.size(); d++) {
			var last = this.lastData.get(d);
			for (int i = 0; i < last.length && index < values.length; i++, index++) {
				last[i] = values[index];
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

	/**
	 * A 1.12.2 container read where 1.21 code expects a menu (player.containerMenu is the player's inventory container
	 * when nothing is open): its 1.21 view, not a failed cast (Sophisticated Backpacks' block entities checked the
	 * players' menus every tick and threw).
	 */
	@javax.annotation.Nullable
	public static AbstractContainerMenu of(@javax.annotation.Nullable net.minecraft.inventory.Container container) {
		return VanillaMenu.of(container);
	}
}
