/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.recipe;

import net.minecraft.item.ItemStack;

/**
 * The items a recipe is matched against (1.21 {@code RecipeInput}).
 */
public interface RecipeInput {
    ItemStack getItem(int index);

    int size();

    default boolean isEmpty() {
        for (int i = 0; i < this.size(); i++) {
            if (!this.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
