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
