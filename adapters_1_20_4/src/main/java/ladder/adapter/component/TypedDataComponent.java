/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.component;

import java.util.Map;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

/** 1.20.5's {@code TypedDataComponent}: a component type with its value. */
public record TypedDataComponent<T>(DataComponentType<T> type, T value) {
    @SuppressWarnings("unchecked")
    public static <T> TypedDataComponent<T> createUnchecked(DataComponentType<T> type, Object value) {
        return new TypedDataComponent<>(type, (T) value);
    }

    @SuppressWarnings("unchecked")
    public static TypedDataComponent<?> fromEntryUnchecked(Map.Entry<? extends DataComponentType<?>, Object> entry) {
        return createUnchecked((DataComponentType<Object>) entry.getKey(), entry.getValue());
    }

    public <D> DataResult<D> encodeValue(DynamicOps<D> ops) {
        return this.type.codecOrThrow().encodeStart(ops, this.value);
    }

    public void applyTo(ladder.carried.net.minecraft.core.component.PatchedDataComponentMap map) {
        map.set(this.type, this.value);
    }
}
