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

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

/**
 * DFU 7+ static methods of {@code MapCodec} the bundled DFU hasn't (a class of their own: {@link DfuRedirects} has
 * {@code Codec.recursive} with the same parameters).
 */
public final class MapCodecRedirects {
	private MapCodecRedirects() {
	}

	/**
	 * DFU 7+ {@code MapCodec.recursive(name, wrapped)}: a map codec that refers to itself (made when first used, from its
	 * own codec). Supplementaries' mob data (cages and jars with a mob) built its codec with it: without it the codec was
	 * null and every such block entity failed to load.
	 */
	public static <A> MapCodec<A> recursive(String name, Function<Codec<A>, MapCodec<A>> wrapped) {
		return new RecursiveMapCodec<>(name, wrapped);
	}

	private static final class RecursiveMapCodec<A> extends MapCodec<A> {
		private final String name;
		private final Supplier<MapCodec<A>> wrapped;

		RecursiveMapCodec(String name, Function<Codec<A>, MapCodec<A>> wrapped) {
			this.name = name;
			this.wrapped = com.google.common.base.Suppliers.memoize(() -> wrapped.apply(this.codec()))::get;
		}

		@Override
		public <T> Stream<T> keys(DynamicOps<T> ops) {
			return this.wrapped.get().keys(ops);
		}

		@Override
		public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
			return this.wrapped.get().decode(ops, input);
		}

		@Override
		public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
			return this.wrapped.get().encode(input, ops, prefix);
		}

		@Override
		public String toString() {
			return "RecursiveMapCodec[" + this.name + "]";
		}
	}
}
