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

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

import java.util.stream.Stream;

/**
 * An ingredient with its own logic (NeoForge {@code ICustomIngredient}). {@link #toVanilla()} wraps it in a 1.12.2
 * {@link Ingredient} ({@link CustomIngredient}), which is what recipes hold.
 */
public interface ICustomIngredient {
    boolean test(ItemStack stack);

    Stream<ItemStack> getItems();

    boolean isSimple();

    IngredientType<?> getType();

    default Ingredient toVanilla() {
        return new CustomIngredient(this);
    }
}
