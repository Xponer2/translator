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

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;

/**
 * 1.21 {@code SimpleContainer} for the mods' own containers that extend it (an upgrade's result slot overriding
 * setChanged): 1.12.2's InventoryBasic, made with the 1.21 constructors (1.12.2's takes a name too: none could be made).
 */
public class SimpleContainer extends InventoryBasic {
	public SimpleContainer(int size) {
		super("", false, size);
	}

	public SimpleContainer(ItemStack... stacks) {
		super("", false, stacks.length);
		for (int i = 0; i < stacks.length; i++) {
			this.setInventorySlotContents(i, stacks[i]);
		}
	}

	/** The 1.12.2 listener made for each 1.21 one (to remove it again). */
	private final java.util.Map<net.minecraft.world.ContainerListener, net.minecraft.inventory.IInventoryChangedListener> listeners121 =
			new java.util.IdentityHashMap<>();

	/**
	 * 1.21 {@code addListener}: a 1.21 listener (carried) told of every change, as 1.12.2's are. Called on a subclass
	 * (Refined Storage's filters, pattern grid) it found no 1.12.2 method: the containers never told their block entities.
	 */
	public void addListener(net.minecraft.world.ContainerListener listener) {
		net.minecraft.inventory.IInventoryChangedListener wrapped = inventory -> {
			try {
				// the carried interface's containerChanged(IInventory) (its compile stub can't name it)
				java.lang.reflect.Method changed = listener.getClass().getMethod("containerChanged", net.minecraft.inventory.IInventory.class);
				changed.setAccessible(true);
				changed.invoke(listener, inventory);
			} catch (ReflectiveOperationException e) {
				io.github.xponer2.translator.runtime.RuntimeStubs.failed(listener.getClass().getName() + ".containerChanged", e);
			}
		};
		this.listeners121.put(listener, wrapped);
		this.addInventoryChangeListener(wrapped);
	}

	public void removeListener(net.minecraft.world.ContainerListener listener) {
		net.minecraft.inventory.IInventoryChangedListener wrapped = this.listeners121.remove(listener);
		if (wrapped != null) {
			this.removeInventoryChangeListener(wrapped);
		}
	}
}
