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

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.network.play.server.SPacketWindowItems;
import net.minecraft.network.play.server.SPacketWindowProperty;
import net.minecraft.util.NonNullList;

/** A server player's synchronizer (1.21 {@code ServerPlayer.containerSynchronizer}): 1.12.2's container packets. */
public final class PlayerSynchronizer implements ContainerSynchronizer {
	private final EntityPlayerMP player;

	public PlayerSynchronizer(EntityPlayerMP player) {
		this.player = player;
	}

	@Override
	public void sendInitialData(AbstractContainerMenu menu, NonNullList<ItemStack> items, ItemStack carried, int[] data) {
		if (this.player.connection == null) {
			return;
		}
		this.player.connection.sendPacket(new SPacketWindowItems(menu.windowId, items));
		this.player.connection.sendPacket(new SPacketSetSlot(-1, -1, carried));
		for (int i = 0; i < data.length; i++) {
			this.player.connection.sendPacket(new SPacketWindowProperty(menu.windowId, i, data[i]));
		}
	}

	@Override
	public void sendSlotChange(AbstractContainerMenu menu, int slot, ItemStack stack) {
		if (this.player.connection != null) {
			this.player.connection.sendPacket(new SPacketSetSlot(menu.windowId, slot, stack));
		}
	}

	@Override
	public void sendCarriedChange(AbstractContainerMenu menu, ItemStack stack) {
		if (this.player.connection != null) {
			this.player.connection.sendPacket(new SPacketSetSlot(-1, -1, stack));
		}
	}

	@Override
	public void sendDataChange(AbstractContainerMenu menu, int id, int value) {
		if (this.player.connection != null) {
			this.player.connection.sendPacket(new SPacketWindowProperty(menu.windowId, id, value));
		}
	}
}
