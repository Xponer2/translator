/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * 1.20.5+ block and item tags 1.20.4 hasn't (F rows), their contents in the adapters' data (data/minecraft/tags, as
 * 1.20.6's and 1.21's vanilla data lists them, the elements 1.20.4 has). A tag 1.21 reads in its own code (the
 * enchantable ones: an enchantment's items) is data only here.
 */
public final class TagAdapters {
    private TagAdapters() {
    }

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation(path));
    }

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(path));
    }

    public static final TagKey<Block> INCORRECT_FOR_WOODEN_TOOL = block("incorrect_for_wooden_tool");
    public static final TagKey<Block> INCORRECT_FOR_STONE_TOOL = block("incorrect_for_stone_tool");
    public static final TagKey<Block> INCORRECT_FOR_IRON_TOOL = block("incorrect_for_iron_tool");
    public static final TagKey<Block> INCORRECT_FOR_GOLD_TOOL = block("incorrect_for_gold_tool");
    public static final TagKey<Block> INCORRECT_FOR_DIAMOND_TOOL = block("incorrect_for_diamond_tool");
    public static final TagKey<Block> INCORRECT_FOR_NETHERITE_TOOL = block("incorrect_for_netherite_tool");
    private static final TagKey<Block> DOES_NOT_BLOCK_HOPPERS = block("does_not_block_hoppers");
    private static final TagKey<Item> SWORD_ENCHANTABLE = item("enchantable/sword");
    private static final TagKey<Item> WEAPON_ENCHANTABLE = item("enchantable/weapon");
    private static final TagKey<Item> SHARP_WEAPON_ENCHANTABLE = item("enchantable/sharp_weapon");

    public static TagKey<Block> incorrectForWoodenTool() {
        return INCORRECT_FOR_WOODEN_TOOL;
    }

    public static TagKey<Block> incorrectForStoneTool() {
        return INCORRECT_FOR_STONE_TOOL;
    }

    public static TagKey<Block> incorrectForIronTool() {
        return INCORRECT_FOR_IRON_TOOL;
    }

    public static TagKey<Block> incorrectForGoldTool() {
        return INCORRECT_FOR_GOLD_TOOL;
    }

    public static TagKey<Block> incorrectForDiamondTool() {
        return INCORRECT_FOR_DIAMOND_TOOL;
    }

    public static TagKey<Block> incorrectForNetheriteTool() {
        return INCORRECT_FOR_NETHERITE_TOOL;
    }

    public static TagKey<Block> doesNotBlockHoppers() {
        return DOES_NOT_BLOCK_HOPPERS;
    }

    public static TagKey<Item> swordEnchantable() {
        return SWORD_ENCHANTABLE;
    }

    public static TagKey<Item> weaponEnchantable() {
        return WEAPON_ENCHANTABLE;
    }

    public static TagKey<Item> sharpWeaponEnchantable() {
        return SHARP_WEAPON_ENCHANTABLE;
    }
}
