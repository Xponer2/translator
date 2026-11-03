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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.text.ITextComponent;

/** 1.21 {@code SimpleMenuProvider}: a menu provider from a constructor and a title. */
public final class SimpleMenuProvider implements MenuProvider {
	private final MenuConstructor menuConstructor;
	private final ITextComponent title;

	public SimpleMenuProvider(MenuConstructor menuConstructor, ITextComponent title) {
		this.menuConstructor = menuConstructor;
		this.title = title;
	}

	@Override
	public ITextComponent getDisplayName() {
		return this.title;
	}

	@Override
	public Container createMenu(int windowId, InventoryPlayer playerInventory, EntityPlayer player) {
		return this.menuConstructor.createMenu(windowId, playerInventory, player);
	}
}
