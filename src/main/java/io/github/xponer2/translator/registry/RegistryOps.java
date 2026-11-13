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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

/**
 * 1.21 {@code RegistryOps}: a format (NBT, JSON) that knows the registries, what {@code registries
 * .createSerializationContext(NbtOps.INSTANCE)} gives (over 100 calls in the tested mods). Everything is the wrapped
 * format's; the registries are the game's ({@link RegistryAccess}).
 */
public class RegistryOps<T> implements DynamicOps<T> {
	private final DynamicOps<T> delegate;
	private final HolderLookup.Provider registries;

	@SuppressWarnings("unchecked")
	protected RegistryOps(DynamicOps<T> delegate, HolderLookup.Provider registries) {
		this.delegate = delegate instanceof RegistryOps<?> ops ? (DynamicOps<T>) ops.delegate : delegate;
		this.registries = registries;
	}

	public static <T> RegistryOps<T> create(DynamicOps<T> delegate, HolderLookup.Provider registries) {
		return new RegistryOps<>(delegate, registries);
	}

	public <U> RegistryOps<U> withParent(DynamicOps<U> parent) {
		return parent == this.delegate ? (RegistryOps<U>) (Object) this : new RegistryOps<>(parent, this.registries);
	}

	/** The registry of that key, for codecs that read holders. */
	public <E> Optional<Registry<E>> getter(ResourceKey<? extends Registry<? extends E>> key) {
		return this.registries.lookup(key);
	}

	/**
	 * 1.21's holder owner check: 1.12.2 has one set of registries, every holder belongs. The carried HolderOwner of the
	 * registry (RegistryFixedCodec casts it to one: the registries themselves failed the cast).
	 */
	public <E> Optional<Object> owner(ResourceKey<? extends Registry<? extends E>> key) {
		return Optional.of(HolderOwners.of(key));
	}

	public DynamicOps<T> delegate() {
		return this.delegate;
	}

	@Override
	public T empty() {
		return this.delegate.empty();
	}

	@Override
	public T emptyMap() {
		return this.delegate.emptyMap();
	}

	@Override
	public T emptyList() {
		return this.delegate.emptyList();
	}

	@Override
	public <U> U convertTo(DynamicOps<U> outOps, T input) {
		return this.delegate.convertTo(outOps, input);
	}

	@Override
	public DataResult<Number> getNumberValue(T input) {
		return this.delegate.getNumberValue(input);
	}

	@Override
	public Number getNumberValue(T input, Number defaultValue) {
		return this.delegate.getNumberValue(input, defaultValue);
	}

	@Override
	public T createNumeric(Number value) {
		return this.delegate.createNumeric(value);
	}

	@Override
	public T createByte(byte value) {
		return this.delegate.createByte(value);
	}

	@Override
	public T createShort(short value) {
		return this.delegate.createShort(value);
	}

	@Override
	public T createInt(int value) {
		return this.delegate.createInt(value);
	}

	@Override
	public T createLong(long value) {
		return this.delegate.createLong(value);
	}

	@Override
	public T createFloat(float value) {
		return this.delegate.createFloat(value);
	}

	@Override
	public T createDouble(double value) {
		return this.delegate.createDouble(value);
	}

	@Override
	public DataResult<Boolean> getBooleanValue(T input) {
		return this.delegate.getBooleanValue(input);
	}

	@Override
	public T createBoolean(boolean value) {
		return this.delegate.createBoolean(value);
	}

	@Override
	public DataResult<String> getStringValue(T input) {
		return this.delegate.getStringValue(input);
	}

	@Override
	public T createString(String value) {
		return this.delegate.createString(value);
	}

	@Override
	public DataResult<T> mergeToList(T list, T value) {
		return this.delegate.mergeToList(list, value);
	}

	@Override
	public DataResult<T> mergeToList(T list, List<T> values) {
		return this.delegate.mergeToList(list, values);
	}

	@Override
	public DataResult<T> mergeToMap(T map, T key, T value) {
		return this.delegate.mergeToMap(map, key, value);
	}

	@Override
	public DataResult<T> mergeToMap(T map, Map<T, T> values) {
		return this.delegate.mergeToMap(map, values);
	}

	@Override
	public DataResult<T> mergeToMap(T map, MapLike<T> values) {
		return this.delegate.mergeToMap(map, values);
	}

	@Override
	public DataResult<T> mergeToPrimitive(T prefix, T value) {
		return this.delegate.mergeToPrimitive(prefix, value);
	}

	@Override
	public DataResult<Stream<Pair<T, T>>> getMapValues(T input) {
		return this.delegate.getMapValues(input);
	}

	@Override
	public DataResult<Consumer<BiConsumer<T, T>>> getMapEntries(T input) {
		return this.delegate.getMapEntries(input);
	}

	@Override
	public T createMap(Stream<Pair<T, T>> map) {
		return this.delegate.createMap(map);
	}

	@Override
	public DataResult<MapLike<T>> getMap(T input) {
		return this.delegate.getMap(input);
	}

	@Override
	public T createMap(Map<T, T> map) {
		return this.delegate.createMap(map);
	}

	@Override
	public DataResult<Stream<T>> getStream(T input) {
		return this.delegate.getStream(input);
	}

	@Override
	public DataResult<Consumer<Consumer<T>>> getList(T input) {
		return this.delegate.getList(input);
	}

	@Override
	public T createList(Stream<T> input) {
		return this.delegate.createList(input);
	}

	@Override
	public DataResult<ByteBuffer> getByteBuffer(T input) {
		return this.delegate.getByteBuffer(input);
	}

	@Override
	public T createByteList(ByteBuffer input) {
		return this.delegate.createByteList(input);
	}

	@Override
	public DataResult<IntStream> getIntStream(T input) {
		return this.delegate.getIntStream(input);
	}

	@Override
	public T createIntList(IntStream input) {
		return this.delegate.createIntList(input);
	}

	@Override
	public DataResult<LongStream> getLongStream(T input) {
		return this.delegate.getLongStream(input);
	}

	@Override
	public T createLongList(LongStream input) {
		return this.delegate.createLongList(input);
	}

	@Override
	public T remove(T input, String key) {
		return this.delegate.remove(input, key);
	}

	@Override
	public boolean compressMaps() {
		return this.delegate.compressMaps();
	}

	@Override
	public DataResult<T> get(T input, String key) {
		return this.delegate.get(input, key);
	}

	@Override
	public DataResult<T> getGeneric(T input, T key) {
		return this.delegate.getGeneric(input, key);
	}

	@Override
	public T set(T input, String key, T value) {
		return this.delegate.set(input, key, value);
	}

	@Override
	public T update(T input, String key, Function<T, T> function) {
		return this.delegate.update(input, key, function);
	}

	@Override
	public T updateGeneric(T input, T key, Function<T, T> function) {
		return this.delegate.updateGeneric(input, key, function);
	}

	@Override
	public ListBuilder<T> listBuilder() {
		return new ListBuilder.Builder<>(this);
	}

	@Override
	public RecordBuilder<T> mapBuilder() {
		return new RecordBuilder.MapBuilder<>(this);
	}

	@Override
	public String toString() {
		return "RegistryOps[" + this.delegate + "]";
	}
}
