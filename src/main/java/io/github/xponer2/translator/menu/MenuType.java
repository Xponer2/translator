package io.github.xponer2.translator.menu;

import javax.annotation.Nullable;

import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;
import io.github.xponer2.translator.util.BuiltInRegistries;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;

/**
 * 1.21 menu type: creates the client side container of a menu the server opened (see {@link Menus}), with the
 * extra data the server wrote when the factory is an {@link IContainerFactory}. Registered in Registries.MENU.
 */
public class MenuType<T extends Container> {
	private final MenuSupplier<T> constructor;

	public MenuType(MenuSupplier<T> constructor, Object requiredFeatures) {
		this.constructor = constructor;
	}

	public T create(int windowId, InventoryPlayer playerInventory) {
		return this.constructor.create(windowId, playerInventory);
	}

	@SuppressWarnings("unchecked")
	public T create(int windowId, InventoryPlayer playerInventory, @Nullable RegistryFriendlyByteBuf data) {
		if (this.constructor instanceof IContainerFactory) {
			return ((IContainerFactory<T>) this.constructor).create(windowId, playerInventory, data);
		}

		return this.constructor.create(windowId, playerInventory);
	}

	@Nullable
	public ResourceLocation getId() {
		return BuiltInRegistries.MENU.getKey(this);
	}

	@FunctionalInterface
	public interface MenuSupplier<T extends Container> {
		T create(int windowId, InventoryPlayer playerInventory);
	}
}
