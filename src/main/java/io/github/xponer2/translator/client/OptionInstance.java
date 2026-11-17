/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
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
	@javax.annotation.Nullable
	private final net.minecraft.client.OptionInstance.ValueSet<T> values;
	@javax.annotation.Nullable
	private final com.mojang.serialization.Codec<T> codec;
	/** An option of a mod's own (the 1.21 constructors): its value here. */
	private T value;

	public OptionInstance(Supplier<T> getter, Consumer<T> setter) {
		this.getter = getter;
		this.setter = setter;
		this.values = null;
		this.codec = null;
	}

	/**
	 * 1.21 {@code new OptionInstance(caption, tooltip, toString, values, initial, onValueUpdate)}: a mod's own option
	 * (the config screens of a dozen mods made theirs so: their constructor had nothing to call). Its value is kept
	 * here, the mod told of each change.
	 */
	public OptionInstance(String caption, net.minecraft.client.OptionInstance.TooltipSupplier<T> tooltip,
			net.minecraft.client.OptionInstance.CaptionBasedToString<T> toString, net.minecraft.client.OptionInstance.ValueSet<T> values, T initial,
			Consumer<T> onValueUpdate) {
		this(caption, tooltip, toString, values, null, initial, onValueUpdate);
	}

	/** The same with the codec it is saved with. */
	public OptionInstance(String caption, net.minecraft.client.OptionInstance.TooltipSupplier<T> tooltip,
			net.minecraft.client.OptionInstance.CaptionBasedToString<T> toString, net.minecraft.client.OptionInstance.ValueSet<T> values,
			@javax.annotation.Nullable com.mojang.serialization.Codec<T> codec, T initial, Consumer<T> onValueUpdate) {
		this.values = values;
		this.codec = codec;
		this.value = initial;
		this.getter = () -> this.value;
		this.setter = value -> {
			if (value != null && !value.equals(this.value)) {
				this.value = value;
				if (onValueUpdate != null) {
					onValueUpdate.accept(value);
				}
			}
		};
	}

	/** 1.21 {@code values()}: the value set a mod's own option was made with (none for the 1.12.2 settings). */
	@javax.annotation.Nullable
	public net.minecraft.client.OptionInstance.ValueSet<T> values() {
		return this.values;
	}

	/** 1.21 {@code codec()}: the codec a mod's own option was made with (none for the 1.12.2 settings). */
	@javax.annotation.Nullable
	public com.mojang.serialization.Codec<T> codec() {
		return this.codec;
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
