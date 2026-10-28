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
