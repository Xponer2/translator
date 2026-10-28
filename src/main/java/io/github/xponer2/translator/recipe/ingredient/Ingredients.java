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
package io.github.xponer2.translator.recipe.ingredient;

import io.github.xponer2.translator.util.Codecs;
import io.github.xponer2.translator.util.TagKey;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.oredict.OreDictionary;

import java.util.stream.Stream;

/**
 * The 1.21 {@code Ingredient.of(..)} factories (the converter rewrites {@code Ingredient.of} to these). Like on 1.21
 * an item matches regardless of damage unless it has subtypes.
 */
public final class Ingredients {
    private Ingredients() { }

    public static Ingredient of() {
        return Ingredient.EMPTY;
    }

    public static Ingredient of(Item... items) {
        var stacks = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            stacks[i] = new ItemStack(items[i], 1, items[i].getHasSubtypes() ? 0 : OreDictionary.WILDCARD_VALUE);
        }
        return Ingredient.fromStacks(stacks);
    }

    public static Ingredient of(Block... blocks) {
        var items = new Item[blocks.length];
        for (int i = 0; i < blocks.length; i++) {
            items[i] = Item.getItemFromBlock(blocks[i]);
        }
        return of(items);
    }

    public static Ingredient of(ItemStack... stacks) {
        return of(Stream.of(stacks));
    }

    public static Ingredient of(Stream<ItemStack> stacks) {
        return Ingredient.fromStacks(stacks.filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toArray(ItemStack[]::new));
    }

    public static Ingredient of(TagKey<Item> tag) {
        return Codecs.tagIngredient(tag.location());
    }
}
