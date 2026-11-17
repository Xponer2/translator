/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.menu;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;

/** 1.21 {@code MenuConstructor}: makes the server side of a menu (see {@link SimpleMenuProvider}). */
@FunctionalInterface
public interface MenuConstructor {
	@Nullable
	AbstractContainerMenu createMenu(int windowId, InventoryPlayer playerInventory, EntityPlayer player);
}
