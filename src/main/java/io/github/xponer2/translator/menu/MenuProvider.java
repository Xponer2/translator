package io.github.xponer2.translator.menu;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.text.ITextComponent;

/**
 * 1.21 source of a menu (usually a tile entity), opened with {@link Menus#openMenu}.
 */
public interface MenuProvider {
	@Nullable
	ITextComponent getDisplayName();

	@Nullable
	Container createMenu(int windowId, InventoryPlayer playerInventory, EntityPlayer player);
}
