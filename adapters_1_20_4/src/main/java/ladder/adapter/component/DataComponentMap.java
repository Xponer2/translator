/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/** 1.20.5's {@code DataComponentMap}: a read-only view of components (a stack's, made from its data when asked). */
public interface DataComponentMap extends Iterable<TypedDataComponent<?>> {
    DataComponentMap EMPTY = new Snapshot(Map.of());

    /** 1.20.5's {@code DataComponentMap.builder()}: the carried builder (its constructor is package-private there) */
    static ladder.carried.net.minecraft.core.component.DataComponentMap$Builder builder() {
        try {
            java.lang.reflect.Constructor<ladder.carried.net.minecraft.core.component.DataComponentMap$Builder> constructor =
                    ladder.carried.net.minecraft.core.component.DataComponentMap$Builder.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("the carried DataComponentMap.Builder can't be made", e);
        }
    }

    @Nullable
    <T> T get(DataComponentType<? extends T> type);

    Set<DataComponentType<?>> keySet();

    /** 1.20.5's {@code getTyped(type)}: the type with its value, or null */
    @Nullable
    default <T> TypedDataComponent<T> getTyped(DataComponentType<T> type) {
        T value = this.get(type);
        return value == null ? null : new TypedDataComponent<>(type, value);
    }

    default boolean has(DataComponentType<?> type) {
        return this.get(type) != null;
    }

    default <T> T getOrDefault(DataComponentType<? extends T> type, T fallback) {
        T value = this.get(type);
        return value != null ? value : fallback;
    }

    default int size() {
        return this.keySet().size();
    }

    default boolean isEmpty() {
        return this.size() == 0;
    }

    /** the components as they were when the map was made */
    record Snapshot(Map<DataComponentType<?>, Object> values) implements DataComponentMap {
        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(DataComponentType<? extends T> type) {
            return (T) this.values.get(type);
        }

        @Override
        public Set<DataComponentType<?>> keySet() {
            return this.values.keySet();
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Iterator<TypedDataComponent<?>> iterator() {
            List<TypedDataComponent<?>> all = new ArrayList<>();
            this.values.forEach((type, value) -> all.add(new TypedDataComponent(type, value)));
            return all.iterator();
        }
    }
}
