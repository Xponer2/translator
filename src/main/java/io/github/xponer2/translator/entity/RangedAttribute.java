/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.entity;

/**
 * An attribute with a range (1.21 {@code RangedAttribute}, neoforge-1.21.1-runtime.tsv): a mod's attribute, a 1.12.2
 * ranged attribute of the same name underneath (entities get it when they get its modifiers).
 */
public class RangedAttribute extends Attribute {
	private final double minValue;
	private final double maxValue;

	public RangedAttribute(String descriptionId, double defaultValue, double minValue, double maxValue) {
		super(new net.minecraft.entity.ai.attributes.RangedAttribute(null, descriptionId, defaultValue, minValue, maxValue), descriptionId, defaultValue);
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	public double getMinValue() {
		return this.minValue;
	}

	public double getMaxValue() {
		return this.maxValue;
	}

	public double sanitizeValue(double value) {
		return Double.isNaN(value) ? this.minValue : Math.max(this.minValue, Math.min(this.maxValue, value));
	}
}
