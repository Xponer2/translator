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
package io.github.xponer2.translator.runtime.mc;

import java.util.Collection;

import net.minecraft.block.properties.PropertyHelper;

/**
 * 1.21 {@code Property} for the block state properties of 1.21.1 mods that extend it: 1.21's is an abstract class, the
 * table maps it to 1.12.2's IProperty, an interface, which a class can't extend (StorageDrawers' slot property: the
 * class and every drawer block using it failed to load). On 1.12.2's PropertyHelper, with 1.21's methods the mod
 * overrides behind 1.12.2's.
 */
public abstract class Property<T extends Comparable<T>> extends PropertyHelper<T> {
	private Integer hash;

	protected Property(String name, Class<T> valueClass) {
		super(name, valueClass);
	}

	/** 1.21 {@code getPossibleValues()}: the values the property can take. */
	public abstract Collection<T> getPossibleValues();

	/** 1.21 {@code getValue(name)}: the value of that name, empty when there is none. */
	public abstract java.util.Optional<T> getValue(String name);

	@Override
	public Collection<T> getAllowedValues() {
		return this.getPossibleValues();
	}

	@Override
	public com.google.common.base.Optional<T> parseValue(String value) {
		return com.google.common.base.Optional.fromNullable(this.getValue(value).orElse(null));
	}

	/** 1.21 {@code generateHashCode()}: of the value class and the name. */
	public int generateHashCode() {
		return 31 * this.getValueClass().hashCode() + this.getName().hashCode();
	}

	@Override
	public int hashCode() {
		if (this.hash == null) {
			this.hash = this.generateHashCode();
		}
		return this.hash;
	}
}
