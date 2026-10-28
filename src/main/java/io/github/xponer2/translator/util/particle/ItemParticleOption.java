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
