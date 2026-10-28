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
package io.github.xponer2.translator.jei;

/**
 * A JEI recipe category id with its recipe class (JEI 1.21 {@code RecipeType}); JEI 4.16 knows categories by a
 * string uid, {@link #getUid()}.
 */
public final class RecipeType<T> {
    private final String uid;
    private final Class<? extends T> recipeClass;

    private RecipeType(String uid, Class<? extends T> recipeClass) {
        this.uid = uid;
        this.recipeClass = recipeClass;
    }

    public static <T> RecipeType<T> create(String namespace, String path, Class<? extends T> recipeClass) {
        return new RecipeType<>(namespace + ":" + path, recipeClass);
    }

    public String getUid() {
        return this.uid;
    }

    public Class<? extends T> getRecipeClass() {
        return this.recipeClass;
    }

    @Override
    public String toString() {
        return this.uid;
    }
}
