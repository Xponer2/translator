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
