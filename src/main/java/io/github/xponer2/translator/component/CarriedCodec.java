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
package io.github.xponer2.translator.component;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

/**
 * The {@code CODEC} of a carried 1.21.1 class, looked up on first use (the class is carried after the Translator's
 * component types are made).
 */
final class CarriedCodec<T> implements Codec<T> {
	private final String className;
	private volatile Codec<T> codec;

	CarriedCodec(String className) {
		this.className = className;
	}

	@SuppressWarnings("unchecked")
	private Codec<T> codec() {
		Codec<T> found = this.codec;
		if (found == null) {
			try {
				found = (Codec<T>) Class.forName(this.className, true, net.minecraft.launchwrapper.Launch.classLoader).getField("CODEC").get(null);
			} catch (ReflectiveOperationException | LinkageError e) {
				throw new IllegalStateException("No codec of " + this.className, e);
			}
			this.codec = found;
		}
		return found;
	}

	@Override
	public <O> DataResult<Pair<T, O>> decode(DynamicOps<O> ops, O input) {
		try {
			return this.codec().decode(ops, input);
		} catch (IllegalStateException e) {
			return DataResult.error(e.getMessage());
		}
	}

	@Override
	public <O> DataResult<O> encode(T input, DynamicOps<O> ops, O prefix) {
		try {
			return this.codec().encode(input, ops, prefix);
		} catch (IllegalStateException e) {
			return DataResult.error(e.getMessage());
		}
	}

	@Override
	public String toString() {
		return "CarriedCodec[" + this.className + "]";
	}
}
