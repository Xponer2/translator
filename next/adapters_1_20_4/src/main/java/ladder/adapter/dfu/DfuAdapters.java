/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.dfu;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;

import ladder.adapter.provided.DataResultError;

/**
 * DFU 8's API (Minecraft 1.21) on DFU 6 (1.20.4), receiver first. Two of its changes the descriptors don't show: a
 * dispatch codec's types give map codecs (DFU 6: codecs), and a result's error is an {@code Error} (DFU 6: a partial
 * result); both are converted here.
 */
public final class DfuAdapters {
    private DfuAdapters() {
    }

    // ------------------------------------------------------------------ DataResult

    private static <R> DataResultError<R> errorValue(DataResult<R> result) {
        String message = result.error().map(DataResult.PartialResult::message).orElse("");
        return new DataResultError<>(message, result.resultOrPartial(m -> {
        }));
    }

    /** DFU 8's {@code getOrThrow()}: the value, else IllegalStateException with the error */
    public static Object getOrThrow(DataResult<?> result) {
        Optional<?> value = result.result();
        if (value.isPresent()) {
            return value.get();
        }
        throw new IllegalStateException(errorValue(result).message());
    }

    /** DFU 8's {@code getOrThrow(error -> exception)} */
    public static <E extends Throwable> Object getOrThrow(DataResult<?> result, Function<String, E> exception) throws E {
        Optional<?> value = result.result();
        if (value.isPresent()) {
            return value.get();
        }
        throw exception.apply(errorValue(result).message());
    }

    public static boolean isSuccess(DataResult<?> result) {
        return result.result().isPresent();
    }

    public static boolean isError(DataResult<?> result) {
        return result.error().isPresent();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static DataResult<?> ifSuccess(DataResult<?> result, Consumer consumer) {
        result.result().ifPresent(consumer);
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static DataResult<?> ifError(DataResult<?> result, Consumer consumer) {
        if (result.error().isPresent()) {
            consumer.accept(errorValue(result));
        }
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object mapOrElse(DataResult<?> result, Function success, Function failure) {
        Optional<?> value = result.result();
        return value.isPresent() ? success.apply(value.get()) : failure.apply(errorValue(result));
    }

    /** DFU 8's {@code resultOrPartial()} (no error consumer) */
    public static Optional<?> resultOrPartial(DataResult<?> result) {
        return result.resultOrPartial(m -> {
        });
    }

    /** DFU 8's {@code error()}: an Error, not DFU 6's partial result */
    public static Optional<?> errorOf(DataResult<?> result) {
        return result.error().isPresent() ? Optional.of(errorValue(result)) : Optional.empty();
    }

    // ------------------------------------------------------------------ Codec

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Codec<?> validate(Codec codec, Function check) {
        return codec.flatXmap(check, check);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static MapCodec<?> validate(MapCodec codec, Function check) {
        return codec.flatXmap(check, check);
    }

    /** DFU 6's optional fields are the lenient ones (DFU 8 made the plain ones strict) */
    public static MapCodec<?> lenientOptionalFieldOf(Codec<?> codec, String name) {
        return codec.optionalFieldOf(name);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static MapCodec<?> lenientOptionalFieldOf(Codec codec, String name, Object fallback) {
        return codec.optionalFieldOf(name, fallback);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static MapCodec<?> lenientOptionalFieldOf(Codec codec, String name, Object fallback, Lifecycle lifecycle) {
        return codec.optionalFieldOf(name, fallback, lifecycle);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static MapCodec<?> lenientOptionalFieldOf(Codec codec, String name, Lifecycle fieldLifecycle, Object fallback, Lifecycle lifecycle) {
        return codec.optionalFieldOf(name, fieldLifecycle, fallback, lifecycle);
    }

    /** DFU 8's {@code listOf(min, max)} */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Codec<?> listOf(Codec codec, int min, int max) {
        Function<List<?>, DataResult<List<?>>> check = list -> list.size() < min || list.size() > max
                ? DataResult.error(() -> "List size " + list.size() + " outside of [" + min + ", " + max + "]")
                : DataResult.success(list);
        return codec.listOf().flatXmap((Function) check, (Function) check);
    }

    /** a dispatch type's codec: DFU 8 gives a map codec, DFU 6 takes a codec (a map codec's codec keeps its fields inline) */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Function asCodecs(Function codecOf) {
        return type -> {
            Object c = codecOf.apply(type);
            return c instanceof MapCodec<?> map ? map.codec() : c;
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Codec<?> dispatch(Codec codec, Function type, Function codecOf) {
        return codec.dispatch(type, asCodecs(codecOf));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Codec<?> dispatch(Codec codec, String key, Function type, Function codecOf) {
        return codec.dispatch(key, type, asCodecs(codecOf));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Codec<?> dispatchStable(Codec codec, Function type, Function codecOf) {
        return codec.dispatchStable(type, asCodecs(codecOf));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static MapCodec<?> dispatchMap(Codec codec, Function type, Function codecOf) {
        return codec.dispatchMap(type, asCodecs(codecOf));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static MapCodec<?> dispatchMap(Codec codec, String key, Function type, Function codecOf) {
        return codec.dispatchMap(key, type, asCodecs(codecOf));
    }
}
