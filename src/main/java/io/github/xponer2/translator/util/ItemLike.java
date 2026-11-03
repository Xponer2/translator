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
 * Something that has an item (1.21 {@code ItemLike}). 1.12.2 items and blocks don't implement it; an item class that
 * does gets itself as its item.
 */
public interface ItemLike {
    default Item asItem() {
        return (Item) this;
    }

    /**
     * The item of anything a 1.21 call passes as an ItemLike: 1.12.2 items and blocks don't implement this interface
     * (an original 1.21.1 mod passes them as ItemLike anyway), so calls on it go through here.
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
