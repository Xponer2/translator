/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.component;

import java.util.Objects;

/**
 * A component type with its value (1.21 {@code net.minecraft.core.component.TypedDataComponent}).
 */
public final class TypedDataComponent<T> {
    private final DataComponentType<T> type;
    private final T value;

    public TypedDataComponent(DataComponentType<T> type, T value) {
        this.type = type;
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    static <T> TypedDataComponent<T> of(DataComponentType<T> type, Object value) {
        return new TypedDataComponent<>(type, (T) value);
    }

    public DataComponentType<T> type() {
        return this.type;
    }

    public T value() {
        return this.value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TypedDataComponent && ((TypedDataComponent<?>) o).type == this.type
                && Objects.equals(((TypedDataComponent<?>) o).value, this.value);
    }

    @Override
    public int hashCode() {
        return 31 * this.type.hashCode() + Objects.hashCode(this.value);
    }

    @Override
    public String toString() {
        return this.type + "=>" + this.value;
    }
}
