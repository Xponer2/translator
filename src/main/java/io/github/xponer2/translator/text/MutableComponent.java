package io.github.xponer2.translator.text;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;

/**
 * 1.21 mutable text component: the builder methods on a 1.12.2 text component. The converter creates literal and
 * translated texts as {@link LiteralComponent} / {@link TranslatableComponent}, which implement it.
 */
public interface MutableComponent extends ITextComponent {
	default MutableComponent withStyle(TextFormatting... formats) {
		var style = this.getStyle();
		for (var format : formats) {
			apply(style, format);
		}

		return this;
	}

	default MutableComponent withStyle(Style style) {
		this.setStyle(style);
		return this;
	}

	default MutableComponent append(String text) {
		this.appendText(text);
		return this;
	}

	default MutableComponent append(ITextComponent component) {
		this.appendSibling(component);
		return this;
	}

	/** 1.12.2 has no RGB text colors: the nearest of the 16 formatting colors. */
	default MutableComponent withColor(int rgb) {
		return this.withStyle(nearestColor(rgb));
	}

	default MutableComponent copy() {
		var copy = this.createCopy();
		return copy instanceof MutableComponent mutable ? mutable : new LiteralComponent("").append(copy);
	}

	static void apply(Style style, TextFormatting format) {
		if (format.isColor()) {
			style.setColor(format);
			return;
		}

		switch (format) {
			case BOLD -> style.setBold(true);
			case ITALIC -> style.setItalic(true);
			case UNDERLINE -> style.setUnderlined(true);
			case STRIKETHROUGH -> style.setStrikethrough(true);
			case OBFUSCATED -> style.setObfuscated(true);
			case RESET -> {
				style.setColor(null);
				style.setBold(false);
				style.setItalic(false);
				style.setUnderlined(false);
				style.setStrikethrough(false);
				style.setObfuscated(false);
			}
			default -> { }
		}
	}

	// the RGB values of the formatting colors (0-9, a-f)
	int[] COLORS = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
			0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

	static TextFormatting nearestColor(int rgb) {
		int best = 15;
		long bestDistance = Long.MAX_VALUE;
		for (int i = 0; i < COLORS.length; i++) {
			long dr = ((rgb >> 16) & 0xFF) - ((COLORS[i] >> 16) & 0xFF);
			long dg = ((rgb >> 8) & 0xFF) - ((COLORS[i] >> 8) & 0xFF);
			long db = (rgb & 0xFF) - (COLORS[i] & 0xFF);
			long distance = dr * dr + dg * dg + db * db;
			if (distance < bestDistance) {
				bestDistance = distance;
				best = i;
			}
		}

		return TextFormatting.fromColorIndex(best);
	}
}
