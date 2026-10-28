package io.github.xponer2.translator.util.particle;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;

/**
 * 1.21 item particle: 1.12.2 ITEM_CRACK with the item id and meta as arguments.
 */
public class ItemParticleOption extends ParticleOptions {
	public ItemParticleOption(EnumParticleTypes type, ItemStack stack) {
		super(type, Item.getIdFromItem(stack.getItem()), stack.getMetadata());
	}
}
