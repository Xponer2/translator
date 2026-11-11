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
package io.github.xponer2.translator.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.IStringSerializable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A value with a name for data files (1.21 {@code net.minecraft.util.StringRepresentable}). It is also a 1.12.2
 * {@link IStringSerializable}, so such enums work as block state properties.
 */
public interface StringRepresentable extends IStringSerializable {
    String getSerializedName();

    @Override
    default String getName() {
        return this.getSerializedName();
    }

    /**
     * The enum constants by their serialized names.
     */
    /** The enum constants by their serialized names (the ports compile against this one: keeps its descriptor). */
    static <E extends Enum<E> & StringRepresentable> Codec<E> fromEnum(Supplier<E[]> values) {
        return fromEnumCodec(values);
    }

    /** 1.21 {@code fromEnum} as original 1.21.1 mods call it (it returns the EnumCodec): M line of the runtime table. */
    static <E extends Enum<E> & StringRepresentable> EnumCodec<E> fromEnumCodec(Supplier<E[]> values) {
        E[] constants = values.get();
        var byName = new java.util.LinkedHashMap<String, E>();
        for (E constant : constants) {
            byName.put(constant.getSerializedName(), constant);
        }
        return new EnumCodec<>(byName, StringRepresentable::getSerializedName);
    }

    /**
     * 1.21 {@code StringRepresentable.EnumCodec}: an enum by its serialized name, with {@link #byName} for code that
     * reads the name itself. {@code EnumCodecs} makes them for 1.12.2 enums too (their CODEC fields).
     */
    final class EnumCodec<E> implements Codec<E> {
        private final Map<String, E> byName;
        private final Function<E, String> name;
        private final Codec<E> codec;

        /**
         * 1.21 {@code new EnumCodec(values, resolver)} (Mekanism makes its upgrade and transmission type codecs so; it was
         * left empty and the codecs were null): the constants by their serialized names.
         */
        @SuppressWarnings("unchecked")
        public EnumCodec(Enum<?>[] values, Function<String, E> resolver) {
            this((Map<String, E>) (Map<?, ?>) namesOf(values), value -> value instanceof IStringSerializable named ? named.getName() : String.valueOf(value));
        }

        private static Map<String, Object> namesOf(Enum<?>[] values) {
            var byName = new java.util.LinkedHashMap<String, Object>();
            for (Enum<?> value : values) {
                byName.put(value instanceof IStringSerializable named ? named.getName() : value.name(), value);
            }
            return byName;
        }

        public EnumCodec(Map<String, E> byName, Function<E, String> name) {
            this.byName = byName;
            this.name = name;
            this.codec = Codec.STRING.comapFlatMap(key -> {
                E value = byName.get(key);
                return value != null ? DataResult.success(value) : DataResult.error("Unknown element name: " + key);
            }, name);
        }

        @javax.annotation.Nullable
        public E byName(@javax.annotation.Nullable String key) {
            return key == null ? null : this.byName.get(key);
        }

        public E byName(@javax.annotation.Nullable String key, E fallback) {
            E value = this.byName(key);
            return value != null ? value : fallback;
        }

        public E byName(@javax.annotation.Nullable String key, Supplier<? extends E> fallback) {
            E value = this.byName(key);
            return value != null ? value : fallback.get();
        }

        @Override
        public <T> DataResult<com.mojang.datafixers.util.Pair<E, T>> decode(com.mojang.serialization.DynamicOps<T> ops, T input) {
            return this.codec.decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(E input, com.mojang.serialization.DynamicOps<T> ops, T prefix) {
            return this.codec.encode(input, ops, prefix);
        }

        @Override
        public String toString() {
            return "EnumCodec" + this.byName.keySet();
        }
    }

    static <E extends StringRepresentable> Codec<E> fromValues(Supplier<E[]> values) {
        Map<String, E> byName = Arrays.stream(values.get()).collect(Collectors.toMap(StringRepresentable::getSerializedName, Function.identity()));
        return Codec.STRING.comapFlatMap(name -> {
            var value = byName.get(name);
            return value != null ? DataResult.success(value) : DataResult.error("Unknown element name: " + name);
        }, StringRepresentable::getSerializedName);
    }
}
