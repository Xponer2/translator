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

	/**
	 * 1.21 {@code splitLines(text, width, style, withEndSpace, linePos)}: the lines of an edited text as positions, each
	 * told to the (carried) {@code LinePosConsumer} as {@code accept(style, start, end)}: broken at new lines, and at the
	 * last space before the width (or inside a word too long for it). Create's clipboard and Supplementaries' notes
	 * lay out their text with it.
	 */
	public void splitLines(String text, int width, Style style, boolean withEndSpace, Object linePos) {
		java.lang.reflect.Method accept = linePosAccept(linePos);
		if (accept == null) {
			return;
		}
		int length = text.length();
		int lineStart = 0;
		int lastSpace = -1;
		int lineWidth = 0;
		int i = 0;
		while (i < length) {
			char c = text.charAt(i);
			if (c == '\n') {
				acceptLine(accept, linePos, style, lineStart, i);
				lineStart = ++i;
				lastSpace = -1;
				lineWidth = 0;
				continue;
			}
			if (c == '§' && i + 1 < length) {
				i += 2;
				continue;
			}
			int charWidth = Math.max(0, this.font.getCharWidth(c));
			if (lineWidth + charWidth > width && i > lineStart) {
				if (lastSpace >= lineStart) {
					acceptLine(accept, linePos, style, lineStart, withEndSpace ? lastSpace + 1 : lastSpace);
					lineStart = lastSpace + 1;
				} else {
					acceptLine(accept, linePos, style, lineStart, i);
					lineStart = i;
				}
				i = lineStart;
				lastSpace = -1;
				lineWidth = 0;
				continue;
			}
			if (c == ' ') {
				lastSpace = i;
			}
			lineWidth += charWidth;
			i++;
		}
		acceptLine(accept, linePos, style, lineStart, length);
	}

	@Nullable
	private static java.lang.reflect.Method linePosAccept(@Nullable Object linePos) {
		if (linePos == null) {
			return null;
		}
		for (Class<?> type : linePos.getClass().getInterfaces()) {
			for (java.lang.reflect.Method method : type.getMethods()) {
				if ("accept".equals(method.getName()) && method.getParameterCount() == 3 && !method.isDefault()) {
					return method;
				}
			}
		}
		return null;
	}

	private static void acceptLine(java.lang.reflect.Method accept, Object linePos, Style style, int start, int end) {
		try {
			accept.invoke(linePos, style, start, end);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
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
