package io.github.xponer2.translator.component;

import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Keeps the value of a component type on a stack where 1.12.2 keeps that data (e.g. the "Unbreakable" tag) instead
 * of the components tag of {@link StackComponents}.
 */
public interface StackStorage<T> {
    /**
     * The value on the stack, null if it has none (the item's default applies then).
     */
    @Nullable
    T get(ItemStack stack);

    /**
     * Sets the value; null removes it.
     */
    void set(ItemStack stack, @Nullable T value);
}
