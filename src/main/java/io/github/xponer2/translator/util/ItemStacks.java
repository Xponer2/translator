package io.github.xponer2.translator.util;

import io.github.xponer2.translator.registry.Holder;
import io.github.xponer2.translator.tags.TagManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * The 1.21 {@code ItemStack#is(..)} checks (the converter rewrites {@code stack.is(item)} to these).
 */
public final class ItemStacks {
    private ItemStacks() { }

    public static boolean is(ItemStack stack, Item item) {
        return stack.getItem() == item;
    }

    public static boolean is(ItemStack stack, Holder<Item> item) {
        return stack.getItem() == item.value();
    }

    public static boolean is(ItemStack stack, TagKey<Item> tag) {
        return TagManager.is(stack, tag);
    }
}
