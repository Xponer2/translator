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
package io.github.xponer2.translator.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * NeoForge model data (a block entity's data for its baked model): values by model property (NeoForge's
 * {@code ModelProperty}, carried with the API, are the keys). 1.12.2 asks no block entity for it; the mods' own code
 * passes it around.
 */
public final class ModelData {
    public static final ModelData EMPTY = new ModelData(Collections.emptyMap());

    private final Map<Object, Object> properties;

    private ModelData(Map<Object, Object> properties) {
        this.properties = properties;
    }

    public Set<Object> getProperties() {
        return this.properties.keySet();
    }

    public boolean has(Object property) {
        return this.properties.containsKey(property);
    }

    @Nullable
    public Object get(Object property) {
        return this.properties.get(property);
    }

    public Builder derive() {
        return new Builder(new HashMap<>(this.properties));
    }

    public static Builder builder() {
        return new Builder(new HashMap<>());
    }

    public static ModelData of(Object property, Object value) {
        return builder().with(property, value).build();
    }

    public static final class Builder {
        private final Map<Object, Object> properties;

        private Builder(Map<Object, Object> properties) {
            this.properties = properties;
        }

        public Builder with(Object property, @Nullable Object value) {
            if (value == null) {
                this.properties.remove(property);
            } else {
                this.properties.put(property, value);
            }
            return this;
        }

        public ModelData build() {
            return this.properties.isEmpty() ? EMPTY : new ModelData(Collections.unmodifiableMap(new HashMap<>(this.properties)));
        }
    }
}
