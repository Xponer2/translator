/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

/**
 * NeoForge 21.1's new common tags (Tags.*) on 1.21: their keys (the F rows' getters; generated from both versions'
 * Tags) and their contents (data/c/tags, from 21.1's jar).
 */
public final class NewTags {
    private NewTags() {
    }

    public static TagKey<net.minecraft.world.level.biome.Biome> biomesIsTemperate() {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("c", "is_temperate"));
    }

    public static TagKey<net.minecraft.world.level.biome.Biome> biomesIsTemperateEnd() {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("c", "is_temperate/end"));
    }

    public static TagKey<net.minecraft.world.level.biome.Biome> biomesIsTemperateNether() {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("c", "is_temperate/nether"));
    }

    public static TagKey<net.minecraft.world.level.biome.Biome> biomesIsTemperateOverworld() {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("c", "is_temperate/overworld"));
    }

    public static TagKey<net.minecraft.world.level.block.Block> blocksObsidiansCrying() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "obsidians/crying"));
    }

    public static TagKey<net.minecraft.world.level.block.Block> blocksObsidiansNormal() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "obsidians/normal"));
    }

    public static TagKey<net.minecraft.world.level.block.Block> blocksPumpkins() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "pumpkins"));
    }

    public static TagKey<net.minecraft.world.level.block.Block> blocksPumpkinsCarved() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "pumpkins/carved"));
    }

    public static TagKey<net.minecraft.world.level.block.Block> blocksPumpkinsJackOLanterns() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "pumpkins/jack_o_lanterns"));
    }

    public static TagKey<net.minecraft.world.level.block.Block> blocksPumpkinsNormal() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "pumpkins/normal"));
    }

    public static TagKey<net.minecraft.world.level.block.Block> blocksStrippedLogs() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "stripped_logs"));
    }

    public static TagKey<net.minecraft.world.level.block.Block> blocksStrippedWoods() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "stripped_woods"));
    }

    public static TagKey<net.minecraft.world.level.material.Fluid> fluidsExperience() {
        return TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "experience"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsAnimalFoods() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "animal_foods"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsCropsCactus() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "crops/cactus"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsCropsCocoaBean() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "crops/cocoa_bean"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsCropsMelon() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "crops/melon"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsCropsPumpkin() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "crops/pumpkin"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsCropsSugarCane() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "crops/sugar_cane"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsDrinks() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "drinks"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsDrinksHoney() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "drinks/honey"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsDrinksJuice() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "drinks/juice"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsDrinksMagic() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "drinks/magic"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsDrinksMilk() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "drinks/milk"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsDrinksOminous() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "drinks/ominous"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsDrinksWater() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "drinks/water"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsDrinksWatery() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "drinks/watery"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsDrinkContainingBottle() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "drink_containing/bottle"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsDrinkContainingBucket() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "drink_containing/bucket"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsFoodsDough() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "foods/dough"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsFoodsPie() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "foods/pie"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsLoomPatterns() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "loom_patterns"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsObsidiansCrying() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "obsidians/crying"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsObsidiansNormal() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "obsidians/normal"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsPotions() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "potions"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsPotionBottle() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "potions/bottle"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsPumpkins() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "pumpkins"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsPumpkinsCarved() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "pumpkins/carved"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsPumpkinsJackOLanterns() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "pumpkins/jack_o_lanterns"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsPumpkinsNormal() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "pumpkins/normal"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsSeedsTorchflower() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "seeds/torchflower"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsStrippedLogs() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "stripped_logs"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsStrippedWoods() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "stripped_woods"));
    }

    public static TagKey<net.minecraft.world.item.Item> itemsToolsWrench() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));
    }
}
