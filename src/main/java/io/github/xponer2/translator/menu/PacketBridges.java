/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.menu;

import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.network.play.server.SPacketWindowItems;
import net.minecraft.util.NonNullList;

/**
 * The 1.21 container packets a mod makes itself (a synchronizer of its own: Sophisticated Core's), as 1.12.2's
 * (runtime, their missing constructors come here). 1.12.2's have no state id; its window items packet has no
 * carried stack (a mod sends that one as the slot -1 packet too).
 */
public final class PacketBridges {
	private PacketBridges() {
	}

	/** {@code new ClientboundContainerSetSlotPacket(containerId, stateId, slot, stack)}. */
	public static SPacketSetSlot newSPacketSetSlot(int containerId, int stateId, int slot, ItemStack stack) {
		return new SPacketSetSlot(containerId, slot, stack);
	}

	/** {@code new ClientboundContainerSetContentPacket(containerId, stateId, items, carried)}. */
	public static SPacketWindowItems newSPacketWindowItems(int containerId, int stateId, NonNullList<ItemStack> items, ItemStack carried) {
		return new SPacketWindowItems(containerId, items);
	}
}
