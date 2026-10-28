package io.github.xponer2.translator.component;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Component values by type (1.21 {@code net.minecraft.core.component.DataComponentMap}): what a stack has
 * ({@link StackComponents#getComponents}) or what should be applied to one. Immutable.
 */
public final class DataComponentMap implements Iterable<TypedDataComponent<?>> {
    public static final DataComponentMap EMPTY = new DataComponentMap(Collections.emptyMap());

    private final Map<DataComponentType<?>, Object> values;

    DataComponentMap(Map<DataComponentType<?>, Object> values) {
        this.values = values;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T get(DataComponentType<? extends T> type) {
        return (T) this.values.get(type);
    }

    @Nullable
    public <T> T get(Supplier<? extends DataComponentType<? extends T>> type) {
        return this.get(type.get());
    }

    public <T> T getOrDefault(DataComponentType<? extends T> type, T defaultValue) {
        T value = this.get(type);
        return value != null ? value : defaultValue;
    }

    public <T> T getOrDefault(Supplier<? extends DataComponentType<? extends T>> type, T defaultValue) {
        return this.getOrDefault(type.get(), defaultValue);
    }

    public boolean has(DataComponentType<?> type) {
        return this.values.containsKey(type);
    }

    public boolean has(Supplier<? extends DataComponentType<?>> type) {
        return this.has(type.get());
    }

    public Set<DataComponentType<?>> keySet() {
        return Collections.unmodifiableSet(this.values.keySet());
    }

    public int size() {
        return this.values.size();
    }

    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    public Stream<TypedDataComponent<?>> stream() {
        return this.values.entrySet().stream().map(e -> TypedDataComponent.of(e.getKey(), e.getValue()));
    }

    @Override
    public Iterator<TypedDataComponent<?>> iterator() {
        return this.stream().iterator();
    }

    Map<DataComponentType<?>, Object> values() {
        return this.values;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DataComponentMap && ((DataComponentMap) o).values.equals(this.values);
    }

    @Override
    public int hashCode() {
        return this.values.hashCode();
    }

    @Override
    public String toString() {
        return this.values.toString();
    }

    public static final class Builder {
        private final Map<DataComponentType<?>, Object> values = new LinkedHashMap<>();

        public <T> Builder set(DataComponentType<T> type, @Nullable T value) {
            if (value == null) {
                this.values.remove(type);
            } else {
                this.values.put(type, value);
            }
            return this;
        }

        public <T> Builder set(Supplier<? extends DataComponentType<T>> type, @Nullable T value) {
            return this.set(type.get(), value);
        }

        public Builder addAll(DataComponentMap map) {
            this.values.putAll(map.values);
            return this;
        }

        public DataComponentMap build() {
            return this.values.isEmpty() ? EMPTY : new DataComponentMap(Collections.unmodifiableMap(new LinkedHashMap<>(this.values)));
        }
    }
}
