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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.xponer2.translator.network.StreamCodec;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.IStringSerializable;

/**
 * The 1.21 {@code CODEC} / {@code STREAM_CODEC} of vanilla enums 1.12.2 declares without them (Direction, DyeColor,
 * Rotation, Mirror, GameType, EquipmentSlot, ...): the converter reads these instead of the missing static fields
 * (MethodGuard), a null there took down every codec built on them in a static initializer. Names are the 1.12.2
 * {@link IStringSerializable} names, else the lower-case constant; the few that changed since are aliased.
 */
public final class EnumCodecs {
    private static final Map<Class<?>, StringRepresentable.EnumCodec<?>> CODECS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, StreamCodec<ByteBuf, ?>> STREAM_CODECS = new ConcurrentHashMap<>();

    private EnumCodecs() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static StringRepresentable.EnumCodec codec(Class type) {
        return CODECS.computeIfAbsent(type, t -> make((Class<? extends Enum>) t));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <E extends Enum<E>> StringRepresentable.EnumCodec<E> make(Class<E> type) {
        var byName = new LinkedHashMap<String, E>();
        for (E constant : type.getEnumConstants()) {
            byName.put(name(constant), constant);
        }
        return new StringRepresentable.EnumCodec<>(byName, EnumCodecs::name);
    }

    /** The values by ordinal (a var int), as 1.21's ByteBufCodecs.idMapper does for enums. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static StreamCodec streamCodec(Class type) {
        return STREAM_CODECS.computeIfAbsent(type, t -> {
            Object[] values = t.getEnumConstants();
            return StreamCodec.<ByteBuf, Object>of(
                    (buffer, value) -> io.github.xponer2.translator.network.ByteBufCodecs.VAR_INT.encode(buffer, ((Enum<?>) value).ordinal()),
                    buffer -> values[io.github.xponer2.translator.network.ByteBufCodecs.VAR_INT.decode(buffer)]);
        });
    }

    static String name(Enum<?> constant) {
        // a 1.12.2 name 1.21 changed (the flattening)
        if (constant instanceof net.minecraft.item.EnumDyeColor && "SILVER".equals(constant.name())) {
            return "light_gray";
        }
        if (constant instanceof IStringSerializable named) {
            return named.getName();
        }
        return constant.name().toLowerCase(Locale.ROOT);
    }
}
