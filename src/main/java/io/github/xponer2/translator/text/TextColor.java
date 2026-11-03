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
package io.github.xponer2.translator.text;

import javax.annotation.Nullable;

import net.minecraft.util.text.TextFormatting;

/**
 * A text color (1.21 {@code TextColor}, neoforge-1.21.1-runtime.tsv): any RGB, shown in 1.12.2 text as the nearest of
 * the 16 formatting colors.
 */
public final class TextColor {
	private static final int[] RGB = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA, 0x555555, 0x5555FF, 0x55FF55,
			0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

	private final int value;

	private TextColor(int value) {
		this.value = value & 0xFFFFFF;
	}

	public static TextColor fromRgb(int rgb) {
		return new TextColor(rgb);
	}

	@Nullable
	public static TextColor fromLegacyFormat(@Nullable TextFormatting format) {
		return format == null || !format.isColor() ? null : new TextColor(RGB[format.getColorIndex()]);
	}

	@Nullable
	public static TextColor parseColor(String text) {
		try {
			return text.startsWith("#") ? new TextColor(Integer.parseInt(text.substring(1), 16)) : fromLegacyFormat(TextFormatting.getValueByName(text));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public int getValue() {
		return this.value;
	}

	public String serialize() {
		return String.format("#%06X", this.value);
	}

	/** The formatting color nearest to this color. */
	public TextFormatting nearest() {
		TextFormatting best = TextFormatting.WHITE;
		long bestDistance = Long.MAX_VALUE;
		for (TextFormatting format : TextFormatting.values()) {
			if (!format.isColor()) {
				continue;
			}
			int rgb = RGB[format.getColorIndex()];
			long dr = (rgb >> 16 & 0xFF) - (this.value >> 16 & 0xFF);
			long dg = (rgb >> 8 & 0xFF) - (this.value >> 8 & 0xFF);
			long db = (rgb & 0xFF) - (this.value & 0xFF);
			long distance = dr * dr + dg * dg + db * db;
			if (distance < bestDistance) {
				bestDistance = distance;
				best = format;
			}
		}
		return best;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof TextColor color && color.value == this.value;
	}

	@Override
	public int hashCode() {
		return this.value;
	}

	@Override
	public String toString() {
		return this.serialize();
	}
}
