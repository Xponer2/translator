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
    static <E extends Enum<E> & StringRepresentable> Codec<E> fromEnum(Supplier<E[]> values) {
        return fromValues(values);
    }

    static <E extends StringRepresentable> Codec<E> fromValues(Supplier<E[]> values) {
        Map<String, E> byName = Arrays.stream(values.get()).collect(Collectors.toMap(StringRepresentable::getSerializedName, Function.identity()));
        return Codec.STRING.comapFlatMap(name -> {
            var value = byName.get(name);
            return value != null ? DataResult.success(value) : DataResult.error("Unknown element name: " + name);
        }, StringRepresentable::getSerializedName);
    }
}
