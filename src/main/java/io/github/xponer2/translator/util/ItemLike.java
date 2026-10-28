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
}
