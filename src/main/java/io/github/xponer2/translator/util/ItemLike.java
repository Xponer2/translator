/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
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
    /** The 1.12.2 item a block without an ItemBlock stands for: its pick item, else the item of its name. */
    static Item itemOfBlock(net.minecraft.block.Block block) {
        try {
            var picked = block.getItem(null, net.minecraft.util.math.BlockPos.ORIGIN, block.getDefaultState());
            if (picked != null && !picked.isEmpty()) {
                return picked.getItem();
            }
        } catch (RuntimeException e) {
            // a pick item that needs the world: the item of the block's name
        }
        Item named = block.getRegistryName() == null ? null : Item.getByNameOrId(block.getRegistryName().toString());
        return named != null ? named : net.minecraft.init.Items.AIR;
    }

    static Item item(Object value) {
        if (value instanceof Item item) {
            return item;
        }
        if (value instanceof io.github.xponer2.translator.runtime.redirect.AbsentBlock absent) {
            // a 1.21 vanilla block 1.12.2 lacks (the stonecutter): its placeholder item, not air. JEI's stonecutting
            // category drew Blocks.STONECUTTER: air threw, JEI's start failed and ran again at every screen opened
            Item placeholder = Item.getByNameOrId(absent.name());
            return placeholder != null ? placeholder : net.minecraft.init.Items.AIR;
        }
        if (value instanceof net.minecraft.block.Block block) {
            Item item = Item.getItemFromBlock(block);
            if (item == net.minecraft.init.Items.AIR && block != net.minecraft.init.Blocks.AIR) {
                // 1.12.2 blocks whose item is an item of its own (the brewing stand, cauldron, flower pot, skull, cake,
                // doors, beds, signs, repeater, comparator): that item, as 1.21's block.asItem() gives (JEI's brewing
                // category drew Blocks.BREWING_STAND: air threw and JEI's start failed)
                item = itemOfBlock(block);
            }
            return item;
        }
        return value instanceof ItemLike like ? like.asItem() : net.minecraft.init.Items.AIR;
    }
}
