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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/** 1.20.5's {@code DataComponentMap}: a read-only view of components (a stack's, made from its data when asked). */
public interface DataComponentMap extends Iterable<TypedDataComponent<?>> {
    DataComponentMap EMPTY = new Snapshot(Map.of());

    @Nullable
    <T> T get(DataComponentType<? extends T> type);

    Set<DataComponentType<?>> keySet();

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
