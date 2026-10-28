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

import javax.annotation.Nullable;

import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;

/**
 * NeoForge menu factory with the extra data the server wrote when it opened the menu (null on the server side).
 */
@FunctionalInterface
public interface IContainerFactory<T extends Container> extends MenuType.MenuSupplier<T> {
	T create(int windowId, InventoryPlayer playerInventory, @Nullable RegistryFriendlyByteBuf data);

	@Override
	default T create(int windowId, InventoryPlayer playerInventory) {
		return this.create(windowId, playerInventory, null);
	}
}
