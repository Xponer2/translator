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
package io.github.xponer2.translator.registry;

import net.minecraft.util.ResourceLocation;

import java.util.Optional;

/**
 * A registry entry (1.21 {@code net.minecraft.core.Holder}).
 */
public interface Holder<T> {
    T value();

    boolean isBound();

    Optional<ResourceKey<T>> unwrapKey();

    default boolean is(ResourceLocation location) {
        return this.unwrapKey().map(key -> key.location().equals(location)).orElse(false);
    }

    default boolean is(ResourceKey<T> key) {
        return this.unwrapKey().map(k -> k == key).orElse(false);
    }

    static <T> Holder<T> direct(T value) {
        return new Direct<>(value);
    }

    /**
     * A holder of an object that isn't registered.
     */
    final class Direct<T> implements Holder<T> {
        private final T value;

        public Direct(T value) {
            this.value = value;
        }

        @Override
        public T value() {
            return this.value;
        }

        @Override
        public boolean isBound() {
            return true;
        }

        @Override
        public Optional<ResourceKey<T>> unwrapKey() {
            return Optional.empty();
        }

        @Override
        public String toString() {
            return "Direct{" + this.value + "}";
        }
    }
}
