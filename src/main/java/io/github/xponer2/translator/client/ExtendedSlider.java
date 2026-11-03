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

import java.text.DecimalFormat;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

/**
 * NeoForge slider with a range, a step and "prefix value suffix" as its text. The value is kept from 0 to 1 like
 * the vanilla slider and snapped to the step.
 */
public class ExtendedSlider extends AbstractSliderButton {
	protected ITextComponent prefix;
	protected ITextComponent suffix;
	protected double minValue;
	protected double maxValue;
	protected double stepSize;
	protected boolean drawString;
	private final DecimalFormat format;

	public ExtendedSlider(int x, int y, int width, int height, ITextComponent prefix, ITextComponent suffix, double minValue,
			double maxValue, double currentValue, double stepSize, int precision, boolean drawString) {
		super(x, y, width, height, new TextComponentString(""), 0.0D);
		this.prefix = prefix;
		this.suffix = suffix;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.stepSize = Math.abs(stepSize);
		this.value = this.snapToNearest((currentValue - minValue) / (maxValue - minValue));
		this.drawString = drawString;
		if (stepSize == 0.0D) {
			precision = Math.min(precision, 4);
			StringBuilder pattern = new StringBuilder("0");
			if (precision > 0) {
				pattern.append('.');
			}
			while (precision-- > 0) {
				pattern.append('0');
			}
			this.format = new DecimalFormat(pattern.toString());
		} else if (this.stepSize == Math.floor(this.stepSize)) {
			this.format = new DecimalFormat("0");
		} else {
			this.format = new DecimalFormat(Double.toString(this.stepSize).replaceAll("\\d", "0"));
		}
		this.updateMessage();
	}

	public ExtendedSlider(int x, int y, int width, int height, ITextComponent prefix, ITextComponent suffix, double minValue,
			double maxValue, double currentValue, boolean drawString) {
		this(x, y, width, height, prefix, suffix, minValue, maxValue, currentValue, 1.0D, 0, drawString);
	}

	public double getValue() {
		return this.value * (this.maxValue - this.minValue) + this.minValue;
	}

	public long getValueLong() {
		return Math.round(this.getValue());
	}

	public int getValueInt() {
		return (int) this.getValueLong();
	}

	public void setValue(double value) {
		this.value = this.snapToNearest((value - this.minValue) / (this.maxValue - this.minValue));
		this.updateMessage();
	}

	public String getValueString() {
		return this.format.format(this.getValue());
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		this.setValueFromMouse(mouseX);
	}

	@Override
	protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
		super.onDrag(mouseX, mouseY, dragX, dragY);
		this.setValueFromMouse(mouseX);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		boolean left = keyCode == GLFW_KEY_LEFT;
		if (left || keyCode == GLFW_KEY_RIGHT) {
			if (this.minValue > this.maxValue) {
				left = !left;
			}
			float step = left ? -1.0F : 1.0F;
			if (this.stepSize <= 0.0D) {
				this.setSliderValue(this.value + step / (this.width - 8));
			} else {
				this.setValue(this.getValue() + step * this.stepSize);
			}
		}
		return false;
	}

	private void setValueFromMouse(double mouseX) {
		this.setSliderValue((mouseX - (this.x + 4)) / (this.width - 8));
	}

	private void setSliderValue(double value) {
		double old = this.value;
		this.value = this.snapToNearest(value);
		if (old != this.value) {
			this.applyValue();
		}
		this.updateMessage();
	}

	private double snapToNearest(double value) {
		if (this.stepSize <= 0.0D) {
			return MathHelper.clamp(value, 0.0D, 1.0D);
		}
		value = this.minValue + MathHelper.clamp(value, 0.0D, 1.0D) * (this.maxValue - this.minValue);
		value = this.stepSize * Math.round(value / this.stepSize);
		value = this.minValue > this.maxValue ? MathHelper.clamp(value, this.maxValue, this.minValue)
				: MathHelper.clamp(value, this.minValue, this.maxValue);
		return (value - this.minValue) / (this.maxValue - this.minValue);
	}

	@Override
	protected void updateMessage() {
		if (this.drawString) {
			this.setMessage(new TextComponentString("").appendSibling(this.prefix.createCopy()).appendText(this.getValueString())
					.appendSibling(this.suffix.createCopy()));
		} else {
			this.setMessage(new TextComponentString(""));
		}
	}

	@Override
	protected void applyValue() {
	}
}
