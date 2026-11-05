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
        return this.unwrapKey().map(k -> k == key || k.equals(key)).orElse(false);
    }

    /** In a tag of the mods' data: items, blocks, entity types. */
    @SuppressWarnings("unchecked")
    default boolean is(io.github.xponer2.translator.util.TagKey<?> tag) {
        Object value = this.value();
        if (value instanceof net.minecraft.item.Item item) {
            return io.github.xponer2.translator.tags.TagManager.is(item, (io.github.xponer2.translator.util.TagKey<net.minecraft.item.Item>) tag);
        }
        if (value instanceof net.minecraft.block.Block block) {
            return io.github.xponer2.translator.tags.TagManager.is(block, (io.github.xponer2.translator.util.TagKey<net.minecraft.block.Block>) tag);
        }
        if (value instanceof EntityType<?> type) {
            return type.is(tag);
        }
        return false;
    }

    default java.util.stream.Stream<io.github.xponer2.translator.util.TagKey<T>> tags() {
        return java.util.stream.Stream.empty();
    }

    /**
     * 1.21 {@code kind()}: the holder of a registered entry or a direct one. The 1.21 codec of a list of holders
     * ({@code HolderSetCodec}: the blocks of a feature's {@code matching_blocks}) checks that they are of one kind.
     */
    default Kind kind() {
        return this.unwrapKey().isPresent() ? Kind.REFERENCE : Kind.DIRECT;
    }

    /** 1.21 {@code Holder.Kind}. */
    enum Kind {
        REFERENCE,
        DIRECT
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
