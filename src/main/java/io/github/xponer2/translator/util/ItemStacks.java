/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
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
