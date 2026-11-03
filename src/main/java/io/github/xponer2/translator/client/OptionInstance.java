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
package io.github.xponer2.translator.client;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A client option (1.21 {@code OptionInstance}, neoforge-1.21.1-runtime.tsv): the 1.12.2 game setting it reads and
 * writes, or a fixed 1.21 default for an option 1.12.2 doesn't have.
 */
@SideOnly(Side.CLIENT)
public final class OptionInstance<T> {
	private final Supplier<T> getter;
	private final Consumer<T> setter;

	public OptionInstance(Supplier<T> getter, Consumer<T> setter) {
		this.getter = getter;
		this.setter = setter;
	}

	public static <T> OptionInstance<T> fixed(T value) {
		return new OptionInstance<>(() -> value, ignored -> {
		});
	}

	public T get() {
		return this.getter.get();
	}

	public void set(T value) {
		this.setter.accept(value);
	}
}
