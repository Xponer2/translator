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

import java.util.List;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 {@code StringSplitter} ({@code font.getSplitter()}): widths and line breaks of text, on 1.12.2's font renderer.
 * 1.21's formatted texts are 1.12.2 text components here.
 */
@SideOnly(Side.CLIENT)
public class StringSplitter {
	private final FontRenderer font;

	public StringSplitter(FontRenderer font) {
		this.font = font;
	}

	public float stringWidth(@Nullable String text) {
		return text == null ? 0 : this.font.getStringWidth(text);
	}

	public float stringWidth(@Nullable ITextComponent text) {
		return text == null ? 0 : this.font.getStringWidth(text.getFormattedText());
	}

	public int plainIndexAtWidth(String text, int width, Style style) {
		return this.font.trimStringToWidth(text, width).length();
	}

	public String plainHeadByWidth(String text, int width, Style style) {
		return this.font.trimStringToWidth(text, width);
	}

	public String plainTailByWidth(String text, int width, Style style) {
		return this.font.trimStringToWidth(text, width, true);
	}

	/** The text cut into lines no wider than {@code width} (1.12.2 splits chat lines the same way). */
	public List<ITextComponent> splitLines(ITextComponent text, int width, Style style) {
		return GuiUtilRenderComponents.splitText(text, width, this.font, false, false);
	}

	public void splitLines(ITextComponent text, int width, Style style, BiConsumer<ITextComponent, Boolean> lines) {
		List<ITextComponent> split = this.splitLines(text, width, style);
		for (int i = 0; i < split.size(); i++) {
			lines.accept(split.get(i), i > 0);
		}
	}

	public List<ITextComponent> splitLines(String text, int width, Style style) {
		return this.splitLines(new TextComponentString(text).setStyle(style.createShallowCopy()), width, style);
	}

	/** The style of the text at that width (what a click there hits), null past its end. */
	@Nullable
	public Style componentStyleAtWidth(@Nullable ITextComponent text, int width) {
		if (text == null || width < 0) {
			return null;
		}
		int x = 0;
		for (ITextComponent part : text) {
			String plain = part.getUnformattedComponentText();
			if (plain.isEmpty()) {
				continue;
			}
			x += this.font.getStringWidth(part.getStyle().getFormattingCode() + plain);
			if (x > width) {
				return part.getStyle();
			}
		}
		return null;
	}

	/** 1.21 {@code getWordPosition}: the index after moving {@code count} words from {@code start}. */
	public static int getWordPosition(String text, int count, int start, boolean skipSpaces) {
		int index = start;
		boolean back = count < 0;
		for (int i = 0, n = Math.abs(count); i < n; i++) {
			if (back) {
				while (skipSpaces && index > 0 && text.charAt(index - 1) == ' ') {
					index--;
				}
				while (index > 0 && text.charAt(index - 1) != ' ') {
					index--;
				}
			} else {
				int length = text.length();
				int space = text.indexOf(' ', index);
				index = space == -1 ? length : space;
				while (skipSpaces && index < length && text.charAt(index) == ' ') {
					index++;
				}
			}
		}
		return index;
	}
}
