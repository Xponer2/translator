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
package io.github.xponer2.translator.runtime.redirect;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

/**
 * The DataFixerUpper 8 methods (Minecraft 1.21.1) on the DataFixerUpper 4 the Translator carries (runtime,
 * neoforge-1.21.1-redirects.tsv): {@code result.getOrThrow()} becomes {@code DfuRedirects.getOrThrow(result)}.
 * DataResult.Error is DataResult.PartialResult (neoforge-1.21.1-runtime.tsv).
 */
public final class DfuRedirects {
	private DfuRedirects() {
	}

	// --- DataResult --------------------------------------------------------------------------------------------------

	public static <R> DataResult<R> error(Supplier<String> message) {
		return DataResult.error(message.get());
	}

	public static <R> DataResult<R> error(Supplier<String> message, Object partial) {
		@SuppressWarnings("unchecked")
		R value = (R) partial;
		return DataResult.error(message.get(), value);
	}

	public static <R> R getOrThrow(DataResult<R> result) {
		return result.getOrThrow(false, message -> {
		});
	}

	public static <R> R getOrThrow(DataResult<R> result, Function<String, ? extends Throwable> exception) {
		Optional<R> value = result.result();
		if (value.isPresent()) {
			return value.get();
		}
		throw DfuRedirects.<RuntimeException>sneaky(exception.apply(result.error().map(DataResult.PartialResult::message).orElse("")));
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> E sneaky(Throwable t) throws E {
		throw (E) t;
	}

	/** DFU 8 {@code getPartialOrThrow}: the result, or the partial result of an error, else an exception. */
	public static <R> R getPartialOrThrow(DataResult<R> result) {
		return result.resultOrPartial(message -> { }).orElseThrow(() -> new IllegalStateException(
				result.error().map(DataResult.PartialResult::message).orElse("No value")));
	}

	public static <R, E extends Throwable> R getPartialOrThrow(DataResult<R> result, Function<String, E> exception) throws E {
		var value = result.resultOrPartial(message -> { });
		if (value.isPresent()) {
			return value.get();
		}
		throw exception.apply(result.error().map(DataResult.PartialResult::message).orElse("No value"));
	}

	public static <R> Optional<R> resultOrPartial(DataResult<R> result) {
		return result.resultOrPartial(message -> {
		});
	}

	public static boolean isSuccess(DataResult<?> result) {
		return result.result().isPresent();
	}

	public static boolean isError(DataResult<?> result) {
		return !result.result().isPresent();
	}

	public static <R> DataResult<R> ifSuccess(DataResult<R> result, Consumer<? super R> action) {
		result.result().ifPresent(action);
		return result;
	}

	public static <R> DataResult<R> ifError(DataResult<R> result, Consumer<? super DataResult.PartialResult<R>> action) {
		if (!result.result().isPresent()) {
			result.error().ifPresent(action);
		}
		return result;
	}

	public static <R, T> T mapOrElse(DataResult<R> result, Function<? super R, ? extends T> success,
			Function<? super DataResult.PartialResult<R>, ? extends T> error) {
		Optional<R> value = result.result();
		return value.isPresent() ? success.apply(value.get()) : error.apply(result.error().get());
	}

	public static <R> DataResult<R> setPartial(DataResult<R> result, Supplier<R> partial) {
		return result.setPartial(partial.get());
	}

	// --- Codec / MapCodec --------------------------------------------------------------------------------------------

	public static <A> Codec<A> lazyInitialized(Supplier<Codec<A>> supplier) {
		return new Codec<A>() {
			private Codec<A> codec;

			private Codec<A> codec() {
				if (this.codec == null) {
					this.codec = supplier.get();
				}
				return this.codec;
			}

			@Override
			public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
				return this.codec().decode(ops, input);
			}

			@Override
			public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
				return this.codec().encode(input, ops, prefix);
			}
		};
	}

	/** A codec that refers to itself (a component inside a component): {@code wrapped} gets the codec it makes. */
	public static <A> Codec<A> recursive(String name, Function<Codec<A>, Codec<A>> wrapped) {
		return new Codec<A>() {
			private Codec<A> codec;

			private Codec<A> codec() {
				if (this.codec == null) {
					this.codec = wrapped.apply(this);
				}
				return this.codec;
			}

			@Override
			public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
				return this.codec().decode(ops, input);
			}

			@Override
			public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
				return this.codec().encode(input, ops, prefix);
			}

			@Override
			public String toString() {
				return "RecursiveCodec[" + name + "]";
			}
		};
	}

	public static <A> Codec<A> withAlternative(Codec<A> primary, Codec<? extends A> alternative) {
		return Codec.either(primary, alternative).xmap(either -> either.map(Function.identity(), Function.identity()), Either::left);
	}

	public static <A, B> Codec<A> withAlternative(Codec<A> primary, Codec<B> alternative, Function<B, A> converter) {
		return Codec.either(primary, alternative).xmap(either -> either.map(Function.identity(), converter), Either::left);
	}

	public static <A> Codec<A> validate(Codec<A> codec, Function<A, DataResult<A>> checker) {
		return codec.flatXmap(checker, checker);
	}

	public static <A> MapCodec<A> validate(MapCodec<A> codec, Function<A, DataResult<A>> checker) {
		return codec.flatXmap(checker, checker);
	}

	/** DFU 8 {@code Codec.string(min, max)}: a string of that length (ExtraCodecs.PLAYER_NAME: without it the rest of ExtraCodecs stayed null). */
	public static Codec<String> string(int min, int max) {
		Function<String, DataResult<String>> check = s -> s.length() < min ? DataResult.error("String \"" + s + "\" is too short: " + s.length() + ", expected range [" + min + "-" + max + "]")
				: s.length() > max ? DataResult.error("String \"" + s + "\" is too long: " + s.length() + ", expected range [" + min + "-" + max + "]") : DataResult.success(s);
		return Codec.STRING.flatXmap(check, check);
	}

	/** DFU 8 {@code Codec.xor}: one of two codecs (DFU 4 {@code Codec.either}, which takes the first that decodes). */
	public static <F, S> Codec<Either<F, S>> xor(Codec<F> first, Codec<S> second) {
		return Codec.either(first, second);
	}

	/** DFU 8 {@code Codec.dispatchedMap}: a map whose values are decoded with the codec their key names. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <K, V> Codec<java.util.Map<K, V>> dispatchedMap(Codec<K> keyCodec, Function<K, Codec<? extends V>> valueCodec) {
		return new Codec<java.util.Map<K, V>>() {
			@Override
			public <T> DataResult<Pair<java.util.Map<K, V>, T>> decode(DynamicOps<T> ops, T input) {
				return ops.getMap(input).flatMap(map -> {
					var result = new java.util.LinkedHashMap<K, V>();
					var errors = new StringBuilder();
					map.entries().forEach(entry -> {
						var key = keyCodec.parse(ops, entry.getFirst());
						var value = key.flatMap(k -> ((Codec<V>) valueCodec.apply(k)).parse(ops, entry.getSecond()));
						var k = key.result();
						var v = value.result();
						if (k.isPresent() && v.isPresent()) {
							result.put(k.get(), v.get());
						} else {
							value.error().ifPresent(e -> errors.append(e.message()).append("; "));
						}
					});
					var pair = Pair.of((java.util.Map<K, V>) result, input);
					return errors.length() == 0 ? DataResult.success(pair) : DataResult.error(errors.toString(), pair);
				});
			}

			@Override
			public <T> DataResult<T> encode(java.util.Map<K, V> input, DynamicOps<T> ops, T prefix) {
				RecordBuilder<T> builder = ops.mapBuilder();
				for (var entry : input.entrySet()) {
					builder.add(keyCodec.encodeStart(ops, entry.getKey()), ((Codec) valueCodec.apply(entry.getKey())).encodeStart(ops, entry.getValue()));
				}
				return builder.build(prefix);
			}

			@Override
			public String toString() {
				return "DispatchedMapCodec[" + keyCodec + "]";
			}
		};
	}

	public static <A> Codec<List<A>> listOf(Codec<A> codec, int min, int max) {
		Function<List<A>, DataResult<List<A>>> check = list -> list.size() < min ? DataResult.error("List too short: " + list.size() + ", expected " + min)
				: list.size() > max ? DataResult.error("List too long: " + list.size() + ", expected at most " + max) : DataResult.success(list);
		return codec.listOf().flatXmap(check, check);
	}

	public static <A> Codec<List<A>> sizeLimitedListOf(Codec<A> codec, int max) {
		return listOf(codec, 0, max);
	}

	public static <A> MapCodec<Optional<A>> lenientOptionalFieldOf(Codec<A> codec, String name) {
		return codec.optionalFieldOf(name);
	}

	public static <A> MapCodec<A> lenientOptionalFieldOf(Codec<A> codec, String name, A defaultValue) {
		return codec.optionalFieldOf(name, defaultValue);
	}

	public static <A> MapCodec<Optional<A>> optionalField(String name, Codec<A> codec, boolean lenient) {
		return codec.optionalFieldOf(name);
	}

	public static <A> DataResult<A> parse(Codec<A> codec, Dynamic<?> input) {
		return parseDynamic(codec, input);
	}

	private static <A, T> DataResult<A> parseDynamic(Codec<A> codec, Dynamic<T> input) {
		return codec.parse(input.getOps(), input.getValue());
	}

	public static <E> Codec<E> stringResolver(Function<E, String> toString, Function<String, E> fromString) {
		return Codec.STRING.flatXmap(name -> {
			E value = fromString.apply(name);
			return value != null ? DataResult.success(value) : DataResult.error("Unknown element name:" + name);
		}, value -> {
			String name = toString.apply(value);
			return name != null ? DataResult.success(name) : DataResult.error("Element with unknown name: " + value);
		});
	}

	/** The map the codec reads, as a MapCodec (a codec made from a MapCodec gives that one back). */
	public static <A> MapCodec<A> assumeMapUnsafe(Codec<A> codec) {
		if (codec instanceof MapCodec.MapCodecCodec) {
			return ((MapCodec.MapCodecCodec<A>) codec).codec();
		}
		return new MapCodec<A>() {
			@Override
			public <T> Stream<T> keys(DynamicOps<T> ops) {
				return Stream.empty();
			}

			@Override
			public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
				return codec.parse(ops, ops.createMap(input.entries()));
			}

			@Override
			public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
				DataResult<T> encoded = codec.encodeStart(ops, input);
				Optional<T> value = encoded.result();
				if (!value.isPresent()) {
					return prefix.withErrorsFrom(encoded);
				}
				return ops.getMapEntries(value.get()).map(entries -> {
					entries.accept(prefix::add);
					return prefix;
				}).result().orElseGet(() -> prefix.withErrorsFrom(encoded));
			}
		};
	}

	// --- Either ------------------------------------------------------------------------------------------------------

	public static <T> T unwrap(Either<? extends T, ? extends T> either) {
		return either.map(Function.identity(), Function.identity());
	}

	// --- DFU 8 dispatch: the codec of each kind is a MapCodec (DFU 4 wants a Codec and cast it: every 1.21 feature,
	// placement, block state provider and trunk placer threw "MapCodec$1 cannot be cast to Codec") ----------------------

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static <K, V> Function<? super K, ? extends Codec<? extends V>> asCodecs(Function<? super K, ?> codecs) {
		return key -> {
			Object codec = codecs.apply(key);
			return codec instanceof MapCodec map ? map.codec() : (Codec<? extends V>) codec;
		};
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static <K, V> Function<? super K, ? extends DataResult<? extends Codec<? extends V>>> asCodecResults(Function<? super K, ?> codecs) {
		return key -> ((DataResult<Object>) codecs.apply(key)).map(codec -> codec instanceof MapCodec map ? map.codec() : (Codec) codec);
	}

	/** DFU 8 {@code codec.dispatch(type, codec)} with MapCodecs. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <K, V> Codec<V> dispatch(Codec<K> keys, Function<? super V, ? extends K> type, Function<? super K, ?> codecs) {
		return keys.dispatch(type, (Function) asCodecs(codecs));
	}

	/** DFU 8 {@code codec.dispatch(typeKey, type, codec)} with MapCodecs. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <K, V> Codec<V> dispatch(Codec<K> keys, String typeKey, Function<? super V, ? extends K> type, Function<? super K, ?> codecs) {
		return keys.dispatch(typeKey, type, (Function) asCodecs(codecs));
	}

	/** DFU 8 {@code codec.dispatchStable(type, codec)} with MapCodecs. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <K, V> Codec<V> dispatchStable(Codec<K> keys, Function<? super V, ? extends K> type, Function<? super K, ?> codecs) {
		return keys.dispatchStable(type, (Function) asCodecs(codecs));
	}

	/** DFU 8 {@code codec.partialDispatch(typeKey, type, codec)} with MapCodec results. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <K, V> Codec<V> partialDispatch(Codec<K> keys, String typeKey, Function<? super V, ? extends DataResult<? extends K>> type,
			Function<? super K, ?> codecs) {
		return keys.partialDispatch(typeKey, (Function) type, (Function) asCodecResults(codecs));
	}

	/** DFU 8 {@code codec.dispatchMap(typeKey, type, codec)} with MapCodecs. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <K, V> MapCodec<V> dispatchMap(Codec<K> keys, String typeKey, Function<? super V, ? extends K> type, Function<? super K, ?> codecs) {
		return keys.dispatchMap(typeKey, type, (Function) asCodecs(codecs));
	}
}
