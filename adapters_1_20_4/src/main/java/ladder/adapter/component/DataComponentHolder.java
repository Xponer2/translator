/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
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
