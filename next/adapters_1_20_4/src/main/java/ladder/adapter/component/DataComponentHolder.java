/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.component;

/**
 * 1.20.5's {@code DataComponentHolder}: a 1.20.4 ItemStack is one (mixin.ItemStackMixin); the converted calls of its methods go to the
 * component store with the stack as the receiver (ComponentStore), so this is only the type the calls name.
 */
public interface DataComponentHolder {
    DataComponentMap getComponents();

    @SuppressWarnings("unchecked")
    default <T> T get(DataComponentType<? extends T> type) {
        return (T) ComponentStore.get(this, type);
    }

    @SuppressWarnings("unchecked")
    default <T> T getOrDefault(DataComponentType<? extends T> type, T fallback) {
        return (T) ComponentStore.getOrDefault(this, type, fallback);
    }

    default boolean has(DataComponentType<?> type) {
        return ComponentStore.has(this, type);
    }
}
