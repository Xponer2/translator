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
package io.github.xponer2.translator.util;

import net.minecraft.item.Item;

/**
 * Something that has an item (1.21 {@code ItemLike}). Every 1.12.2 item and block is one (the coremod adds the
 * interface to Item and Block): an item is its own item, a block's is its item block.
 */
public interface ItemLike {
    default Item asItem() {
        if (this instanceof net.minecraft.block.Block block) {
            return Item.getItemFromBlock(block);
        }
        return (Item) this;
    }

    /**
     * The item of anything a 1.21 call passes as an ItemLike (the converted calls on ItemLike go through here: also
     * safe for a 1.12.2 class the coremod didn't reach).
     */
    static Item item(Object value) {
        if (value instanceof Item item) {
            return item;
        }
        if (value instanceof net.minecraft.block.Block block) {
            return Item.getItemFromBlock(block);
        }
        return value instanceof ItemLike like ? like.asItem() : net.minecraft.init.Items.AIR;
    }
}
