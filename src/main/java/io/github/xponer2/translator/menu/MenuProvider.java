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

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.ITextComponent;

/**
 * 1.21 source of a menu (usually a tile entity), opened with {@link Menus#openMenu}.
 */
public interface MenuProvider {
	/**
	 * The menu title. Tile entities and entities implement it by overriding their vanilla getDisplayName, which the
	 * release jars rename to its SRG name: this default reaches that override there (in the development
	 * environment the class method wins and this isn't called). Other providers override it.
	 */
	@Nullable
	default ITextComponent getDisplayName() {
		if (this instanceof TileEntity tile) {
			return tile.getDisplayName();
		}
		if (this instanceof Entity entity) {
			return entity.getDisplayName();
		}
		throw new AbstractMethodError(this.getClass().getName() + " doesn't implement MenuProvider#getDisplayName");
	}

	@Nullable
	Container createMenu(int windowId, InventoryPlayer playerInventory, EntityPlayer player);
}
