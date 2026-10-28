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
package io.github.xponer2.translator.entity;

import javax.annotation.Nullable;

import net.minecraft.entity.ai.attributes.IAttribute;

/**
 * 1.21 attribute: a 1.12.2 IAttribute, or one 1.12.2 keeps elsewhere (step height in a field, creative flight in the
 * player capabilities), applied by {@link AttributeInstance}.
 */
public final class Attribute {
	@Nullable
	private final IAttribute forge;
	private final String name;
	private final double defaultValue;

	Attribute(@Nullable IAttribute forge, String name, double defaultValue) {
		this.forge = forge;
		this.name = name;
		this.defaultValue = defaultValue;
	}

	@Nullable
	public IAttribute forge() {
		return this.forge;
	}

	public String getDescriptionId() {
		return this.forge != null ? "attribute.name." + this.forge.getName() : "attribute.name." + this.name;
	}

	public double getDefaultValue() {
		return this.forge != null ? this.forge.getDefaultValue() : this.defaultValue;
	}

	String name() {
		return this.name;
	}

	@Override
	public String toString() {
		return this.name;
	}
}
