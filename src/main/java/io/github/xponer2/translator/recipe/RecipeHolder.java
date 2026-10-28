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
package io.github.xponer2.translator.recipe;

import net.minecraft.util.ResourceLocation;

/**
 * A loaded recipe with its id (1.21 {@code RecipeHolder}).
 */
public final class RecipeHolder<T extends Recipe<?>> {
    private final ResourceLocation id;
    private final T value;

    public RecipeHolder(ResourceLocation id, T value) {
        this.id = id;
        this.value = value;
    }

    public ResourceLocation id() {
        return this.id;
    }

    public T value() {
        return this.value;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof RecipeHolder<?> other && this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
