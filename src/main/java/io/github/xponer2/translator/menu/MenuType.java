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
import io.github.xponer2.translator.util.BuiltInRegistries;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;

/**
 * 1.21 menu type: creates the client side container of a menu the server opened (see {@link Menus}), with the
 * extra data the server wrote when the factory is an {@link IContainerFactory}. Registered in Registries.MENU.
 */
public class MenuType<T extends Container> {
	// the vanilla chest menus mods open for their own containers (ChestMenu.threeRows), under their 1.21 ids
	public static final MenuType<ChestMenu> GENERIC_9x1 = vanilla("generic_9x1", ChestMenu::oneRow);
	public static final MenuType<ChestMenu> GENERIC_9x2 = vanilla("generic_9x2", ChestMenu::twoRows);
	public static final MenuType<ChestMenu> GENERIC_9x3 = vanilla("generic_9x3", ChestMenu::threeRows);
	public static final MenuType<ChestMenu> GENERIC_9x4 = vanilla("generic_9x4", ChestMenu::fourRows);
	public static final MenuType<ChestMenu> GENERIC_9x5 = vanilla("generic_9x5", ChestMenu::fiveRows);
	public static final MenuType<ChestMenu> GENERIC_9x6 = vanilla("generic_9x6", ChestMenu::sixRows);
	public static final MenuType<ShulkerBoxMenu> SHULKER_BOX = vanilla("shulker_box", ShulkerBoxMenu::new);
	// vanilla menus mods name (JEI's recipe transfer, Ars Nouveau's and Refined Storage's crafting grids): 1.12.2's containers
	public static final MenuType<net.minecraft.inventory.ContainerWorkbench> CRAFTING = vanilla("crafting", (id, inventory) -> {
		var menu = new net.minecraft.inventory.ContainerWorkbench(inventory, inventory.player.world, inventory.player.getPosition());
		menu.windowId = id;
		return menu;
	});
	public static final MenuType<net.minecraft.inventory.ContainerFurnace> FURNACE = vanilla("furnace", (id, inventory) -> {
		var menu = new net.minecraft.inventory.ContainerFurnace(inventory, new net.minecraft.tileentity.TileEntityFurnace());
		menu.windowId = id;
		return menu;
	});
	public static final MenuType<net.minecraft.inventory.ContainerBrewingStand> BREWING_STAND = vanilla("brewing_stand", (id, inventory) -> {
		var menu = new net.minecraft.inventory.ContainerBrewingStand(inventory, new net.minecraft.tileentity.TileEntityBrewingStand());
		menu.windowId = id;
		return menu;
	});
	public static final MenuType<net.minecraft.inventory.ContainerRepair> ANVIL = vanilla("anvil", (id, inventory) -> {
		var menu = new net.minecraft.inventory.ContainerRepair(inventory, inventory.player.world, inventory.player.getPosition(), inventory.player);
		menu.windowId = id;
		return menu;
	});
	// menus of blocks 1.12.2 hasn't (JEI names them for its recipe transfer): a type of their own, none is ever opened
	public static final MenuType<Container> SMOKER = vanilla("smoker", (id, inventory) -> null);
	public static final MenuType<Container> BLAST_FURNACE = vanilla("blast_furnace", (id, inventory) -> null);
	public static final MenuType<Container> SMITHING = vanilla("smithing", (id, inventory) -> null);
	public static final MenuType<Container> CRAFTER_3x3 = vanilla("crafter_3x3", (id, inventory) -> null);

	private final MenuSupplier<T> constructor;

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static <T extends Container> MenuType<T> vanilla(String name, MenuSupplier<T> constructor) {
		var type = new MenuType<>(constructor, null);
		if (BuiltInRegistries.MENU instanceof io.github.xponer2.translator.registry.MappedRegistry registry) {
			registry.register(new ResourceLocation("minecraft", name), type);
		}
		return type;
	}

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
