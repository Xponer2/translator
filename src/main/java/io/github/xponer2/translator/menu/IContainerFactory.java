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
