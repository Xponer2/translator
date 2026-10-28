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
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Item ids of 1.21 data and codecs to 1.12.2 stacks and back. A flattened vanilla id (1.21 {@code minecraft:white_wool})
 * is an item with a meta value on 1.12.2 ({@code minecraft:wool} meta 0): those are registered with
 * {@link #registerFlattened} (by the tag loader's table); every other id is the item of that registry name, meta 0.
 */
public final class ItemIds {
    private static final Map<ResourceLocation, ItemStack> FLATTENED = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> FLATTENED_IDS = new ConcurrentHashMap<>();
    // vanilla content added after 1.12.2 -> something similar, for recipe results only (no way back)
    private static final Map<ResourceLocation, ItemStack> SIMILAR = new ConcurrentHashMap<>();

    private ItemIds() { }

    public static void registerFlattened(ResourceLocation id, Item item, int meta) {
        FLATTENED.put(id, new ItemStack(item, 1, meta));
        FLATTENED_IDS.put(key(item, meta), id);
    }

    public static void registerSimilar(ResourceLocation id, Item item, int meta) {
        SIMILAR.put(id, new ItemStack(item, 1, meta));
    }

    /**
     * A stack of the item of an id for a result (recipe outputs, displays): like {@link #stack}, but vanilla content
     * 1.12.2 doesn't have becomes a similar item.
     */
    public static ItemStack resultStack(ResourceLocation id, int count) {
        var stack = stack(id, count);
        var similar = SIMILAR.get(id);
        if (stack.isEmpty() && similar != null) {
            stack = similar.copy();
            stack.setCount(count);
        }
        return stack;
    }

    /**
     * A stack of the item of an id, empty if there is no such item.
     */
    public static ItemStack stack(ResourceLocation id, int count) {
        var flattened = FLATTENED.get(id);
        if (flattened != null) {
            var stack = flattened.copy();
            stack.setCount(count);
            return stack;
        }
        var item = item(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    /**
     * The item of an id (for a flattened id the 1.12.2 item, without its meta value), null if there is none.
     */
    @Nullable
    public static Item item(ResourceLocation id) {
        var flattened = FLATTENED.get(id);
        if (flattened != null) {
            return flattened.getItem();
        }
        return ForgeRegistries.ITEMS.containsKey(id) ? ForgeRegistries.ITEMS.getValue(id) : null;
    }

    /**
     * The 1.21 id of a stack: the flattened id of its item and meta, else the item's registry name.
     */
    @Nullable
    public static ResourceLocation id(ItemStack stack) {
        var flattened = FLATTENED_IDS.get(key(stack.getItem(), stack.getMetadata()));
        return flattened != null ? flattened : stack.getItem().getRegistryName();
    }

    private static String key(Item item, int meta) {
        return item.getRegistryName() + "@" + meta;
    }
}
